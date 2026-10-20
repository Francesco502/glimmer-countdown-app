package com.example.timeapk.ui.common

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Private, durable ownership records; filenames never come from directory traversal. */
internal class EventImageRegistry(
    private val metadata: File,
    private val images: File,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private data class State(
        val candidates: MutableMap<String, Long> = linkedMapOf(),
        val owners: MutableMap<String, Set<String>> = linkedMapOf()
    )

    private var unsafe = false

    @Synchronized
    fun registerCandidate(name: String): Boolean = change { state ->
        require(validName(name))
        state.candidates[name] = nowMillis()
    }

    @Synchronized
    fun replaceReferences(owner: String, names: Set<String>): Boolean = change { state ->
        require(validOwner(owner) && names.all(::validName))
        val previous = state.owners[owner].orEmpty()
        (previous - names).forEach { state.candidates[it] = nowMillis() }
        if (names.isEmpty()) state.owners.remove(owner) else state.owners[owner] = names
    }

    @Synchronized
    fun retainDraftReferences(owner: String, names: Set<String>): Boolean = change { state ->
        require(validOwner(owner) && names.all(::validName))
        // SavedState may restore an older checkpoint after process death; retain earlier picks
        // until the editor entry actually leaves, even if the visible draft has changed again.
        val retained = state.owners[owner].orEmpty() + names
        retained.forEach { state.candidates.putIfAbsent(it, nowMillis()) }
        if (retained.isNotEmpty()) state.owners[owner] = retained
    }

    @Synchronized
    fun releaseOwnersWithPrefix(prefix: String): Boolean = change { state ->
        val owners = state.owners.keys.filter { it.startsWith(prefix) }
        owners.forEach { owner ->
            state.owners.remove(owner).orEmpty().forEach { state.candidates[it] = nowMillis() }
        }
    }

    /** The reference loader runs under the same lock as ownership changes and deletion. */
    @Synchronized
    fun cleanup(referencedFiles: () -> Set<String>): Boolean {
        if (unsafe) return false
        return try {
            val state = read()
            val referenced = referencedFiles() + state.owners.values.flatten()
            val now = nowMillis()
            // ponytail: Only registered files are eligible. Preserve unknown historical images
            // until every legacy draft can be inventoried reliably before a directory-wide sweep.
            state.candidates.toMap().forEach { (name, since) ->
                if (name !in referenced && now >= since && now - since >= GRACE_PERIOD_MILLIS) {
                    val file = imageFile(name)
                    if (!file.exists() || (file.isFile && file.delete())) state.candidates.remove(name)
                }
            }
            write(state)
            true
        } catch (_: Exception) {
            unsafe = true
            false
        }
    }

    private fun change(action: (State) -> Unit): Boolean {
        if (unsafe) return false
        return try {
            val state = read()
            val previous = state.copy(candidates = state.candidates.toMutableMap(), owners = state.owners.toMutableMap())
            action(state)
            if (state != previous) write(state)
            true
        } catch (_: Exception) {
            // A failed protection write must disable deletion for this process.
            unsafe = true
            false
        }
    }

    private fun read(): State {
        if (!metadata.exists()) return State()
        val properties = Properties().apply { metadata.inputStream().use { load(it) } }
        check(properties.getProperty("version") == "1") { "Unknown image registry version" }
        val state = State()
        properties.stringPropertyNames().forEach { key ->
            val value = properties.getProperty(key)
            when {
                key == "version" -> Unit
                key.startsWith("candidate.") -> {
                    val name = key.removePrefix("candidate.")
                    check(validName(name))
                    val since = value.toLong()
                    check(since >= 0)
                    state.candidates[name] = since
                }
                key.startsWith("owner.") -> {
                    val owner = key.removePrefix("owner.")
                    check(validOwner(owner))
                    val names = value.split(',').toSet()
                    check(names.isNotEmpty() && names.all(::validName))
                    state.owners[owner] = names
                }
                else -> error("Unknown image registry record")
            }
        }
        return state
    }

    private fun write(state: State) {
        check(metadata.parentFile!!.isDirectory || metadata.parentFile!!.mkdirs())
        val properties = Properties().apply {
            setProperty("version", "1")
            state.candidates.forEach { (name, since) -> setProperty("candidate.$name", since.toString()) }
            state.owners.forEach { (owner, names) -> setProperty("owner.$owner", names.sorted().joinToString(",")) }
        }
        val temp = File(metadata.parentFile, "${metadata.name}.tmp")
        try {
            temp.outputStream().use { stream ->
                properties.store(stream, null)
                stream.fd.sync()
            }
            Files.move(temp.toPath(), metadata.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    private fun imageFile(name: String): File {
        require(validName(name))
        val root = images.canonicalFile
        val file = File(root, name).canonicalFile
        check(file.parentFile == root && file.name == name) { "Image must remain in private storage" }
        return file
    }

    companion object {
        const val GRACE_PERIOD_MILLIS = 24L * 60 * 60 * 1000
        private val FILE_NAME_PATTERN = Regex("^[0-9a-f]{32}\\.jpg$")
        private val OWNER_PATTERN = Regex("^[A-Za-z0-9_-]{1,100}$")
        fun validName(name: String?): Boolean = name != null && FILE_NAME_PATTERN.matches(name)
        private fun validOwner(owner: String): Boolean = OWNER_PATTERN.matches(owner)
    }
}

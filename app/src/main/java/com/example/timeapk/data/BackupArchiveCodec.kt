package com.example.timeapk.data

import com.example.timeapk.ui.common.EventImageRegistry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

internal const val MAX_BACKUP_IMAGE_BYTES = 8 * 1024 * 1024
internal const val MAX_BACKUP_ARCHIVE_BYTES = 64 * 1024 * 1024

internal data class BackupOrdering(
    val pinned: List<Int>,
    val customOrder: List<Int>,
    val milestones: List<Long>,
    val sortType: Int
)

internal data class BackupArchivePayload(
    val images: Map<String, ByteArray>,
    val ordering: BackupOrdering
)

internal object BackupArchiveCodec {
    private const val MANIFEST = "manifest.json"
    private const val IMAGE_DIR = "images/"
    private const val FORMAT_VERSION = 1
    private const val MAX_ENTRIES = 8_192

    fun write(
        output: OutputStream,
        events: List<Event>,
        categories: List<Category>,
        ordering: BackupOrdering,
        openImage: (String) -> InputStream
    ) {
        val rows = JSONArray(events.toJsonString(categories))
        val imageNames = linkedSetOf<String>()
        events.forEachIndexed { index, event ->
            event.imagePath?.let { fileName ->
                require(EventImageRegistry.validName(fileName)) { "Invalid event image name" }
                rows.getJSONObject(index).put("imagePath", fileName)
                imageNames += fileName
            }
        }
        val categoryRows = JSONArray(categories.map { category ->
            JSONObject().put("id", category.id)
                .put("name", category.name)
                .put("colorHex", category.colorHex ?: JSONObject.NULL)
        })
        val manifest = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("events", rows)
            .put("categories", categoryRows)
            .put("pinned", JSONArray(ordering.pinned))
            .put("customOrder", JSONArray(ordering.customOrder))
            .put("milestones", JSONArray(ordering.milestones))
            .put("sortType", ordering.sortType)
            .toString().toByteArray(Charsets.UTF_8)
        if (manifest.size > MAX_BACKUP_IMPORT_BYTES || imageNames.size >= MAX_ENTRIES) {
            throw BackupInputTooLargeException(archive = true)
        }
        var remaining = MAX_BACKUP_ARCHIVE_BYTES - manifest.size
        ZipOutputStream(output).use { zip ->
            zip.writeEntry(MANIFEST, manifest)
            imageNames.forEach { fileName ->
                val bytes = openImage(fileName).use { readEntry(it, minOf(MAX_BACKUP_IMAGE_BYTES, remaining)) }
                require(bytes.isNotEmpty()) { "Empty event image" }
                remaining -= bytes.size
                zip.writeEntry(IMAGE_DIR + fileName, bytes)
            }
        }
    }

    fun read(input: InputStream): Pair<BackupParseResult, BackupArchivePayload> {
        var manifest: ByteArray? = null
        val images = linkedMapOf<String, ByteArray>()
        var remaining = MAX_BACKUP_ARCHIVE_BYTES
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (images.size >= MAX_ENTRIES) throw BackupInputTooLargeException(archive = true)
                require(!entry.isDirectory) { "Unexpected ZIP directory" }
                val imageName = entry.name.removePrefix(IMAGE_DIR)
                val isManifest = entry.name == MANIFEST
                require(if (isManifest) manifest == null else {
                    entry.name.startsWith(IMAGE_DIR) && EventImageRegistry.validName(imageName) && imageName !in images
                }) { "Unexpected or duplicate ZIP entry" }
                val limit = minOf(remaining, if (isManifest) MAX_BACKUP_IMPORT_BYTES else MAX_BACKUP_IMAGE_BYTES)
                if (entry.size > limit) throw BackupInputTooLargeException(archive = true)
                val bytes = readEntry(zip, limit)
                require(bytes.isNotEmpty()) { "Empty ZIP entry" }
                remaining -= bytes.size
                if (isManifest) manifest = bytes else images[imageName] = bytes
            }
        }
        val root = JSONObject(requireNotNull(manifest) { "Missing backup manifest" }.toString(Charsets.UTF_8))
        require(root.getInt("version") == FORMAT_VERSION) { "Unsupported backup version" }
        val parsed = parseEventsJson(root.getJSONArray("events").toString(), archive = true)
        require(parsed.errorCount == 0) { "Invalid event in backup" }
        val events = parsed.events
        require(events.map(Event::id).distinct().size == events.size) { "Duplicate event ID" }
        require(events.mapNotNullTo(mutableSetOf(), Event::imagePath) == images.keys) { "Missing or unreferenced image" }
        val categoryRows = root.getJSONArray("categories")
        val categories = linkedMapOf<String, ImportedCategoryDraft>()
        repeat(categoryRows.length()) { index ->
            val row = categoryRows.getJSONObject(index)
            val id = row.getString("id")
            val name = normalizeCategoryName(row.getString("name"))
            require(isCustomCategoryId(id) && id !in categories && name.isNotBlank()) { "Invalid backup category" }
            categories[id] = ImportedCategoryDraft(name, if (row.isNull("colorHex")) null else row.getString("colorHex"))
        }
        require(parsed.categoryDrafts.keys.all(categories::containsKey)) { "Missing backup category" }
        val ordering = BackupOrdering(
            pinned = root.getJSONArray("pinned").eventIds(),
            customOrder = root.getJSONArray("customOrder").eventIds(),
            milestones = root.getJSONArray("milestones").let { values ->
                List(values.length()) { index -> values.getLong(index) }.onEach { require(it > 0) }
            },
            sortType = root.getInt("sortType").also { require(it in 0..2) }
        )
        return BackupParseResult(events, BackupSource.ZIP, events.size, 0, 0, null, categories) to
            BackupArchivePayload(images, ordering)
    }

    private fun JSONArray.eventIds(): List<Int> =
        List(length()) { getInt(it) }.onEach { require(it > 0) }.distinct()

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun readEntry(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = limit
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, remaining + 1))
            if (count < 0) return output.toByteArray()
            if (count > remaining) throw BackupInputTooLargeException(archive = true)
            output.write(buffer, 0, count)
            remaining -= count
        }
    }
}

package com.example.timeapk.update

/**
 * Release version: numeric segments plus an optional prerelease suffix.
 *
 * Accepts `4.2`, `v4.2.0`, `4.2-rc1`, `4.2.beta`, `4.2-beta.2`. Numeric segments compare first
 * with missing trailing segments treated as zero (`4.2 == 4.2.0`); for equal numbers a prerelease
 * sorts below the final release (`4.2-rc1 < 4.2`).
 */
internal data class ReleaseVersion(
    val numbers: List<Int>,
    val prerelease: String? = null
) : Comparable<ReleaseVersion> {

    override fun compareTo(other: ReleaseVersion): Int {
        for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
            val a = numbers.getOrElse(i) { 0 }
            val b = other.numbers.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return when {
            prerelease == null && other.prerelease == null -> 0
            prerelease == null -> 1
            other.prerelease == null -> -1
            else -> comparePrerelease(prerelease, other.prerelease)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ReleaseVersion && compareTo(other) == 0

    override fun hashCode(): Int =
        31 * numbers.dropLastWhile { it == 0 }.hashCode() + (prerelease?.hashCode() ?: 0)

    /** Canonical display form, e.g. `4.2` or `4.2-rc1`. */
    val displayName: String
        get() = numbers.joinToString(".") + (prerelease?.let { "-$it" } ?: "")

    val isPrerelease: Boolean get() = prerelease != null
}

private val RELEASE_VERSION_WITH_SUFFIX =
    Regex("^(\\d+(?:\\.\\d+)*)(?:[-.]([A-Za-z][A-Za-z0-9]*(?:[.-][A-Za-z0-9]+)*))?$")

/** Parses a release tag or version name; returns null instead of throwing when unrecognized. */
internal fun parseReleaseVersion(raw: String): ReleaseVersion? {
    val trimmed = raw.trim()
    val body = if (trimmed.startsWith("v") || trimmed.startsWith("V")) trimmed.substring(1) else trimmed
    val match = RELEASE_VERSION_WITH_SUFFIX.matchEntire(body) ?: return null
    val numbers = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
    val suffix = match.groupValues[2].takeIf { it.isNotEmpty() }?.lowercase()
    return ReleaseVersion(numbers, suffix)
}

private fun comparePrerelease(a: String, b: String): Int {
    val pa = a.split('.', '-')
    val pb = b.split('.', '-')
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrNull(i) ?: return -1
        val y = pb.getOrNull(i) ?: return 1
        val c = comparePrereleasePart(x, y)
        if (c != 0) return c
    }
    return 0
}

private val ALPHA_NUM = Regex("^([a-z]*)(\\d*)$")

private fun comparePrereleasePart(x: String, y: String): Int {
    val mx = ALPHA_NUM.matchEntire(x)
    val my = ALPHA_NUM.matchEntire(y)
    if (mx != null && my != null) {
        val labelCompare = mx.groupValues[1].compareTo(my.groupValues[1])
        if (labelCompare != 0) return labelCompare
        val nx = mx.groupValues[2].toLongOrNull() ?: 0L
        val ny = my.groupValues[2].toLongOrNull() ?: 0L
        return nx.compareTo(ny)
    }
    return x.compareTo(y)
}

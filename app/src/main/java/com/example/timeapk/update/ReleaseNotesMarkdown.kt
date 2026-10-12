package com.example.timeapk.update

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val HEADING = Regex("^(#{1,6})\\s+(.*?)\\s*#*\\s*$")
private val BULLET = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val IMAGE = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
private val LINK = Regex("\\[([^\\]]+)]\\([^)]*\\)")
private val AUTOLINK = Regex("<(https?://[^>]+)>")
private val INLINE_CODE = Regex("`([^`]+)`")
private val BOLD = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__")

/**
 * Lightweight, dependency-free rendering of GitHub release notes:
 * headings become bold (and [headingStyle]), `- ` / `* ` bullets become `• `,
 * `**bold**` becomes bold, links keep only their text, and blank-line runs collapse.
 */
fun renderReleaseNotesMarkdown(
    markdown: String,
    headingStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    boldStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold)
): AnnotatedString = buildAnnotatedString {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    var wroteLine = false
    var pendingBlank = false
    for (rawLine in lines) {
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            if (wroteLine) pendingBlank = true
            continue
        }
        if (HORIZONTAL_RULE.matches(line.trim())) continue
        if (wroteLine) append(if (pendingBlank) "\n\n" else "\n")
        pendingBlank = false
        wroteLine = true

        val heading = HEADING.matchEntire(line.trim())
        val bullet = BULLET.matchEntire(line)
        when {
            heading != null -> withStyle(headingStyle) {
                appendInline(stripBoldMarkers(heading.groupValues[2]), boldStyle)
            }
            bullet != null -> {
                val depth = bullet.groupValues[1].replace("\t", "  ").length / 2
                append("  ".repeat(depth))
                append("• ")
                appendInline(bullet.groupValues[2], boldStyle)
            }
            else -> appendInline(line.trim().removePrefix("> "), boldStyle)
        }
    }
}

private val HORIZONTAL_RULE = Regex("^([-*_])(\\s*\\1){2,}$")

private fun stripBoldMarkers(text: String): String =
    BOLD.replace(text) { it.groupValues[1].ifEmpty { it.groupValues[2] } }

private fun simplifyInline(text: String): String {
    var out = IMAGE.replace(text) { it.groupValues[1] }
    out = LINK.replace(out) { it.groupValues[1] }
    out = AUTOLINK.replace(out) { it.groupValues[1] }
    out = INLINE_CODE.replace(out) { it.groupValues[1] }
    return out
}

private fun AnnotatedString.Builder.appendInline(text: String, boldStyle: SpanStyle) {
    val simplified = simplifyInline(text)
    var cursor = 0
    for (match in BOLD.findAll(simplified)) {
        append(simplified.substring(cursor, match.range.first))
        withStyle(boldStyle) {
            append(match.groupValues[1].ifEmpty { match.groupValues[2] })
        }
        cursor = match.range.last + 1
    }
    append(simplified.substring(cursor))
}

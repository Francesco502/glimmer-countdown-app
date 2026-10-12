package com.example.timeapk.update

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesMarkdownTest {

    private val heading = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
    private val bold = SpanStyle(fontWeight = FontWeight.Bold)

    @Test
    fun rendersHeadingsBulletsBoldAndStripsLinks() {
        val md = """
            ## What's new

            - **Faster** widgets
            * See [the docs](https://example.com/docs) and <https://example.com>
              - nested `code`

            ---
            Plain line
        """.trimIndent()

        val out = renderReleaseNotesMarkdown(md, heading, bold)

        assertEquals(
            "What's new\n\n• Faster widgets\n• See the docs and https://example.com\n  • nested code\n\nPlain line",
            out.text
        )
        val headingSpan = out.spanStyles.single { it.item == heading }
        assertEquals("What's new", out.text.substring(headingSpan.start, headingSpan.end))
        val boldSpan = out.spanStyles.single { it.item == bold }
        assertEquals("Faster", out.text.substring(boldSpan.start, boldSpan.end))
    }

    @Test
    fun headingMarkersAndBoldInsideHeadingAreRemoved() {
        val out = renderReleaseNotesMarkdown("# **4.2** #\r\ntext", heading, bold)
        assertEquals("4.2\ntext", out.text)
    }

    @Test
    fun plainTextAndUnclosedMarkersPassThrough() {
        val out = renderReleaseNotesMarkdown("a * b and **open", heading, bold)
        assertEquals("a * b and **open", out.text)
        assertTrue(out.spanStyles.isEmpty())
    }

    @Test
    fun blankInputRendersEmpty() {
        assertEquals("", renderReleaseNotesMarkdown("\n\n  \n", heading, bold).text)
    }
}

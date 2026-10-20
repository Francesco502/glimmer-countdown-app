package com.example.timeapk.widget

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetValueFormatterTest {
    @Test
    fun measuredWidthKeepsHalfTheRowForTheTitle() {
        val widths = mapOf("12 days until next birthday" to 180f, "Birthday in 12d" to 96f, "In 12d" to 36f)
        val candidates = widths.keys.toList()
        assertEquals(WidgetValueLayout("12 days until next birthday", false),
            WidgetValueFormatter.fitMeasuredValue(candidates, 400f, 4f) { widths.getValue(it) })
        assertEquals(WidgetValueLayout("In 12d", false),
            WidgetValueFormatter.fitMeasuredValue(candidates, 100f, 4f) { widths.getValue(it) })
    }

    @Test
    fun largeFontStacksInsteadOfHidingTitleOrClippingUnit() {
        val widths = mapOf("距下次生日还有 12 天" to 180f, "生日余12天" to 80f, "余12天" to 50f)
        assertEquals(WidgetValueLayout("余12天", true),
            WidgetValueFormatter.fitMeasuredValue(widths.keys.toList(), 66f, 4f) { widths.getValue(it) })
    }

    @Test
    fun extremeFontRetainsWholeTodayTextForWrapping() {
        assertEquals(WidgetValueLayout("Birthday today", true),
            WidgetValueFormatter.fitMeasuredValue(listOf("Birthday today"), 62f, 4f) { 180f })
    }

    @Test
    fun equalCharacterCountsDoNotImplyEqualRenderedWidths() {
        val wide = WidgetValueFormatter.fitMeasuredValue(listOf("今天生日"), 70f, 4f) { 64f }
        val narrow = WidgetValueFormatter.fitMeasuredValue(listOf("Today"), 70f, 4f) { 30f }
        assertEquals(true, wide.stacked)
        assertEquals(false, narrow.stacked)
    }


    @Test
    fun semanticValue_prefersTextWhenItFits() {
        val text = WidgetValueFormatter.semanticValueOrFallback(
            preferredText = "还有12天",
            fallbackText = "余12天",
            maxChars = 8
        )

        assertEquals("还有12天", text)
    }

    @Test
    fun semanticValue_fallsBackWhenTextIsTooLong() {
        val text = WidgetValueFormatter.semanticValueOrFallback(
            preferredText = "已经1年2个月3天",
            fallbackText = "已428天",
            maxChars = 8
        )

        assertEquals("已428天", text)
    }

    @Test
    fun semanticValue_blankFullTextKeepsSemanticFallback() {
        assertEquals("In 12d", WidgetValueFormatter.semanticValueOrFallback("", "In 12d", 8))
    }

    @Test
    fun dayCount_smallCountsRemainExact() {
        assertEquals("12", WidgetValueFormatter.compactDayCount(12))
        assertEquals("0", WidgetValueFormatter.compactDayCount(0))
    }

    @Test
    fun dayCount_largeCountsKeepMagnitudeWithoutTruncating() {
        assertEquals("123m", WidgetValueFormatter.compactDayCount(123_456_789))
        assertEquals("1b", WidgetValueFormatter.compactDayCount(1_000_000_000))
    }

    @Test
    fun semanticValue_tinyBudgetStillKeepsDirectionAndUnit() {
        val text = WidgetValueFormatter.semanticValueOrFallback("还有123,456天", "余123k天", 3)
        assertTrue(text.startsWith("余"))
        assertTrue(text.endsWith("天"))
    }
}

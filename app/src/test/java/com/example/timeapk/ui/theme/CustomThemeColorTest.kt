package com.example.timeapk.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomThemeColorTest {

    @Test
    fun normalizeOpaqueThemeHex_acceptsOnlySixDigitRgb() {
        assertEquals("#A1B2C3", normalizeOpaqueThemeHex("  #a1b2c3  "))
        assertNull(normalizeOpaqueThemeHex("80A1B2C3"))
    }

    @Test
    fun normalizeOpaqueThemeHex_rejectsInvalidInput() {
        assertNull(normalizeOpaqueThemeHex("#12345"))
        assertNull(normalizeOpaqueThemeHex("#GG1122"))
        assertNull(normalizeOpaqueThemeHex("-12345"))
        assertNull(normalizeOpaqueThemeHex("+12345"))
        assertNull(normalizeOpaqueThemeHex(""))
    }
}

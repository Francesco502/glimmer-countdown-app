package com.example.timeapk.ui.theme

internal fun normalizeOpaqueThemeHex(input: String?): String? {
    val digits = input
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 }
        ?: return null
    if (digits.any { !it.isDigit() && it.uppercaseChar() !in 'A'..'F' }) return null
    return "#${digits.uppercase()}"
}

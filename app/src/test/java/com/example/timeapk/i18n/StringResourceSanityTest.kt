package com.example.timeapk.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class StringResourceSanityTest {

    @Test
    fun resourceKeys_areAlignedAcrossDefaultZhAndEn() {
        val defaultKeys = extractKeys(readResourceText("values/strings.xml"))
        val zhKeys = extractKeys(readResourceText("values-zh/strings.xml"))
        val enKeys = extractKeys(readResourceText("values-en/strings.xml"))

        assertEquals(enKeys, defaultKeys)
        assertEquals(enKeys, zhKeys)
    }

    @Test
    fun englishPack_doesNotContainChineseCharacters() {
        val en = readResourceText("values-en/strings.xml")
        val hasHan = Regex("\\p{IsHan}").containsMatchIn(en)
        assertFalse("English resources contain Chinese text", hasHan)
    }

    @Test
    fun zhPacks_doNotContainCommonMojibakeMarkers() {
        val suspicious = listOf(
            "\u951b",
            "\u9286",
            "\u9225",
            "\uFFFD",
            "\u95C7\u20AC",
            "\u9359"
        )
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")

        suspicious.forEach { token ->
            assertFalse("Default strings contain mojibake token: $token", defaultText.contains(token))
            assertFalse("zh strings contain mojibake token: $token", zhText.contains(token))
        }
    }

    private fun readResourceText(relative: String): String {
        val direct = File("src/main/res/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/res/$relative")
        require(fromRoot.exists()) { "Missing resource file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }

    private fun extractKeys(xml: String): Set<String> {
        return Regex("<string name=\"([^\"]+)\">").findAll(xml).map { it.groupValues[1] }.toSet()
    }
}

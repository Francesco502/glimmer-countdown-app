package com.example.timeapk.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Guards the subsetted fonts produced by scripts/fonts/subset-fonts.py: every
 * character shown by the UI strings and the lunar calendar must stay in the cmap.
 */
class FontSubsetCoverageTest {

    @Test
    fun serifFonts_coverLunarAndCoreUiCharacters() {
        SERIF_FONTS.forEach { name ->
            val cmap = readCmap(fontFile(name))
            val missing = (LUNAR_CHARS + CORE_UI_WORDS).codePoints().toArray()
                .filter { !Character.isWhitespace(it) && it !in cmap }
            assertTrue("$name is missing ${describe(missing)}", missing.isEmpty())
        }
    }

    @Test
    fun serifFont_coversEveryCharacterInStringResources() {
        val cmap = readCmap(fontFile("glimmer_serif.ttf"))
        STRING_FILES.forEach { relative ->
            val missing = stringResourceCodePoints(resFile(relative)).filter { it !in cmap }
            assertTrue("$relative has characters missing from Glimmer Serif: ${describe(missing)}", missing.isEmpty())
        }
    }

    @Test
    fun xiaoweiFont_coversLunarCharacters() {
        val cmap = readCmap(fontFile("glimmer_xiaowei_regular.ttf"))
        // ZCOOL XiaoWei has no U+3007 (〇); the system font supplies it if ever needed.
        val missing = LUNAR_CHARS.replace("〇", "").codePoints().toArray()
            .filter { !Character.isWhitespace(it) && it !in cmap }
        assertTrue("glimmer_xiaowei_regular.ttf is missing ${describe(missing)}", missing.isEmpty())
    }

    private fun describe(codePoints: List<Int>): String =
        codePoints.distinct().joinToString { "'${String(Character.toChars(it))}'(U+%04X)".format(it) }

    private fun stringResourceCodePoints(file: File): List<Int> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val texts = mutableListOf<String>()
        listOf("string", "item").forEach { tag ->
            val nodes = document.getElementsByTagName(tag)
            for (index in 0 until nodes.length) {
                texts += (nodes.item(index) as Element).textContent
            }
        }
        return texts.flatMap { raw ->
            raw.replace(FORMAT_PLACEHOLDER, "")
                .replace(ESCAPE_SEQUENCE, "")
                .codePoints().toArray().toList()
        }.filter { !Character.isWhitespace(it) && !Character.isISOControl(it) }.distinct()
    }

    /** Minimal OpenType cmap reader supporting subtable formats 4 and 12. */
    private fun readCmap(file: File): Set<Int> {
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.BIG_ENDIAN)
        val numTables = buffer.getShort(4).toInt() and 0xFFFF
        var cmapOffset = -1
        for (index in 0 until numTables) {
            val record = 12 + index * 16
            val tag = String(ByteArray(4) { buffer.get(record + it) }, Charsets.US_ASCII)
            if (tag == "cmap") cmapOffset = buffer.getInt(record + 8)
        }
        require(cmapOffset >= 0) { "${file.name} has no cmap table" }

        val codePoints = HashSet<Int>()
        val subtableCount = buffer.getShort(cmapOffset + 2).toInt() and 0xFFFF
        for (index in 0 until subtableCount) {
            val record = cmapOffset + 4 + index * 8
            val platformId = buffer.getShort(record).toInt() and 0xFFFF
            val encodingId = buffer.getShort(record + 2).toInt() and 0xFFFF
            val isUnicode = platformId == 0 || (platformId == 3 && (encodingId == 1 || encodingId == 10))
            if (!isUnicode) continue
            val subtable = cmapOffset + buffer.getInt(record + 4)
            when (buffer.getShort(subtable).toInt() and 0xFFFF) {
                4 -> readFormat4(buffer, subtable, codePoints)
                12 -> readFormat12(buffer, subtable, codePoints)
            }
        }
        require(codePoints.isNotEmpty()) { "${file.name} has no supported Unicode cmap subtable" }
        return codePoints
    }

    private fun readFormat4(buffer: ByteBuffer, offset: Int, out: MutableSet<Int>) {
        val segCount = (buffer.getShort(offset + 6).toInt() and 0xFFFF) / 2
        val endCodes = offset + 14
        val startCodes = endCodes + segCount * 2 + 2
        val idDeltas = startCodes + segCount * 2
        val idRangeOffsets = idDeltas + segCount * 2
        for (segment in 0 until segCount) {
            val end = buffer.getShort(endCodes + segment * 2).toInt() and 0xFFFF
            val start = buffer.getShort(startCodes + segment * 2).toInt() and 0xFFFF
            val delta = buffer.getShort(idDeltas + segment * 2).toInt()
            val rangeOffsetPosition = idRangeOffsets + segment * 2
            val rangeOffset = buffer.getShort(rangeOffsetPosition).toInt() and 0xFFFF
            if (start == 0xFFFF) continue
            for (code in start..end) {
                val glyph = if (rangeOffset == 0) {
                    (code + delta) and 0xFFFF
                } else {
                    val glyphPosition = rangeOffsetPosition + rangeOffset + (code - start) * 2
                    val raw = buffer.getShort(glyphPosition).toInt() and 0xFFFF
                    if (raw == 0) 0 else (raw + delta) and 0xFFFF
                }
                if (glyph != 0) out += code
            }
        }
    }

    private fun readFormat12(buffer: ByteBuffer, offset: Int, out: MutableSet<Int>) {
        val groups = buffer.getInt(offset + 12)
        for (group in 0 until groups) {
            val record = offset + 16 + group * 12
            val start = buffer.getInt(record)
            val end = buffer.getInt(record + 4)
            val startGlyph = buffer.getInt(record + 8)
            for (code in start..end) {
                if (startGlyph + (code - start) != 0) out += code
            }
        }
    }

    private fun fontFile(name: String): File = resFile("font/$name")

    private fun resFile(relative: String): File {
        val direct = File("src/main/res/$relative")
        if (direct.exists()) return direct
        val fromRoot = File("app/src/main/res/$relative")
        require(fromRoot.exists()) { "Missing resource file: $relative" }
        return fromRoot
    }

    private companion object {
        val SERIF_FONTS = listOf("glimmer_serif.ttf")
        val STRING_FILES = listOf("values/strings.xml", "values-zh/strings.xml", "values-en/strings.xml")

        const val LUNAR_CHARS =
            "正二三四五六七八九十冬腊闰初廿卅月日年" +
                "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥" +
                "鼠牛虎兔龙蛇马羊猴鸡狗猪" +
                "立春雨水惊蛰分清明谷夏小满芒种至暑大秋处白露寒霜降雪" +
                "〇零一0123456789"
        const val CORE_UI_WORDS = "正当此时已经还有天周今明"

        val FORMAT_PLACEHOLDER = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")
        val ESCAPE_SEQUENCE = Regex("\\\\(u[0-9a-fA-F]{4}|.)")
    }
}

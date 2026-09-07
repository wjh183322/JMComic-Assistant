package com.jinman.chahao.data

object ParseIds {
    private val CN_DIGIT = mapOf(
        '零' to 0, '〇' to 0,
        '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
        '六' to 6, '七' to 7, '八' to 8, '九' to 9,
        '壹' to 1, '贰' to 2, '兩' to 2, '两' to 2, '叁' to 3,
        '肆' to 4, '伍' to 5, '陆' to 6, '陸' to 6,
        '柒' to 7, '捌' to 8, '玖' to 9,
    )
    private val CN_UNIT = mapOf(
        '十' to 10, '拾' to 10,
        '百' to 100, '佰' to 100,
        '千' to 1000, '仟' to 1000,
        '万' to 10000, '萬' to 10000,
        '亿' to 100000000, '億' to 100000000,
    )
    private val CN_ANY = CN_DIGIT.keys + CN_UNIT.keys

    private val PREFIX =
        Regex("""(?:JM|jm|Jm|禁漫(?:天堂)?|车[牌号]|車牌|車號)\s*[:：#号號]?\s*(\d{4,8})""")
    private val COLON = Regex("""[：:]\s*(\d{5,8})(?!\d)""")
    private val STANDALONE = Regex("""(?<!\d)(\d{5,8})(?!\d)""")

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when {
                    c in '\uFF10'..'\uFF19' -> ('0'.code + (c.code - 0xFF10)).toChar()
                    c == '〇' -> '零'
                    else -> c
                },
            )
        }
        return sb.toString()
    }

    fun parseChineseNumber(raw: String): String {
        if (raw.isEmpty()) return ""
        val hasUnit = raw.any { it in CN_UNIT }
        if (!hasUnit) {
            return raw.map { CN_DIGIT[it]?.toString() ?: "" }.joinToString("")
        }
        var total = 0
        var section = 0
        var number = 0
        for (c in raw) {
            val d = CN_DIGIT[c]
            if (d != null) {
                number = d
                continue
            }
            val unit = CN_UNIT[c] ?: continue
            if (unit >= 10000) {
                section = (section + number) * unit
                total += section
                section = 0
                number = 0
            } else {
                section += (if (number == 0) 1 else number) * unit
                number = 0
            }
        }
        return (total + section + number).toString()
    }

    fun extractIds(input: String): List<ExtractedId> {
        val text = normalize(input)
        if (text.isBlank()) return emptyList()
        val used = mutableListOf<IntRange>()
        val spans = mutableListOf<ExtractedId>()

        fun overlaps(start: Int, end: Int) =
            used.any { start < it.last + 1 && end > it.first }

        fun snippet(start: Int, end: Int): String {
            val a = (start - 6).coerceAtLeast(0)
            val b = (end + 6).coerceAtMost(text.length)
            return text.substring(a, b).replace(Regex("""\s+"""), " ").trim()
        }

        PREFIX.findAll(text).forEach { m ->
            val digits = m.groupValues[1]
            used += m.range
            spans += ExtractedId(digits, "prefixed", m.value.trim())
        }
        COLON.findAll(text).forEach { m ->
            val digits = m.groupValues[1]
            val start = m.range.first + m.value.indexOf(digits)
            val end = start + digits.length
            if (!overlaps(start, end)) {
                used += start until end
                spans += ExtractedId(digits, "plain", snippet(start, end))
            }
        }
        STANDALONE.findAll(text).forEach { m ->
            val digits = m.groupValues[1]
            val start = m.range.first
            val end = start + digits.length
            if (!overlaps(start, end)) {
                used += start until end
                spans += ExtractedId(digits, "plain", snippet(start, end))
            }
        }

        val out = mutableListOf<ExtractedId>()
        val seen = mutableSetOf<String>()
        for (s in spans) {
            if (seen.add(s.id)) out += s
        }
        if (out.isEmpty()) {
            val concat = collectCipher(text, used)
            if (concat.matches(Regex("""^\d{4,8}$"""))) {
                out += ExtractedId(
                    concat,
                    "cipher",
                    text.replace(Regex("""\s+"""), " ").trim().take(40),
                )
            }
        }
        return out
    }

    private fun collectCipher(text: String, used: List<IntRange>): String {
        val arabic = mutableListOf<String>()
        val chinese = mutableListOf<String>()
        var i = 0
        fun hit(start: Int, end: Int) =
            used.any { start < it.last + 1 && end > it.first }
        while (i < text.length) {
            if (hit(i, i + 1)) {
                i += 1
                continue
            }
            val ch = text[i]
            if (ch in '0'..'9') {
                var j = i
                while (j < text.length && text[j] in '0'..'9') j++
                if (!hit(i, j)) arabic += text.substring(i, j)
                i = j
                continue
            }
            if (ch in CN_ANY) {
                var j = i
                while (j < text.length && text[j] in CN_ANY) j++
                if (!hit(i, j)) chinese += parseChineseNumber(text.substring(i, j))
                i = j
                continue
            }
            i += 1
        }
        val joined = (if (arabic.isNotEmpty()) arabic else chinese).joinToString("")
        return joined.trimStart('0').ifEmpty { joined }
    }
}

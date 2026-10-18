package com.neteinstein.networth.feature.importer.domain.parse

import kotlinx.datetime.LocalDate

/** Reads ZIP archives (stored or deflated entries) via the central directory. */
internal object Zip {
    fun entries(data: ByteArray): Map<String, () -> ByteArray> {
        var eocd = -1
        for (i in data.size - 22 downTo maxOf(0, data.size - 65_557)) {
            if (u32(data, i) == 0x06054b50L) {
                eocd = i
                break
            }
        }
        if (eocd < 0) throw ParseException(ParseFailure.NOT_XLSX, "This file isn't a valid XLSX (ZIP) file")
        val count = u16(data, eocd + 10)
        var p = u32(data, eocd + 16).toInt()
        val result = LinkedHashMap<String, () -> ByteArray>()
        repeat(count) {
            if (p + 46 > data.size || u32(data, p) != 0x02014b50L) return result
            val method = u16(data, p + 10)
            val compressed = u32(data, p + 20).toInt()
            val nameLength = u16(data, p + 28)
            val extraLength = u16(data, p + 30)
            val commentLength = u16(data, p + 32)
            val localOffset = u32(data, p + 42).toInt()
            val name = data.copyOfRange(p + 46, p + 46 + nameLength).decodeToString()
            result[name] = {
                val localName = u16(data, localOffset + 26)
                val localExtra = u16(data, localOffset + 28)
                val start = localOffset + 30 + localName + localExtra
                val raw = data.copyOfRange(start, minOf(data.size, start + compressed))
                when (method) {
                    0 -> raw
                    8 -> Inflate.raw(raw)
                    else -> throw ParseException(ParseFailure.XLSX_COMPRESSION, "Unsupported compression in this XLSX file")
                }
            }
            p += 46 + nameLength + extraLength + commentLength
        }
        return result
    }

    private fun u16(
        d: ByteArray,
        i: Int,
    ): Int = (d[i].toInt() and 0xFF) or ((d[i + 1].toInt() and 0xFF) shl 8)

    private fun u32(
        d: ByteArray,
        i: Int,
    ): Long = (u16(d, i).toLong()) or (u16(d, i + 2).toLong() shl 16)
}

/** The first worksheet of an XLSX workbook as a table of strings. Dates become `yyyy-mm-dd`. */
internal object Xlsx {
    private val sharedItem = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
    private val textRun = Regex("<t(?:\\s[^>]*)?>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
    private val rowPattern = Regex("<row\\b[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
    private val cellPattern = Regex("<c\\b([^>]*?)(?:/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL)
    private val valuePattern = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
    private val refPattern = Regex("\\br=\"([A-Z]+)\\d+\"")
    private val typePattern = Regex("\\bt=\"(\\w+)\"")
    private val stylePattern = Regex("\\bs=\"(\\d+)\"")
    private val numFmtPattern = Regex("<numFmt\\b[^>]*numFmtId=\"(\\d+)\"[^>]*formatCode=\"([^\"]*)\"")
    private val xfPattern = Regex("<xf\\b[^>]*numFmtId=\"(\\d+)\"")

    fun table(data: ByteArray): List<List<String>> {
        val entries = Zip.entries(data)
        val shared =
            entries["xl/sharedStrings.xml"]?.invoke()?.decodeToString()?.let { xml ->
                sharedItem.findAll(xml).map { item ->
                    textRun.findAll(item.groupValues[1]).joinToString("") { unescape(it.groupValues[1]) }
                }.toList()
            } ?: emptyList()
        val dateStyles = dateStyles(entries["xl/styles.xml"]?.invoke()?.decodeToString())
        val sheetName =
            entries.keys.filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
                .minByOrNull { it.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }
                ?: throw ParseException(ParseFailure.XLSX_NO_WORKSHEET, "This XLSX file has no worksheet")
        val xml = entries.getValue(sheetName)().decodeToString()
        return rowPattern.findAll(xml).map { row ->
            val cells = ArrayList<String>()
            cellPattern.findAll(row.groupValues[1]).forEach { cell ->
                val attrs = cell.groupValues[1]
                val body = cell.groupValues[2]
                val column = refPattern.find(attrs)?.groupValues?.get(1)?.let(::columnIndex) ?: cells.size
                while (cells.size < column) cells += ""
                val type = typePattern.find(attrs)?.groupValues?.get(1)
                val raw = valuePattern.find(body)?.groupValues?.get(1)?.let(::unescape)
                val style = stylePattern.find(attrs)?.groupValues?.get(1)?.toInt()
                val value =
                    when (type) {
                        "s" -> raw?.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                        "inlineStr" -> textRun.findAll(body).joinToString("") { unescape(it.groupValues[1]) }
                        "str", "b", "e" -> raw.orEmpty()
                        else -> number(raw, style != null && style in dateStyles)
                    }
                if (cells.size == column) cells += value else cells[column] = value
            }
            cells
        }.toList()
    }

    private fun number(
        raw: String?,
        isDate: Boolean,
    ): String {
        val n = raw?.toDoubleOrNull() ?: return raw.orEmpty()
        if (isDate) return LocalDate.fromEpochDays(n.toLong().toInt() - EXCEL_EPOCH_OFFSET).toString()
        // Two decimals, dot separator: what Values.amountMinor reads unambiguously.
        val cents = kotlin.math.round(n * 100).toLong()
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** Style indexes whose number format is a date (built-in ids 14–22, 45–47, or a custom format with d/m/y). */
    private fun dateStyles(styles: String?): Set<Int> {
        if (styles == null) return emptySet()
        val custom = numFmtPattern.findAll(styles).associate { it.groupValues[1].toInt() to it.groupValues[2] }
        val cellXfs = styles.substringAfter("<cellXfs", "").substringBefore("</cellXfs>")
        return xfPattern.findAll(cellXfs).mapIndexedNotNull { index, m ->
            val id = m.groupValues[1].toInt()
            val code = custom[id]?.lowercase()?.replace(Regex("\\[[^]]*]|\"[^\"]*\""), "")
            val isDate = id in 14..22 || id in 45..47 || (code != null && code.contains('d') && code.contains('y'))
            index.takeIf { isDate }
        }.toSet()
    }

    private fun columnIndex(letters: String): Int = letters.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1

    private fun unescape(s: String): String =
        s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

    /** Days between 1899-12-30 (Excel's day 0, accounting for its 1900 leap-year bug) and 1970-01-01. */
    private const val EXCEL_EPOCH_OFFSET = 25_569
}

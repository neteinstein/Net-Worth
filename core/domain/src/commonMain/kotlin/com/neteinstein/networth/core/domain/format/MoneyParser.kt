package com.neteinstein.networth.core.domain.format

/**
 * Parses amounts as people and banks write them — `1.234,56`, `1,234.56`, `€ 86,40`, `-12.5`,
 * `−733,59` — into minor units (cents). Returns null for anything that is not a number.
 *
 * With both `.` and `,` present the last one is the decimal separator. With only one kind, it is a
 * thousands separator when every group after it has exactly three digits (`1.234`, `1,234,567`),
 * otherwise a decimal separator (`86,4`, `12.50`).
 */
object MoneyParser {
    fun parseMinor(text: String, fractionDigits: Int = 2): Long? {
        var s = text.trim().replace('−', '-').filterNot { it.isWhitespace() || it == ' ' }
        s = s.filter { it.isDigit() || it == '.' || it == ',' || it == '-' || it == '+' }
        val negative = s.startsWith('-') || s.endsWith('-')
        s = s.trim('-', '+')
        if (s.isEmpty() || s.any { it == '-' || it == '+' }) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimalIndex = when {
            lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
            lastDot >= 0 || lastComma >= 0 -> {
                val separator = if (lastDot >= 0) '.' else ','
                val groups = s.split(separator)
                val looksGrouped =
                    groups.size > 1 && groups.drop(1).all { it.length == 3 } && groups.first().isNotEmpty() && groups.first().length <= 3
                if (looksGrouped) -1 else maxOf(lastDot, lastComma)
            }
            else -> -1
        }
        // The decimal separator can only appear once ("1,2,3" is not a number).
        if (decimalIndex >= 0 && s.count { it == s[decimalIndex] } > 1) return null
        val whole = (if (decimalIndex >= 0) s.substring(0, decimalIndex) else s).filter { it.isDigit() }
        val fraction = if (decimalIndex >= 0) s.substring(decimalIndex + 1) else ""
        if (fraction.any { !it.isDigit() } || (whole.isEmpty() && fraction.isEmpty())) return null
        if (fraction.length > fractionDigits) return null
        val factor = (1..fractionDigits).fold(1L) { acc, _ -> acc * 10 }
        val value =
            (whole.ifEmpty { "0" }.toLongOrNull() ?: return null) * factor + fraction.padEnd(fractionDigits, '0').ifEmpty { "0" }.toLong()
        return if (negative) -value else value
    }
}

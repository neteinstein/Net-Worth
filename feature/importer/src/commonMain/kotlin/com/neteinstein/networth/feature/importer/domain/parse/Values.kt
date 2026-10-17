package com.neteinstein.networth.feature.importer.domain.parse

import kotlinx.datetime.LocalDate

/** Amount and date parsing shared by every statement format, tolerant of PT and EN conventions. */
object Values {
    private val amountPattern =
        Regex("^[-+−]?\\(?[-+−]?(?:€\\s?)?(?:\\d{1,3}(?:[., ']\\d{3})+|\\d+)[.,]\\d{2}\\)?[-+]?(?:\\s?€|\\s?EUR)?$")

    /** True if [text] is a money amount with two decimals ("1.234,56", "-12.30", "(5,00)", "3,10 €"). */
    fun isAmount(text: String): Boolean = amountPattern.matches(text.trim())

    /**
     * Parses an amount into minor units (cents). The decimal separator is whichever of `,`/`.`
     * comes last when followed by 1–2 digits; the other one is a thousands separator.
     * "(5,00)", "5,00-" and "-5,00" are negative; a trailing "D"/"DB" means debit.
     */
    fun amountMinor(text: String): Long? {
        var t = text.trim().replace('−', '-').replace("€", "").replace("EUR", "", ignoreCase = true).trim()
        if (t.isEmpty()) return null
        var negative = false
        val suffix = Regex("\\s*(DB|CR|D|C)$", RegexOption.IGNORE_CASE).find(t)
        if (suffix != null) {
            negative = suffix.groupValues[1].uppercase().startsWith("D")
            t = t.substring(0, suffix.range.first).trim()
        }
        if (t.startsWith("(") && t.endsWith(")")) {
            negative = true
            t = t.substring(1, t.length - 1).trim()
        }
        if (t.endsWith("-")) {
            negative = !negative
            t = t.dropLast(1).trim()
        } else if (t.endsWith("+")) {
            t = t.dropLast(1).trim()
        }
        if (t.startsWith("-")) {
            negative = !negative
            t = t.drop(1).trim()
        } else if (t.startsWith("+")) {
            t = t.drop(1).trim()
        }
        t = t.replace(" ", "").replace("'", "").replace("\u00A0", "")
        if (t.isEmpty() || !t.all { it.isDigit() || it == ',' || it == '.' }) return null
        val lastSep = t.indexOfLast { it == ',' || it == '.' }
        val (whole, fraction) =
            if (lastSep >= 0 && t.length - lastSep - 1 in 1..2) {
                t.substring(0, lastSep) to t.substring(lastSep + 1)
            } else if (lastSep >= 0 && t.count { it == t[lastSep] } == 1 && t.length - lastSep - 1 != 3) {
                t.substring(0, lastSep) to t.substring(lastSep + 1)
            } else {
                t to ""
            }
        val digits = whole.filter { it.isDigit() }
        if (digits.isEmpty() && fraction.isEmpty()) return null
        val cents = fraction.padEnd(2, '0').take(2).ifEmpty { "00" }
        val value = ((digits.ifEmpty { "0" }).toLongOrNull() ?: return null) * 100 + cents.toLong()
        return if (negative) -value else value
    }

    private val dmy = Regex("^(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2}|\\d{4})$")
    private val ymd = Regex("^(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})(?:[T ].*)?$")
    private val compact = Regex("^(\\d{4})(\\d{2})(\\d{2})(?:\\d{6})?(?:\\.\\d+)?(?:\\[.*])?$")

    /** True if [text] is a date in one of the formats [date] understands. */
    fun isDate(text: String): Boolean = date(text) != null

    /** Parses "dd.mm.yy", "dd-mm-yyyy", "dd/mm/yyyy", "yyyy-mm-dd" and OFX's "yyyymmdd[hhmmss]". */
    fun date(text: String): LocalDate? {
        val t = text.trim()
        dmy.matchEntire(t)?.let { m ->
            val year = m.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            return safeDate(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())
        }
        ymd.matchEntire(t)?.let { m -> return safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        compact.matchEntire(t)?.let { m -> return safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }
        return null
    }

    private fun safeDate(
        year: Int,
        month: Int,
        day: Int,
    ): LocalDate? = if (month in 1..12 && day in 1..31) runCatching { LocalDate(year, month, day) }.getOrNull() else null

    /** Digits only, e.g. "0001 2345 6789" → "000123456789". */
    fun digits(text: String): String = text.filter { it.isDigit() }

    private val ibanPattern = Regex("\\b([A-Z]{2}\\d{2}(?:\\s?[A-Z0-9]){11,30})\\b")

    /** The first IBAN in [text], without spaces. */
    fun iban(text: String): String? =
        ibanPattern.find(text.uppercase())?.groupValues?.get(1)?.replace(" ", "")?.takeIf { it.length in 15..34 }

    /** Portuguese NIBs (21 digits) become IBANs with the fixed "PT50" prefix. */
    fun ibanFromNib(nib: String): String? = digits(nib).takeIf { it.length == 21 }?.let { "PT50$it" }
}

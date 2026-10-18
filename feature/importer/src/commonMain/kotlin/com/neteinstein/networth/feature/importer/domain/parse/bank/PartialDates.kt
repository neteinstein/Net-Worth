package com.neteinstein.networth.feature.importer.domain.parse.bank

import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextCell
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Rows that print their dates without the year ("10.01" at ActivoBank and Millennium bcp, "05-12" at Santander,
 * "22/12" at BPI). [complete] turns a row's leading one or two such dates into full dates inside the statement
 * period, so the shared [MovementTable] can read the row. A cell that holds both dates ("10.04 10.04") is split.
 */
internal class PartialDates(
    private val pattern: Regex,
    private val monthFirst: Boolean,
) {
    fun complete(
        line: PdfTextLine,
        start: LocalDate?,
        end: LocalDate?,
    ): PdfTextLine {
        if (start == null || end == null) return line
        val remaining = ArrayDeque(line.cells)
        val converted = ArrayList<PdfTextCell>()
        while (converted.size < 2 && remaining.isNotEmpty()) {
            val cell = remaining.first()
            val tokens = cell.text.split(' ')
            val leading = tokens.takeWhile { pattern.matches(it) }.take(2 - converted.size)
            if (leading.isEmpty()) break
            remaining.removeFirst()
            val width = (cell.endX - cell.x) / maxOf(1, cell.text.length)
            var x = cell.x
            for (token in leading) {
                val date = dateIn(token, start, end) ?: return line
                converted += PdfTextCell(x, x + token.length * width, date.toString())
                x += (token.length + 1) * width
            }
            val rest = tokens.drop(leading.size).joinToString(" ")
            if (rest.isNotEmpty()) {
                remaining.addFirst(PdfTextCell(x, cell.endX, rest))
                break
            }
        }
        return if (converted.isEmpty()) line else line.copy(cells = converted + remaining)
    }

    /** The date with this day and month closest to the period (value dates can fall just before or after it). */
    private fun dateIn(
        token: String,
        start: LocalDate,
        end: LocalDate,
    ): LocalDate? {
        val (first, second) = pattern.matchEntire(token)?.destructured ?: return null
        val month = (if (monthFirst) first else second).toInt()
        val day = (if (monthFirst) second else first).toInt()
        return (start.year - 1..end.year + 1)
            .mapNotNull { year -> runCatching { LocalDate(year, month, day) }.getOrNull() }
            .minByOrNull { date ->
                when {
                    date < start -> date.daysUntil(start)
                    date > end -> end.daysUntil(date)
                    else -> 0
                }
            }
    }

    companion object {
        /** "10.01" = 1 October (ActivoBank, Millennium bcp). */
        val MONTH_DAY = PartialDates(Regex("^(\\d{1,2})\\.(\\d{2})$"), monthFirst = true)

        /** "05-12" = 5 December (Santander). */
        val DAY_MONTH_DASH = PartialDates(Regex("^(\\d{2})-(\\d{2})$"), monthFirst = false)

        /** "22/12" = 22 December (BPI). */
        val DAY_MONTH_SLASH = PartialDates(Regex("^(\\d{2})/(\\d{2})$"), monthFirst = false)
    }
}

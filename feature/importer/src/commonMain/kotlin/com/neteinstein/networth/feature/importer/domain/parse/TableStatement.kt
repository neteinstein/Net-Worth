package com.neteinstein.networth.feature.importer.domain.parse

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.model.ParsedRow
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import kotlin.math.abs

/**
 * Spreadsheet-shaped exports (CSV and XLSX): an optional preamble (account holder, IBAN…), a
 * header row, then one movement per row. Columns are found by their header in Portuguese or
 * English, so column order and extra columns don't matter.
 */
internal object TableStatement {
    private enum class Column { DATE, VALUE_DATE, DESCRIPTION, AMOUNT, DEBIT, CREDIT, BALANCE }

    private fun columnOf(header: String): Column? {
        val h = header.lowercase().trim().replace(Regex("\\s+"), " ")
        if (h.isEmpty()) return null
        return when {
            (h.contains("débito") || h.contains("debito")) && (h.contains("crédito") || h.contains("credito")) -> Column.AMOUNT
            h.startsWith("data valor") || h.startsWith("data-valor") || h.contains("value date") -> Column.VALUE_DATE
            h.startsWith("data") || h.startsWith("date") || h == "dia" || h.contains("posted") || h.contains("booking") -> Column.DATE
            h.contains("saldo") || h.contains("balance") -> Column.BALANCE
            h.contains("débito") ||
                h.contains("debito") ||
                h.contains("debit") ||
                h == "saída" ||
                h == "saidas" ||
                h == "saídas" ||
                h.contains("money out") ||
                h.contains("withdrawal") -> Column.DEBIT
            h.contains("crédito") ||
                h.contains("credito") ||
                h.contains("credit") ||
                h == "entrada" ||
                h == "entradas" ||
                h.contains("money in") ||
                h.contains("deposit") -> Column.CREDIT
            h.contains("montante") ||
                h.contains("valor") ||
                h.contains("amount") ||
                h.contains("importância") ||
                h.contains("importancia") -> Column.AMOUNT
            h.contains("descri") ||
                h.contains("movimento") ||
                h.contains("detail") ||
                h.contains("memo") ||
                h.contains("payee") ||
                h.contains("narrativ") ||
                h.contains("referência") ||
                h.contains("reference") ||
                h.contains("concept") ||
                h.contains("text") -> Column.DESCRIPTION
            else -> null
        }
    }

    fun parse(
        fileName: String,
        format: StatementFormat,
        table: List<List<String>>,
    ): ParsedStatement {
        val headerIndex =
            table.indexOfFirst { row ->
                val columns = row.mapNotNull(::columnOf).toSet()
                Column.DATE in columns && (Column.AMOUNT in columns || Column.DEBIT in columns || Column.CREDIT in columns)
            }
        if (headerIndex < 0) throw ParseException(ParseFailure.NO_HEADER_ROW, "Couldn't find the header row (a date and an amount column)")
        val header = table[headerIndex]
        val columns = HashMap<Column, Int>()
        header.forEachIndexed { i, name ->
            val column = columnOf(name) ?: return@forEachIndexed
            val better = column == Column.DESCRIPTION &&
                name.contains("descri", ignoreCase = true) &&
                columns[column]?.let { !header[it].contains("descri", ignoreCase = true) } == true
            if (column !in columns || better) columns[column] = i
        }
        if (Column.DESCRIPTION !in columns) {
            // Fall back to the widest text column that isn't mapped.
            val free = header.indices.filter { it !in columns.values }
            free.maxByOrNull { i ->
                table.drop(headerIndex + 1).take(20).sumOf { it.getOrNull(i)?.length ?: 0 }
            }?.let {
                columns[Column.DESCRIPTION] =
                    it
            }
        }
        val preamble = table.take(headerIndex).flatten().joinToString(" ")
        val iban = Values.iban(preamble)

        fun cell(
            row: List<String>,
            column: Column,
        ): String = columns[column]?.let { row.getOrNull(it) }?.trim().orEmpty()

        val rows =
            table.drop(headerIndex + 1).mapNotNull { row ->
                val date = Values.date(cell(row, Column.DATE)) ?: return@mapNotNull null
                val amount =
                    if (Column.AMOUNT in columns && cell(row, Column.AMOUNT).isNotEmpty()) {
                        Values.amountMinor(cell(row, Column.AMOUNT))
                    } else {
                        val debit = Values.amountMinor(cell(row, Column.DEBIT))?.let { abs(it) } ?: 0L
                        val credit = Values.amountMinor(cell(row, Column.CREDIT))?.let { abs(it) } ?: 0L
                        if (debit == 0L && credit == 0L) null else credit - debit
                    } ?: return@mapNotNull null
                ParsedRow(
                    date = date,
                    valueDate = Values.date(cell(row, Column.VALUE_DATE)),
                    description = cell(row, Column.DESCRIPTION).replace(Regex("\\s+"), " "),
                    amountMinor = amount,
                    balanceMinor = Values.amountMinor(cell(row, Column.BALANCE)),
                )
            }
        if (rows.isEmpty()) throw ParseException(ParseFailure.NO_ROWS, "No movements found under the header row")
        // Banks export newest-first as often as oldest-first; keep the file order of same-day rows.
        val ordered = if (rows.first().date > rows.last().date) rows.reversed() else rows
        val first = ordered.first()
        return ParsedStatement(
            fileName = fileName,
            format = format,
            sections =
                listOf(
                    ParsedSection(
                        label = "Imported account",
                        iban = iban,
                        suggestedType = AccountType.CURRENT,
                        openingBalanceMinor = first.balanceMinor?.minus(first.amountMinor),
                        closingBalanceMinor = ordered.last().balanceMinor,
                        rows = ordered,
                    ),
                ),
        )
    }
}

/** RFC 4180-ish CSV with delimiter sniffing (`;` is the norm for PT banks, `,`, tab and `|` also occur). */
internal object Csv {
    fun parse(text: String): List<List<String>> {
        val lines = text.lineSequence().take(50).filter { it.isNotBlank() }.toList()
        val delimiter =
            listOf(';', ',', '\t', '|').maxByOrNull { d ->
                val counts = lines.map { line -> line.count { it == d } }.filter { it > 0 }
                // Prefer a delimiter that splits many lines into the same number of fields.
                counts.groupingBy { it }.eachCount().maxOfOrNull { (fields, n) -> n * 10 + fields } ?: 0
            } ?: ';'
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> if (field.isBlank()) quoted = true else field.append(c)
                    delimiter -> {
                        row += field.toString()
                        field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row += field.toString()
                        field.clear()
                        if (row.any { it.isNotBlank() }) rows += row
                        row = ArrayList()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        row += field.toString()
        if (row.any { it.isNotBlank() }) rows += row
        return rows
    }
}

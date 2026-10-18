package com.neteinstein.networth.feature.importer.domain.parse.bank

import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import com.neteinstein.networth.feature.importer.domain.parse.Values
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine
import kotlinx.datetime.LocalDate

/**
 * A bank whose statement lists each account's movements under a heading, inside one period:
 *
 * - [account] finds an account number (and, with [sectionStart] unset, also starts that account's movements);
 *   [labelOf] and [numberOf] read the match.
 * - [sectionStart], when set, is the line that starts the movements of the last account number seen (the number
 *   sits in the page header); [labelOf] then reads this match.
 * - [period] holds the statement's first and last day (it may span two lines, e.g. "De: …" / "A: …").
 * - [partialDates] completes rows that print dates without the year.
 * - An IBAN before the first heading belongs to the first account; one under a heading to that account.
 */
internal class SectionedPdfProfile(
    private val bank: String,
    private val markers: List<String>,
    private val account: Regex,
    private val period: Regex,
    private val sectionStart: Regex? = null,
    private val partialDates: PartialDates? = null,
    private val labelOf: (MatchResult) -> String = { it.groupValues[1].trim() },
    private val numberOf: (MatchResult) -> String = { Values.digits(it.groupValues[2]) },
) : PdfStatementProfile {
    override fun matches(lines: List<PdfTextLine>): Boolean = lines.any { line -> markers.any { line.text.contains(it, true) } }

    override fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement {
        val sections = LinkedHashMap<String, MovementTable>()
        var current: MovementTable? = null
        var number: String? = null
        var pendingIban: String? = null
        var start: LocalDate? = null
        var end: LocalDate? = null
        var previousText = ""

        for (line in lines) {
            val text = line.text
            period.find("$previousText\n$text")?.let { match ->
                start = Values.date(match.groupValues[1])
                end = Values.date(match.groupValues[2])
                current?.takeIf { it.periodStart == null }?.let { table ->
                    table.periodStart = start
                    table.periodEnd = end
                }
            }
            previousText = text
            val iban = ibanIn(line)
            if (iban != null) {
                val table = current
                if (table == null) {
                    pendingIban = iban
                } else if (table.iban == null) {
                    table.iban = iban
                }
                continue
            }
            val heading = account.find(text)
            val starting = sectionStart?.find(text)
            val started =
                when {
                    sectionStart == null && heading != null -> heading to numberOf(heading)
                    starting != null -> number?.let { starting to it }
                    else -> null
                }
            if (heading != null && sectionStart != null) number = numberOf(heading)
            if (started != null) {
                val (match, accountNumber) = started
                val label = labelOf(match)
                current =
                    sections.getOrPut(accountNumber) {
                        MovementTable(
                            label = label,
                            accountNumber = accountNumber,
                            type = accountTypeFor(label),
                            periodStart = start,
                            periodEnd = end,
                            iban = pendingIban,
                        ).also { pendingIban = null }
                    }
                continue
            }
            if (heading != null) continue
            val table = current ?: continue
            if (!table.closed) table.accept(partialDates?.complete(line, start, end) ?: line)
        }
        return ParsedStatement(
            fileName = fileName,
            format = StatementFormat.PDF,
            bank = bank,
            sections = sections.values.map { it.build() },
        )
    }

    /** The IBAN in the cell labelled "IBAN" (or the cell after the label), not the rest of the line. */
    private fun ibanIn(line: PdfTextLine): String? {
        val index = line.cells.indexOfFirst { it.text.contains("IBAN", true) }
        if (index < 0) return null
        val cell = line.cells[index].text
        val value = cell.substring(cell.indexOf("IBAN", ignoreCase = true) + 4).substringAfter(':')
        return Values.iban(value) ?: line.cells.getOrNull(index + 1)?.let { Values.iban(it.text) }
    }
}

/**
 * Santander "Extrato": the page header `EXTRATO Nº … CONTA Nº 0000.00000000000 PERÍODO DE yyyy-mm-dd A yyyy-mm-dd`,
 * then "Detalhe de Movimentos da Conta à Ordem" with `dd-mm | dd-mm | Descritivo | Montante | Saldo` rows, from
 * "Saldo Inicial EUR" to "Saldo Contabilístico Final EUR".
 */
internal val SantanderProfile =
    SectionedPdfProfile(
        bank = "Santander",
        markers = listOf("TOTAPTPL", "Santander Totta"),
        account = Regex("CONTA N\\S{0,2}\\s*(\\d[\\d.]{5,}\\d)", RegexOption.IGNORE_CASE),
        sectionStart = Regex("^Detalhe de Movimentos d[ao] (.+)$", RegexOption.IGNORE_CASE),
        period = Regex("PER[ÍI]ODO DE (\\d{4}-\\d{2}-\\d{2}) A (\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE),
        partialDates = PartialDates.DAY_MONTH_DASH,
        numberOf = { Values.digits(it.groupValues[1]) },
    )

/**
 * BPI "Extracto de Conta": `Período De dd/mm/yyyy a dd/mm/yyyy`, then per account `CONTA A ORDEM BPI Nº: 1-1234567-001-001`,
 * its NIB and IBAN, and `dd/mm | dd/mm | Descrição | Valor | Saldo` rows (one signed column) from
 * "SALDO ANTERIOR CONTABILISTICO" to "SALDO ACTUAL CONTABILISTICO".
 */
internal val BpiProfile =
    SectionedPdfProfile(
        bank = "BPI",
        markers = listOf("BBPIPTPL", "Banco BPI"),
        account = Regex("^(CONTA .+?) N\\S{0,2}\\s*:?\\s*(\\d[\\d-]{6,}\\d)", RegexOption.IGNORE_CASE),
        period = Regex("Per[íi]odo\\s+De (\\d{2}/\\d{2}/\\d{4}) a (\\d{2}/\\d{2}/\\d{4})", RegexOption.IGNORE_CASE),
        partialDates = PartialDates.DAY_MONTH_SLASH,
    )

/**
 * Banco Montepio extract: `Período : yyyy-mm-dd a yyyy-mm-dd`, then per account
 * `DEPÓSITO À ORDEM - MOVIMENTOS - 123.10.000000-0 - <product>` and `yyyy-mm-dd | yyyy-mm-dd | Descrição | Montante |
 * Saldo | Moeda` rows, amounts suffixed with + or -.
 */
internal val MontepioProfile =
    SectionedPdfProfile(
        bank = "Montepio",
        markers = listOf("MPIOPTPL", "Banco Montepio", "Montepio Geral"),
        account = Regex("^(DEP[ÓO]SITO.+?)\\s*-\\s*MOVIMENTOS\\s*-\\s*(\\d[\\d.\\-]{5,}\\d)(?:\\s*-\\s*(.+))?$", RegexOption.IGNORE_CASE),
        period = Regex("Per[íi]odo\\s*:\\s*(\\d{4}-\\d{2}-\\d{2}) a (\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE),
        labelOf = { match -> match.groupValues[3].trim().ifEmpty { match.groupValues[1].trim() } },
    )

/**
 * Caixa Geral de Depósitos "Extracto": per account `CONTA <name> 0000.000000.000`, its IBAN, and
 * `Data Mov. | Data Valor | Descrição | Valor | Saldo Contabilístico` rows (one signed column, ISO dates) from
 * "Saldo Anterior" to "Saldo contabilístico".
 */
internal val CgdProfile =
    SectionedPdfProfile(
        bank = "CGD",
        markers = listOf("CGDIPTPL", "Caixa Geral de Dep"),
        account = Regex("^(CONTA .+?) (\\d{4}\\.\\d{6}\\.\\d{3})", RegexOption.IGNORE_CASE),
        period = Regex("Per[íi]odo\\s*:?\\s*(?:de\\s+)?(\\d{4}-\\d{2}-\\d{2}) a (\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE),
    )

/**
 * Crédito Agrícola online "Consulta de Movimentos de Contas D.O.": `Número de Conta: <number>`, `De: dd/mm/yyyy` /
 * `A: dd/mm/yyyy`, and `Data de Movimento | Data Valor | Descrição | Descritivo | Montante | Déb./Créd. | Saldo
 * Contabilístico` rows, where a "D"/"C" column gives the sign.
 */
internal val CreditoAgricolaProfile =
    SectionedPdfProfile(
        bank = "Crédito Agrícola",
        markers = listOf("CCCMPTPL", "creditoagricola.pt", "Crédito Agrícola Mútuo"),
        account = Regex("^(N[úu]mero de Conta):\\s*(\\d{6,})", RegexOption.IGNORE_CASE),
        period = Regex("De:\\s*(\\d{2}/\\d{2}/\\d{4})\\s+A:\\s*(\\d{2}/\\d{2}/\\d{4})", RegexOption.IGNORE_CASE),
        labelOf = { "Crédito Agrícola" },
    )

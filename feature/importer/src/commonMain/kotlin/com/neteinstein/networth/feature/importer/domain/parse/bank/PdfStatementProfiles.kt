package com.neteinstein.networth.feature.importer.domain.parse.bank

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.model.ParsedHolding
import com.neteinstein.networth.feature.importer.domain.model.ParsedScheduledPayment
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.ParseFailure
import com.neteinstein.networth.feature.importer.domain.parse.Values
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine

/** Turns the text lines of a bank's PDF statement into a [ParsedStatement]. */
internal interface PdfStatementProfile {
    fun matches(lines: List<PdfTextLine>): Boolean

    fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement
}

internal object PdfStatements {
    val profiles: List<PdfStatementProfile> = listOf(NovobancoProfile, BancoBestProfile)

    fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement {
        val profile = profiles.firstOrNull { it.matches(lines) } ?: GenericPdfProfile
        val statement = profile.parse(fileName, lines)
        if (statement.sections.isEmpty() && statement.holdings.isEmpty()) {
            throw ParseException(ParseFailure.NO_PDF_MOVEMENTS, "No movements found in this PDF. Is it a bank statement?")
        }
        return statement
    }
}

/**
 * novobanco "Extrato Integrado". Each account's movements start with
 * `CONTA <name> nº <number> de dd.mm.yyyy a dd.mm.yyyy` (repeated on every page), followed by
 * `Data | Data Valor | Descritivo | Débito | Crédito | Saldo`, `SALDO ANTERIOR` and
 * `SALDO CONTABILÍSTICO`. Page 1 has the IBAN and the agenda of scheduled payments; the
 * "Crédito Habitação" detail has the mortgage's outstanding capital.
 */
internal object NovobancoProfile : PdfStatementProfile {
    private val sectionHeader =
        Regex("CONTA (.+?) n[º°o] ([\\d ]+?) de (\\d{2}\\.\\d{2}\\.\\d{4}) a (\\d{2}\\.\\d{2}\\.\\d{4})", RegexOption.IGNORE_CASE)
    private val primaryAccount = Regex("Conta à Ordem n[º°o] ([\\d ]+)", RegexOption.IGNORE_CASE)
    private val statementDate = Regex("Data Extrato Atual\\s+(\\d{2}\\.\\d{2}\\.\\d{4})", RegexOption.IGNORE_CASE)

    override fun matches(lines: List<PdfTextLine>): Boolean = lines.any {
        it.text.contains("novobanco", true) ||
            it.text.contains("Novo Banco", true)
    }

    override fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement {
        val date = lines.firstNotNullOfOrNull { statementDate.find(it.text) }?.let { Values.date(it.groupValues[1]) }
        val primary = lines.firstNotNullOfOrNull { primaryAccount.find(it.text) }?.groupValues?.get(1)?.let(Values::digits)
        val iban = lines.firstOrNull { it.text.startsWith("IBAN", true) }?.let { Values.iban(it.text.removePrefix("IBAN").trim()) }

        val sections = LinkedHashMap<String, MovementTable>()
        var current: MovementTable? = null
        val holdings = ArrayList<ParsedHolding>()
        val scheduled = ArrayList<ParsedScheduledPayment>()
        var creditType: AccountType? = null
        var agendaPage = -1

        for (line in lines) {
            val text = line.text
            val header = sectionHeader.find(text)
            if (header != null) {
                val number = Values.digits(header.groupValues[2])
                val label = header.groupValues[1].trim()
                current =
                    sections.getOrPut(number) {
                        MovementTable(
                            label = label,
                            accountNumber = number,
                            type = accountTypeFor(label),
                            periodStart = Values.date(header.groupValues[3]),
                            periodEnd = Values.date(header.groupValues[4]),
                            iban = iban.takeIf { number == primary },
                        )
                    }
                creditType = null
                continue
            }
            val upper = text.uppercase()
            when {
                upper.startsWith("AGENDA DE MOVIMENTOS PREVISTOS") -> {
                    agendaPage = line.page
                    continue
                }
                upper == "CRÉDITO HABITAÇÃO" -> {
                    creditType = AccountType.MORTGAGE
                    current = null
                    continue
                }
                upper.startsWith("CRÉDITO PESSOAL") || upper.startsWith("CRÉDITO AUTOMÓVEL") || upper == "OUTRO CRÉDITO" -> {
                    creditType = AccountType.LOAN
                    current = null
                    continue
                }
                upper.startsWith("DETALHE DO PATRIM") || upper.startsWith("AVISOS DE LAN") || upper.startsWith("SEGUROS") -> {
                    current = null
                    if (!upper.startsWith("DETALHE")) creditType = null
                    continue
                }
            }
            val table = current
            if (table != null && !table.closed) {
                table.accept(line)
                continue
            }
            val cells = line.cells
            if (line.page == agendaPage && cells.size >= 3) {
                val due = cells.firstNotNullOfOrNull { Values.date(it.text) }
                val amount = cells.last().takeIf { Values.isAmount(it.text) }?.let { Values.amountMinor(it.text) }
                if (due != null && amount != null && !Values.isDate(cells.first().text)) {
                    scheduled += ParsedScheduledPayment(cells.first().text, due, -amount, primary)
                }
            }
            val type = creditType
            if (type != null &&
                date != null &&
                cells.size >= 3 &&
                Values.digits(cells[1].text).length >= 6 &&
                cells[1].text.all { it.isDigit() || it == ' ' }
            ) {
                val owed = cells.last().takeIf { Values.isAmount(it.text) }?.let { Values.amountMinor(it.text) }
                if (owed != null) holdings += ParsedHolding(cells.first().text, type, owed, date, Values.digits(cells[1].text))
            }
        }
        return ParsedStatement(
            fileName = fileName,
            format = StatementFormat.PDF,
            bank = "novobanco",
            sections = sections.values.map { it.build() },
            holdings = holdings,
            scheduled = scheduled,
        )
    }
}

/**
 * Banco Best "Extrato Integrado". In "Detalhe de movimentos" each account starts with the line
 * `N.º Conta | Designação | Data Início | Data Fim | Moeda` and then `<number> | <name> | start | end | EUR`,
 * under a group heading ("Depósitos à ordem", "Depósitos a prazo e contas poupança"). Rows run from
 * `Saldo inicial` to `Saldo contabilístico`. Investment funds are listed in "Detalhe dos ativos".
 */
internal object BancoBestProfile : PdfStatementProfile {
    private val statementDate = Regex("Data do extrato atual:\\s*(\\d{2}-\\d{2}-\\d{2,4})", RegexOption.IGNORE_CASE)

    override fun matches(lines: List<PdfTextLine>): Boolean =
        lines.any { it.text.contains("bancobest", true) || it.text.contains("Banco Electrónico de Serviço Total", true) }

    override fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement {
        val date = lines.firstNotNullOfOrNull { statementDate.find(it.text) }?.let { Values.date(it.groupValues[1]) }
        val ibans = HashMap<String, String>()
        lines.forEachIndexed { i, line ->
            if (line.text.startsWith("NIB:", true) && i > 0) {
                val number = Values.digits(lines[i - 1].cells.first().text)
                Values.ibanFromNib(line.text.substringAfter(':'))?.let { if (number.isNotEmpty()) ibans[number] = it }
            }
        }

        val sections = LinkedHashMap<String, MovementTable>()
        var current: MovementTable? = null
        var group = AccountType.CURRENT
        var inMovements = false
        var inFunds = false
        var expectAccountLine = false
        val holdings = ArrayList<ParsedHolding>()

        for (line in lines) {
            val text = line.text
            val lower = text.lowercase()
            when {
                lower == "detalhe de movimentos" -> {
                    inMovements = true
                    inFunds = false
                    continue
                }
                lower == "avisos de lançamento" -> {
                    inMovements = false
                    current = null
                    continue
                }
                lower == "depósitos à ordem" -> {
                    group = AccountType.CURRENT
                    inFunds = false
                    continue
                }
                lower.startsWith("depósitos a prazo") -> {
                    group = AccountType.TERM_DEPOSIT
                    inFunds = false
                    continue
                }
                lower == "fundos de investimento" -> {
                    inFunds = !inMovements
                    continue
                }
                lower.startsWith("n.º conta") && lower.contains("data início") -> {
                    expectAccountLine = true
                    current = null
                    continue
                }
            }
            val cells = line.cells
            if (expectAccountLine) {
                expectAccountLine = false
                val number = Values.digits(cells.first().text)
                if (number.length >= 6 && cells.size >= 4) {
                    val label = cells[1].text
                    val dates = cells.mapNotNull { Values.date(it.text) }
                    val guessed = accountTypeFor(label)
                    current =
                        sections.getOrPut(number) {
                            MovementTable(
                                label = label,
                                accountNumber = number,
                                type = if (group ==
                                    AccountType.CURRENT
                                ) {
                                    AccountType.CURRENT
                                } else {
                                    guessed.takeIf { it != AccountType.CURRENT } ?: group
                                },
                                periodStart = dates.getOrNull(0),
                                periodEnd = dates.getOrNull(1),
                                iban = ibans[number],
                            )
                        }
                    continue
                }
            }
            val table = current
            if (inMovements && table != null && !table.closed) {
                table.accept(line)
                continue
            }
            if (inFunds && date != null && cells.size >= 3 && Values.digits(cells.first().text).length >= 6) {
                val value = cells.last().takeIf { Values.isAmount(it.text) }?.let { Values.amountMinor(it.text) }
                if (value !=
                    null
                ) {
                    holdings += ParsedHolding(cells[1].text, AccountType.INVESTMENT, value, date, Values.digits(cells.first().text))
                }
            }
        }
        return ParsedStatement(
            fileName = fileName,
            format = StatementFormat.PDF,
            bank = "Banco Best",
            sections = sections.values.map { it.build() },
            holdings = holdings,
        )
    }
}

/**
 * Any other bank: one account, rows that start with a date and end with the movement (and usually
 * the running balance). The first IBAN in the file identifies the account.
 */
internal object GenericPdfProfile : PdfStatementProfile {
    override fun matches(lines: List<PdfTextLine>): Boolean = true

    override fun parse(
        fileName: String,
        lines: List<PdfTextLine>,
    ): ParsedStatement {
        val iban = lines.firstNotNullOfOrNull { line ->
            line.text.takeIf { it.contains("IBAN", true) }?.let { Values.iban(it.substringAfter("IBAN").substringAfter(':')) }
        }
        val table =
            MovementTable(
                label = "Imported account",
                accountNumber = null,
                type = AccountType.CURRENT,
                periodStart = null,
                periodEnd = null,
                iban = iban,
            )
        lines.forEach { table.accept(it) }
        val section: ParsedSection = table.build()
        return ParsedStatement(
            fileName = fileName,
            format = StatementFormat.PDF,
            sections = if (section.rows.isEmpty()) emptyList() else listOf(section),
        )
    }
}

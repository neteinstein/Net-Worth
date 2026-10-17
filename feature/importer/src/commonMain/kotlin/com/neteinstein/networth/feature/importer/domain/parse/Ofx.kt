package com.neteinstein.networth.feature.importer.domain.parse

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.model.ParsedRow
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat

/** OFX 1.x (SGML, unclosed tags) and 2.x (XML). One section per bank or credit-card statement. */
internal object Ofx {
    private val statement =
        Regex("<(STMTRS|CCSTMTRS)>(.*?)(?:</\\1>|(?=<(?:STMTRS|CCSTMTRS)>)|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val transaction =
        Regex("<STMTTRN>(.*?)(?:</STMTTRN>|(?=<STMTTRN>)|(?=</BANKTRANLIST>))", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    fun parse(
        fileName: String,
        text: String,
    ): ParsedStatement {
        val sections =
            statement.findAll(text).map { match ->
                val body = match.groupValues[2]
                val isCard = match.groupValues[1].equals("CCSTMTRS", true)
                val accountId = tag(body, "ACCTID")
                val rows =
                    transaction.findAll(body).mapNotNull { tx ->
                        val t = tx.groupValues[1]
                        val date = tag(t, "DTPOSTED")?.let(Values::date) ?: return@mapNotNull null
                        val amount = tag(t, "TRNAMT")?.let(Values::amountMinor) ?: return@mapNotNull null
                        val name = tag(t, "NAME").orEmpty()
                        val memo = tag(t, "MEMO").orEmpty()
                        ParsedRow(
                            date = date,
                            description = listOf(name, memo).filter {
                                it.isNotBlank()
                            }.distinct().joinToString(" ").ifBlank { tag(t, "TRNTYPE").orEmpty() },
                            amountMinor = amount,
                            externalId = tag(t, "FITID"),
                        )
                    }.sortedBy { it.date }.toList()
                val ledger = body.substringAfter("<LEDGERBAL>", "")
                ParsedSection(
                    label = if (isCard) "Credit card" else "Imported account",
                    accountNumber = accountId?.let(Values::digits)?.takeIf { it.isNotEmpty() },
                    iban = accountId?.let(Values::iban),
                    suggestedType = if (tag(body, "ACCTTYPE").equals("SAVINGS", true)) AccountType.SAVINGS else AccountType.CURRENT,
                    periodStart = tag(body, "DTSTART")?.let(Values::date),
                    periodEnd = tag(body, "DTEND")?.let(Values::date),
                    closingBalanceMinor = tag(ledger, "BALAMT")?.let(Values::amountMinor),
                    rows = rows,
                )
            }.filter { it.rows.isNotEmpty() || it.closingBalanceMinor != null }.toList()
        if (sections.isEmpty()) throw ParseException("No statement found in this OFX file")
        return ParsedStatement(fileName = fileName, format = StatementFormat.OFX, sections = sections)
    }

    private fun tag(
        body: String,
        name: String,
    ): String? =
        Regex("<$name>([^<\\r\\n]*)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.trim()
            ?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")
            ?.takeIf { it.isNotEmpty() }
}

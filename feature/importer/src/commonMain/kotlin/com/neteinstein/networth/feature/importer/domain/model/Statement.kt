package com.neteinstein.networth.feature.importer.domain.model

import com.neteinstein.networth.core.domain.model.AccountType
import kotlinx.datetime.LocalDate

enum class StatementFormat(
    val label: String,
) {
    CSV("CSV"),
    XLSX("XLSX"),
    OFX("OFX"),
    PDF("PDF"),
}

/** What a bank file contains, before it is matched to the family's accounts. */
data class ParsedStatement(
    val fileName: String,
    val format: StatementFormat,
    val bank: String? = null,
    val sections: List<ParsedSection>,
    /** Balances of products without movements here: mortgages, loans, investment funds. */
    val holdings: List<ParsedHolding> = emptyList(),
    val scheduled: List<ParsedScheduledPayment> = emptyList(),
) {
    val rowCount: Int get() = sections.sumOf { it.rows.size }
}

/** The movements of one bank account in the file. Most files have one; integrated statements have several. */
data class ParsedSection(
    val label: String,
    val accountNumber: String? = null,
    val iban: String? = null,
    val suggestedType: AccountType = AccountType.CURRENT,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val openingBalanceMinor: Long? = null,
    val closingBalanceMinor: Long? = null,
    val rows: List<ParsedRow> = emptyList(),
) {
    /** The statement's period, or the span of its rows when the file doesn't say. */
    val start: LocalDate? get() = periodStart ?: rows.minOfOrNull { it.date }
    val end: LocalDate? get() = periodEnd ?: rows.maxOfOrNull { it.date }
}

data class ParsedRow(
    val date: LocalDate,
    val description: String,
    /** Signed: negative is money leaving the account. */
    val amountMinor: Long,
    val valueDate: LocalDate? = null,
    val balanceMinor: Long? = null,
    /** A bank-issued id (OFX FITID), the best key for spotting duplicates. */
    val externalId: String? = null,
)

data class ParsedHolding(
    val label: String,
    val type: AccountType,
    /** Positive; for a mortgage or loan it is the amount still owed. */
    val balanceMinor: Long,
    val date: LocalDate,
    val accountNumber: String? = null,
)

data class ParsedScheduledPayment(
    val description: String,
    val date: LocalDate,
    /** Signed like a transaction: an instalment is negative. */
    val amountMinor: Long,
    val accountNumber: String? = null,
)

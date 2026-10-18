package com.neteinstein.networth.feature.importer.domain.model

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.repository.CategorizerFailure
import com.neteinstein.networth.core.domain.text.DescriptionInfo
import kotlinx.datetime.LocalDate

/** Where one section of a statement goes: an existing account, or a new one with these details. */
data class SectionTarget(
    val sectionIndex: Int,
    /** An existing account, or null to create one from [name]/[type]. */
    val accountId: String?,
    val name: String,
    val type: AccountType,
    /** Who the account belongs to; more than one for joint accounts. Replaces the account's owners. */
    val ownerIds: List<String>,
)

data class ImportRequest(
    val statement: ParsedStatement,
    val targets: List<SectionTarget>,
)

/** What an import did, for the summary card on the Import screen. */
data class ImportSummary(
    val fileName: String,
    val accountNames: List<String>,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val rowsRead: Int,
    val duplicatesSkipped: Int,
    val imported: Int,
    val autoCategorised: Int,
    val internalTransfers: Int,
    val needReview: Int,
    /** Earlier imports the duplicates came from (to explain where the overlap was). */
    val overlappedImportIds: List<String>,
    /** Sections whose movements don't add up from the opening to the closing balance. */
    val unreconciledAccounts: List<String>,
    /** The account each imported section went to, by section index (new accounts included). */
    val sectionAccountIds: Map<Int, String> = emptyMap(),
    /** How Claude did on what rules couldn't file; null when Claude isn't set up or nothing was left to file. */
    val claude: ClaudeOutcome? = null,
) {
    /** Movements that count as spending or income, i.e. not internal transfers. */
    val categorisable: Int get() = imported - internalTransfers
}

/** What Claude filed of the movements no rule could, or why it couldn't. Its count is part of [ImportSummary.autoCategorised]. */
data class ClaudeOutcome(
    val filed: Int,
    val failure: CategorizerFailure? = null,
)

enum class MonthCoverage {
    /** Every day of the month is covered by imported statements. */
    FULL,

    /** Some days are covered, or there are movements without a statement for the whole month. */
    PARTIAL,
    MISSING,
}

data class AccountCoverage(
    val account: Account,
    /** Oldest first. */
    val months: List<Pair<CalendarMonth, MonthCoverage>>,
) {
    val missingCount: Int get() = months.count { it.second != MonthCoverage.FULL }
}

/** One transaction waiting for a category, with what Net Worth can suggest. */
data class ReviewItem(
    val transaction: Transaction,
    val info: DescriptionInfo,
    val suggestedCategoryId: String?,
    /** Past choices for the same merchant: (times filed under the suggestion, times seen). */
    val history: Pair<Int, Int>?,
    /** Categories to offer as chips, suggestion first. */
    val choiceIds: List<String>,
    /** MB WAY and batched card payments have no merchant name, so a note helps later. */
    val needsNote: Boolean,
    /** What a learned rule would match on, or null when nothing can be learned (no merchant). */
    val ruleKey: String?,
)

data class ReviewChoice(
    val transactionId: String,
    val categoryId: String,
    val subcategoryId: String? = null,
    val note: String? = null,
    val remember: Boolean = false,
)

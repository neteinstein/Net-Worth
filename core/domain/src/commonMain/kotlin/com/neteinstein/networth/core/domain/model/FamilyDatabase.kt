package com.neteinstein.networth.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * Everything the family shares, as stored in one Google Drive file.
 *
 * Every collection holds live records only. Deleting a record removes it and adds its id to
 * [tombstones] (id → deletion time in epoch millis) so a merge with another device's copy does not
 * bring it back. Records carry `updatedAt` (epoch millis) for record-level last-writer-wins merges.
 */
data class FamilyDatabase(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String,
    val settings: HouseholdSettings,
    val members: List<Member> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val balanceSnapshots: List<BalanceSnapshot> = emptyList(),
    val scheduledPayments: List<ScheduledPayment> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val imports: List<ImportRecord> = emptyList(),
    val merchantRules: List<MerchantRule> = emptyList(),
    val savedFilters: List<SavedFilter> = emptyList(),
    val tombstones: Map<String, Long> = emptyMap(),
) {
    val currency: CurrencyCode get() = CurrencyCode.of(settings.currencyCode)

    fun account(id: String): Account? = accounts.firstOrNull { it.id == id }

    fun category(id: String?): Category? = id?.let { categoryId -> categories.firstOrNull { it.id == categoryId } }

    fun member(id: String): Member? = members.firstOrNull { it.id == id }
}

/** Anything that merges record-by-record between devices. */
interface Syncable {
    val id: String
    val updatedAt: Long
}

/** Household-wide singletons, merged as one record. */
data class HouseholdSettings(
    val householdName: String,
    val currencyCode: String = CurrencyCode.DEFAULT.code,
    val askAccountOnImport: Boolean = true,
    val alerts: AlertSettings = AlertSettings(),
    val assumptions: Assumptions = Assumptions(),
    val updatedAt: Long = 0,
)

enum class MemberRole { ADULT, CHILD }

data class Member(
    override val id: String,
    val name: String,
    val role: MemberRole,
    override val updatedAt: Long = 0,
) : Syncable

enum class AccountType(val isLiability: Boolean) {
    CURRENT(false),
    SAVINGS(false),
    TERM_DEPOSIT(false),
    INVESTMENT(false),
    MORTGAGE(true),
    LOAN(true),
}

data class Account(
    override val id: String,
    val name: String,
    val type: AccountType,
    val ownerIds: List<String> = emptyList(),
    val bank: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    override val updatedAt: Long = 0,
) : Syncable

/** A known balance on a date, e.g. a statement's closing balance or one typed in by hand. */
data class BalanceSnapshot(
    override val id: String,
    val accountId: String,
    val date: LocalDate,
    val amountMinor: Long,
    override val updatedAt: Long = 0,
) : Syncable

data class ScheduledPayment(
    override val id: String,
    val accountId: String,
    val description: String,
    val date: LocalDate,
    val amountMinor: Long,
    override val updatedAt: Long = 0,
) : Syncable

data class Subcategory(
    val id: String,
    val name: String,
)

data class Category(
    override val id: String,
    val name: String,
    val colorHex: String,
    val subcategories: List<Subcategory> = emptyList(),
    val position: Int = 0,
    val monthlyBudgetMinor: Long? = null,
    /** 1–12, a month in which this category is expected to peak (e.g. back to school). */
    val seasonalPeakMonth: Int? = null,
    val warnOnSpike: Boolean = true,
    override val updatedAt: Long = 0,
) : Syncable

/**
 * One bank movement. [amountMinor] is signed: negative is money leaving the account.
 * Internal transfers (between the family's own accounts) are never spending or income.
 */
data class Transaction(
    override val id: String,
    val accountId: String,
    val date: LocalDate,
    val amountMinor: Long,
    val description: String,
    val valueDate: LocalDate? = null,
    val note: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val memberIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val isInternalTransfer: Boolean = false,
    val importId: String? = null,
    val fingerprint: String? = null,
    override val updatedAt: Long = 0,
) : Syncable {
    val isOutflow: Boolean get() = amountMinor < 0
}

data class ImportRecord(
    override val id: String,
    val accountId: String,
    val fileName: String,
    val format: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val rowsRead: Int,
    val duplicatesSkipped: Int,
    val importedAt: Long,
    val importedBy: String,
    override val updatedAt: Long = 0,
) : Syncable

enum class RuleMatch { PREFIX, CONTAINS, PHONE }

data class MerchantRule(
    override val id: String,
    val pattern: String,
    val match: RuleMatch,
    val categoryId: String,
    val subcategoryId: String? = null,
    val hits: Int = 0,
    override val updatedAt: Long = 0,
) : Syncable

enum class TransactionTypeFilter { EXPENSES, INCOME, BOTH }

data class TransactionFilter(
    val accountIds: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val type: TransactionTypeFilter = TransactionTypeFilter.EXPENSES,
    val includeTransfers: Boolean = false,
    val minMinor: Long? = null,
    val maxMinor: Long? = null,
    val text: String? = null,
)

data class SavedFilter(
    override val id: String,
    val name: String,
    val filter: TransactionFilter,
    override val updatedAt: Long = 0,
) : Syncable

data class SeasonalMark(
    val categoryId: String,
    val month: Int,
    val year: Int,
)

data class AlertSettings(
    val spikeThresholdPct: Int = 40,
    val largePaymentMinor: Long = 25_000,
    val newSubscriptions: Boolean = true,
    val seasonalMarks: List<SeasonalMark> = emptyList(),
    val dismissed: List<String> = emptyList(),
)

data class Assumptions(
    val returnRatePct: Double = 3.0,
    val incomeGrowthPct: Double = 2.0,
    val priceRisePct: Double = 2.5,
    val emergencyFundMonths: Int = 6,
)

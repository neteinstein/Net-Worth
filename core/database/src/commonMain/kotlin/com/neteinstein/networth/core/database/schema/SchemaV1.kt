package com.neteinstein.networth.core.database.schema

import kotlinx.serialization.Serializable

/*
 * The on-disk JSON format of the family database (schema version 1). These classes are the file
 * contract shared by every device and app version: add fields with defaults, never rename or remove
 * them without bumping SCHEMA_VERSION and adding a migration in FamilyDatabaseCodec.
 * Dates are ISO-8601 (`2026-09-30`), amounts are integer minor units (cents), times epoch millis.
 */

const val FORMAT_ID = "net-worth-family-database"
const val SCHEMA_VERSION = 1

@Serializable
internal data class DatabaseDto(
    val format: String = FORMAT_ID,
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String,
    val settings: SettingsDto,
    val members: List<MemberDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val balanceSnapshots: List<BalanceSnapshotDto> = emptyList(),
    val scheduledPayments: List<ScheduledPaymentDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val imports: List<ImportDto> = emptyList(),
    val merchantRules: List<MerchantRuleDto> = emptyList(),
    val savedFilters: List<SavedFilterDto> = emptyList(),
    val tombstones: Map<String, Long> = emptyMap(),
)

@Serializable
internal data class SettingsDto(
    val householdName: String,
    val currency: String = "EUR",
    val askAccountOnImport: Boolean = true,
    val alerts: AlertSettingsDto = AlertSettingsDto(),
    val assumptions: AssumptionsDto = AssumptionsDto(),
    val updatedAt: Long = 0,
)

@Serializable
internal data class AlertSettingsDto(
    val spikeThresholdPct: Int = 40,
    val largePaymentMinor: Long = 25_000,
    val newSubscriptions: Boolean = true,
    val seasonalMarks: List<SeasonalMarkDto> = emptyList(),
    val dismissed: List<String> = emptyList(),
)

@Serializable
internal data class SeasonalMarkDto(
    val categoryId: String,
    val month: Int,
    val year: Int,
)

@Serializable
internal data class AssumptionsDto(
    val returnRatePct: Double = 3.0,
    val incomeGrowthPct: Double = 2.0,
    val priceRisePct: Double = 2.5,
    val emergencyFundMonths: Int = 6,
)

@Serializable
internal data class MemberDto(
    val id: String,
    val name: String,
    val role: String,
    val updatedAt: Long = 0,
)

@Serializable
internal data class AccountDto(
    val id: String,
    val name: String,
    val type: String,
    val ownerIds: List<String> = emptyList(),
    val bank: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    val updatedAt: Long = 0,
)

@Serializable
internal data class BalanceSnapshotDto(
    val id: String,
    val accountId: String,
    val date: String,
    val amountMinor: Long,
    val updatedAt: Long = 0,
)

@Serializable
internal data class ScheduledPaymentDto(
    val id: String,
    val accountId: String,
    val description: String,
    val date: String,
    val amountMinor: Long,
    val updatedAt: Long = 0,
)

@Serializable
internal data class SubcategoryDto(
    val id: String,
    val name: String,
)

@Serializable
internal data class CategoryDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val subcategories: List<SubcategoryDto> = emptyList(),
    val position: Int = 0,
    val monthlyBudgetMinor: Long? = null,
    val seasonalPeakMonth: Int? = null,
    val warnOnSpike: Boolean = true,
    val updatedAt: Long = 0,
)

@Serializable
internal data class TransactionDto(
    val id: String,
    val accountId: String,
    val date: String,
    val amountMinor: Long,
    val description: String,
    val valueDate: String? = null,
    val note: String? = null,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val memberIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val internalTransfer: Boolean = false,
    val importId: String? = null,
    val fingerprint: String? = null,
    val updatedAt: Long = 0,
)

@Serializable
internal data class ImportDto(
    val id: String,
    val accountId: String,
    val fileName: String,
    val format: String,
    val periodStart: String,
    val periodEnd: String,
    val rowsRead: Int,
    val duplicatesSkipped: Int,
    val importedAt: Long,
    val importedBy: String,
    val updatedAt: Long = 0,
)

@Serializable
internal data class MerchantRuleDto(
    val id: String,
    val pattern: String,
    val match: String,
    val categoryId: String,
    val subcategoryId: String? = null,
    val hits: Int = 0,
    val updatedAt: Long = 0,
)

@Serializable
internal data class FilterDto(
    val accountIds: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val type: String = "EXPENSES",
    val includeTransfers: Boolean = false,
    val minMinor: Long? = null,
    val maxMinor: Long? = null,
    val text: String? = null,
)

@Serializable
internal data class SavedFilterDto(
    val id: String,
    val name: String,
    val filter: FilterDto,
    val updatedAt: Long = 0,
)

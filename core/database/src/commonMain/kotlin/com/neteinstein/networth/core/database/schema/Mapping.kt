package com.neteinstein.networth.core.database.schema

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.AlertSettings
import com.neteinstein.networth.core.domain.model.Assumptions
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.HouseholdSettings
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.model.ScheduledPayment
import com.neteinstein.networth.core.domain.model.SeasonalMark
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import kotlinx.datetime.LocalDate

// Enum values are stored by name; unknown names (from a newer app) fall back to a safe default.
private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E = enumValues<E>().firstOrNull { it.name == name } ?: fallback

internal fun DatabaseDto.toDomain() = FamilyDatabase(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    settings = settings.toDomain(),
    members = members.map { Member(it.id, it.name, enumOr(it.role, MemberRole.ADULT), it.updatedAt) },
    accounts = accounts.map {
        Account(it.id, it.name, enumOr(it.type, AccountType.CURRENT), it.ownerIds, it.bank, it.accountNumber, it.iban, it.updatedAt)
    },
    balanceSnapshots = balanceSnapshots.map {
        BalanceSnapshot(it.id, it.accountId, LocalDate.parse(it.date), it.amountMinor, it.updatedAt)
    },
    scheduledPayments = scheduledPayments.map {
        ScheduledPayment(it.id, it.accountId, it.description, LocalDate.parse(it.date), it.amountMinor, it.updatedAt)
    },
    categories = categories.map { c ->
        Category(
            id = c.id,
            name = c.name,
            colorHex = c.colorHex,
            subcategories = c.subcategories.map { Subcategory(it.id, it.name) },
            position = c.position,
            monthlyBudgetMinor = c.monthlyBudgetMinor,
            seasonalPeakMonth = c.seasonalPeakMonth,
            warnOnSpike = c.warnOnSpike,
            updatedAt = c.updatedAt,
        )
    },
    transactions = transactions.map { t ->
        Transaction(
            id = t.id,
            accountId = t.accountId,
            date = LocalDate.parse(t.date),
            amountMinor = t.amountMinor,
            description = t.description,
            valueDate = t.valueDate?.let(LocalDate::parse),
            note = t.note,
            categoryId = t.categoryId,
            subcategoryId = t.subcategoryId,
            memberIds = t.memberIds,
            tags = t.tags,
            isInternalTransfer = t.internalTransfer,
            importId = t.importId,
            fingerprint = t.fingerprint,
            updatedAt = t.updatedAt,
        )
    },
    imports = imports.map {
        ImportRecord(
            id = it.id,
            accountId = it.accountId,
            fileName = it.fileName,
            format = it.format,
            periodStart = LocalDate.parse(it.periodStart),
            periodEnd = LocalDate.parse(it.periodEnd),
            rowsRead = it.rowsRead,
            duplicatesSkipped = it.duplicatesSkipped,
            importedAt = it.importedAt,
            importedBy = it.importedBy,
            updatedAt = it.updatedAt,
        )
    },
    merchantRules = merchantRules.map {
        MerchantRule(it.id, it.pattern, enumOr(it.match, RuleMatch.CONTAINS), it.categoryId, it.subcategoryId, it.hits, it.updatedAt)
    },
    savedFilters = savedFilters.map { SavedFilter(it.id, it.name, it.filter.toDomain(), it.updatedAt) },
    tombstones = tombstones,
)

private fun SettingsDto.toDomain() = HouseholdSettings(
    householdName = householdName,
    currencyCode = currency,
    askAccountOnImport = askAccountOnImport,
    alerts = AlertSettings(
        spikeThresholdPct = alerts.spikeThresholdPct,
        largePaymentMinor = alerts.largePaymentMinor,
        newSubscriptions = alerts.newSubscriptions,
        seasonalMarks = alerts.seasonalMarks.map { SeasonalMark(it.categoryId, it.month, it.year) },
        dismissed = alerts.dismissed,
    ),
    assumptions = Assumptions(
        assumptions.returnRatePct,
        assumptions.incomeGrowthPct,
        assumptions.priceRisePct,
        assumptions.emergencyFundMonths,
    ),
    updatedAt = updatedAt,
)

private fun FilterDto.toDomain() = TransactionFilter(
    accountIds = accountIds,
    categoryIds = categoryIds,
    memberIds = memberIds,
    type = enumOr(type, TransactionTypeFilter.EXPENSES),
    includeTransfers = includeTransfers,
    minMinor = minMinor,
    maxMinor = maxMinor,
    text = text,
)

internal fun FamilyDatabase.toDto() = DatabaseDto(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    settings = SettingsDto(
        householdName = settings.householdName,
        currency = settings.currencyCode,
        askAccountOnImport = settings.askAccountOnImport,
        alerts = AlertSettingsDto(
            spikeThresholdPct = settings.alerts.spikeThresholdPct,
            largePaymentMinor = settings.alerts.largePaymentMinor,
            newSubscriptions = settings.alerts.newSubscriptions,
            seasonalMarks = settings.alerts.seasonalMarks.map { SeasonalMarkDto(it.categoryId, it.month, it.year) },
            dismissed = settings.alerts.dismissed,
        ),
        assumptions = settings.assumptions.let {
            AssumptionsDto(it.returnRatePct, it.incomeGrowthPct, it.priceRisePct, it.emergencyFundMonths)
        },
        updatedAt = settings.updatedAt,
    ),
    members = members.map { MemberDto(it.id, it.name, it.role.name, it.updatedAt) },
    accounts = accounts.map { AccountDto(it.id, it.name, it.type.name, it.ownerIds, it.bank, it.accountNumber, it.iban, it.updatedAt) },
    balanceSnapshots = balanceSnapshots.map { BalanceSnapshotDto(it.id, it.accountId, it.date.toString(), it.amountMinor, it.updatedAt) },
    scheduledPayments = scheduledPayments.map {
        ScheduledPaymentDto(it.id, it.accountId, it.description, it.date.toString(), it.amountMinor, it.updatedAt)
    },
    categories = categories.map { c ->
        CategoryDto(
            id = c.id,
            name = c.name,
            colorHex = c.colorHex,
            subcategories = c.subcategories.map { SubcategoryDto(it.id, it.name) },
            position = c.position,
            monthlyBudgetMinor = c.monthlyBudgetMinor,
            seasonalPeakMonth = c.seasonalPeakMonth,
            warnOnSpike = c.warnOnSpike,
            updatedAt = c.updatedAt,
        )
    },
    transactions = transactions.map { t ->
        TransactionDto(
            id = t.id,
            accountId = t.accountId,
            date = t.date.toString(),
            amountMinor = t.amountMinor,
            description = t.description,
            valueDate = t.valueDate?.toString(),
            note = t.note,
            categoryId = t.categoryId,
            subcategoryId = t.subcategoryId,
            memberIds = t.memberIds,
            tags = t.tags,
            internalTransfer = t.isInternalTransfer,
            importId = t.importId,
            fingerprint = t.fingerprint,
            updatedAt = t.updatedAt,
        )
    },
    imports = imports.map {
        ImportDto(
            id = it.id,
            accountId = it.accountId,
            fileName = it.fileName,
            format = it.format,
            periodStart = it.periodStart.toString(),
            periodEnd = it.periodEnd.toString(),
            rowsRead = it.rowsRead,
            duplicatesSkipped = it.duplicatesSkipped,
            importedAt = it.importedAt,
            importedBy = it.importedBy,
            updatedAt = it.updatedAt,
        )
    },
    merchantRules = merchantRules.map {
        MerchantRuleDto(it.id, it.pattern, it.match.name, it.categoryId, it.subcategoryId, it.hits, it.updatedAt)
    },
    savedFilters = savedFilters.map { s ->
        SavedFilterDto(
            id = s.id,
            name = s.name,
            filter = s.filter.let {
                FilterDto(it.accountIds, it.categoryIds, it.memberIds, it.type.name, it.includeTransfers, it.minMinor, it.maxMinor, it.text)
            },
            updatedAt = s.updatedAt,
        )
    },
    tombstones = tombstones,
)

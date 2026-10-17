package com.neteinstein.networth.feature.spending.ui.screens

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.feature.spending.domain.model.DateRange
import com.neteinstein.networth.feature.spending.domain.model.PeriodKind
import com.neteinstein.networth.feature.spending.domain.usecase.CategoryRow
import com.neteinstein.networth.feature.spending.domain.usecase.Spending
import kotlinx.datetime.LocalDate

/** Example data from the Spending artboard (names and figures are the design's, not seed data). */
internal fun previewSpendingState(): SpendingUiState {
    val rows =
        listOf(
            CategoryRow("h", "Housing", "#141B2D", 1_180_00, 1_170_00, false),
            CategoryRow("g", "Groceries", "#2D55D6", 920_00, 905_00, false),
            CategoryRow("s", "School", "#D9652B", 610_00, 285_00, true),
            CategoryRow("t", "Transport", "#7D9BF2", 395_00, 380_00, false),
            CategoryRow("c", "Clothes", "#A8487A", 285_00, 195_00, true),
            CategoryRow("d", "Dining & leisure", "#6A4FC9", 230_00, 290_00, false),
            CategoryRow(null, null, null, 45_00, null, false),
        )
    return SpendingUiState(
        spending =
            Spending(
                currencyCode = "EUR",
                kind = PeriodKind.MONTH,
                period = DateRange(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30)),
                scopes = listOf(AccountScope.All, AccountScope.Single("a1"), AccountScope.Children),
                scope = AccountScope.All,
                scopeName = null,
                scopeNames = mapOf("a1" to "Pedro"),
                scopeAccountIds = emptyList(),
                totalMinor = rows.sumOf { it.amountMinor },
                perPersonMinor = rows.sumOf { it.amountMinor } / 5,
                shareOfFamilyPercent = null,
                rows = rows,
                savings = null,
                spikeThresholdPct = 40,
            ),
        activeFilters = 2,
    )
}

internal fun previewCategories(): List<Category> =
    listOf(
        Category("s", "School", "#D9652B"),
        Category("k", "Kids’ activities", "#F0A964"),
        Category("c", "Clothes", "#A8487A"),
    )

internal fun previewMembers(): List<Member> =
    listOf(Member("p", "Pedro", MemberRole.ADULT), Member("i", "Inês", MemberRole.CHILD))

internal fun previewTransactions(): List<TransactionLine> {
    val school = previewCategories().first()
    return listOf(
        TransactionLine(Transaction("1", "a1", LocalDate(2026, 9, 3), -385_00, "LIVRARIA ESCOLAR", categoryId = "s"), "Pedro", school),
        TransactionLine(
            Transaction("2", "a1", LocalDate(2026, 9, 3), -45_00, "TRF ASSOC PAIS ESC BASICA", categoryId = "s"),
            "Pedro",
            school,
        ),
    )
}

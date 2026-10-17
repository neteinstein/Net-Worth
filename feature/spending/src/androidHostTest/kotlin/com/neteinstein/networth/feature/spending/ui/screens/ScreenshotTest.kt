package com.neteinstein.networth.feature.spending.ui.screens

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.testing.screenshot
import com.neteinstein.networth.feature.spending.domain.usecase.SavingsFlow
import com.neteinstein.networth.feature.spending.domain.usecase.TransactionEdit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun spending() = screenshot("spending") { SpendingContent(previewSpendingState(), SpendingActions()) }

    @Test
    fun spendingSavings() = screenshot("spending_savings") {
        val state = previewSpendingState()
        SpendingContent(
            state.copy(
                spending = state.spending!!.copy(scope = AccountScope.Children, rows = emptyList(), savings = SavingsFlow(9_000, 0)),
            ),
            SpendingActions(),
        )
    }

    @Test
    fun filters() = screenshot("filters") {
        FiltersContent(
            FiltersUiState(
                ready = true,
                accounts = listOf(Account("a1", "Pedro", AccountType.CURRENT), Account("j", "Joint savings", AccountType.SAVINGS)),
                categories = previewCategories(),
                children = previewMembers().drop(1),
                matchCount = 35,
            ),
            FiltersActions(),
        )
    }

    @Test
    fun transactions() = screenshot("transactions") {
        TransactionsContent(
            TransactionsUiState(ready = true, category = previewCategories().first(), lines = previewTransactions(), totalMinor = -430_00),
            {},
            {},
            {},
            {},
            {},
        )
    }

    @Test
    fun transactionEdit() = screenshot("transaction_edit") {
        TransactionsContent(
            TransactionsUiState(
                ready = true,
                lines = previewTransactions(),
                categories = previewCategories(),
                members = previewMembers(),
                editor = TransactionEditor("1", "LIVRARIA ESCOLAR", -385_00, TransactionEdit("s", null, listOf("i"), null, false)),
            ),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

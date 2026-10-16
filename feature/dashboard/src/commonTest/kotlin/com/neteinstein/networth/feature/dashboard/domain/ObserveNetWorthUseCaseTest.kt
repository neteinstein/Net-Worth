package com.neteinstein.networth.feature.dashboard.domain

import com.neteinstein.networth.feature.dashboard.data.InMemoryAccountRepository
import com.neteinstein.networth.feature.dashboard.domain.model.Account
import com.neteinstein.networth.feature.dashboard.domain.model.AccountCategory
import com.neteinstein.networth.feature.dashboard.domain.model.Money
import com.neteinstein.networth.feature.dashboard.domain.usecase.ObserveNetWorthUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveNetWorthUseCaseTest {
    private fun account(id: String, category: AccountCategory, cents: Long) =
        Account(id, id, category.kind, category, Money(cents))

    @Test
    fun netWorthIsAssetsMinusLiabilities() = runTest {
        val repository = InMemoryAccountRepository(
            listOf(
                account("cash", AccountCategory.CASH, 1_000_00),
                account("etf", AccountCategory.INVESTMENTS, 4_000_00),
                account("loan", AccountCategory.LOAN, 1_500_00),
            ),
        )

        val summary = ObserveNetWorthUseCase(repository)().first()

        assertEquals(Money(5_000_00), summary.totalAssets)
        assertEquals(Money(1_500_00), summary.totalLiabilities)
        assertEquals(Money(3_500_00), summary.netWorth)
        assertEquals(0.3, summary.debtRatio)
    }

    @Test
    fun accountsAreSortedLargestFirst() = runTest {
        val repository = InMemoryAccountRepository(
            listOf(account("small", AccountCategory.CASH, 1), account("big", AccountCategory.CASH, 100)),
        )

        val summary = ObserveNetWorthUseCase(repository)().first()

        assertEquals(listOf("big", "small"), summary.assets.map { it.id })
    }

    @Test
    fun emptyAccountsGiveZeroAndNoDivisionByZero() = runTest {
        val summary = ObserveNetWorthUseCase(InMemoryAccountRepository(emptyList()))().first()

        assertEquals(Money.zero(), summary.netWorth)
        assertEquals(0.0, summary.debtRatio)
    }

    @Test
    fun recomputesWhenAccountsChange() = runTest {
        val repository = InMemoryAccountRepository(emptyList())
        val useCase = ObserveNetWorthUseCase(repository)

        repository.upsert(account("cash", AccountCategory.CASH, 10_00))
        assertEquals(Money(10_00), useCase().first().netWorth)

        repository.upsert(account("cash", AccountCategory.CASH, 25_00))
        assertEquals(Money(25_00), useCase().first().netWorth)

        repository.remove("cash")
        assertEquals(Money.zero(), useCase().first().netWorth)
    }
}

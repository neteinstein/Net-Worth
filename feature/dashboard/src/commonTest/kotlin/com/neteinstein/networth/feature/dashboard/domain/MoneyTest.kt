package com.neteinstein.networth.feature.dashboard.domain

import com.neteinstein.networth.feature.dashboard.domain.model.CurrencyCode
import com.neteinstein.networth.feature.dashboard.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun addsAndSubtractsInMinorUnits() {
        assertEquals(Money(1_50), Money(1_00) + Money(50))
        assertEquals(Money(-25), Money(25) - Money(50))
        assertEquals(Money(-10), -Money(10))
    }

    @Test
    fun comparesAmounts() {
        assertTrue(Money(2) > Money(1))
        assertTrue(Money(-1).isNegative)
    }

    @Test
    fun rejectsMixedCurrencies() {
        assertFailsWith<IllegalArgumentException> { Money(1, CurrencyCode.EUR) + Money(1, CurrencyCode.USD) }
    }
}

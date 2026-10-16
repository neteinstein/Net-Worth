package com.neteinstein.networth.feature.dashboard.ui

import com.neteinstein.networth.feature.dashboard.domain.model.CurrencyCode
import com.neteinstein.networth.feature.dashboard.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatterTest {
    @Test
    fun groupsThousandsAndPadsFraction() {
        assertEquals("€1,234,567.05", MoneyFormatter.format(Money(1_234_567_05)))
        assertEquals("€0.00", MoneyFormatter.format(Money(0)))
        assertEquals("€999.99", MoneyFormatter.format(Money(999_99)))
    }

    @Test
    fun formatsNegativeAmounts() {
        assertEquals("-€80.50", MoneyFormatter.format(Money(-80_50)))
    }

    @Test
    fun canDropFraction() {
        assertEquals("$12,000", MoneyFormatter.format(Money(12_000_99, CurrencyCode.USD), showFraction = false))
    }

    @Test
    fun respectsZeroFractionCurrencies() {
        val yen = CurrencyCode("JPY", "¥", fractionDigits = 0)
        assertEquals("¥1,500", MoneyFormatter.format(Money(1_500, yen)))
    }
}

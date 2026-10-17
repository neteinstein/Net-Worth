package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Money
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

    @Test
    fun wholeRoundsHalfAwayFromZero() {
        assertEquals("€4,120", MoneyFormatter.whole(4_119_50, CurrencyCode.EUR))
        assertEquals("€4,119", MoneyFormatter.whole(4_119_49, CurrencyCode.EUR))
        assertEquals("-€87", MoneyFormatter.whole(-86_50, CurrencyCode.EUR))
    }

    @Test
    fun compactUsesThousands() {
        assertEquals("€950", MoneyFormatter.compact(950_00, CurrencyCode.EUR))
        assertEquals("€5.6k", MoneyFormatter.compact(5_620_00, CurrencyCode.EUR))
        assertEquals("-€1.2k", MoneyFormatter.compact(-1_234_00, CurrencyCode.EUR))
    }

    @Test
    fun unknownCurrencyCodeRoundTrips() {
        assertEquals("CHF", CurrencyCode.of("CHF").code)
        assertEquals(CurrencyCode.GBP, CurrencyCode.of("GBP"))
    }
}

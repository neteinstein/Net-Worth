package com.neteinstein.networth.feature.dashboard.ui

import com.neteinstein.networth.feature.dashboard.domain.model.CurrencyCode
import com.neteinstein.networth.feature.dashboard.domain.model.Money
import kotlin.math.absoluteValue

/**
 * Locale-independent formatter (`€1,234.56`, `-€80.00`). Pure Kotlin so it works identically on
 * every target and is unit-testable; swap for a platform formatter if localisation requires it.
 */
object MoneyFormatter {
    fun format(money: Money, showFraction: Boolean = true): String =
        format(money.minorUnits, money.currency, showFraction)

    fun format(minorUnits: Long, currency: CurrencyCode, showFraction: Boolean = true): String {
        val factor = pow10(currency.fractionDigits)
        val abs = minorUnits.absoluteValue
        val whole = groupThousands((abs / factor).toString())
        val sign = if (minorUnits < 0) "-" else ""
        if (!showFraction || currency.fractionDigits == 0) return "$sign${currency.symbol}$whole"
        val fraction = (abs % factor).toString().padStart(currency.fractionDigits, '0')
        return "$sign${currency.symbol}$whole.$fraction"
    }

    private fun groupThousands(digits: String): String =
        digits.reversed().chunked(3).joinToString(",").reversed()

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}

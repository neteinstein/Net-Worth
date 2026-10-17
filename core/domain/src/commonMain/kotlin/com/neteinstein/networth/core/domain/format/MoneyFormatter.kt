package com.neteinstein.networth.core.domain.format

import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Money
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

    /** Whole units, as the design shows most figures: `€4,120`. Rounds half away from zero. */
    fun whole(minorUnits: Long, currency: CurrencyCode): String {
        val factor = pow10(currency.fractionDigits)
        val rounded = (minorUnits.absoluteValue + factor / 2) / factor * factor
        return format(if (minorUnits < 0) -rounded else rounded, currency, showFraction = false)
    }

    /** Thousands with one decimal for chart labels: `€5.6k`, `€950`. */
    fun compact(minorUnits: Long, currency: CurrencyCode): String {
        val units = minorUnits.absoluteValue / pow10(currency.fractionDigits)
        val sign = if (minorUnits < 0) "-" else ""
        if (units < 1_000) return "$sign${currency.symbol}$units"
        val tenths = (units + 50) / 100
        return "$sign${currency.symbol}${tenths / 10}.${tenths % 10}k"
    }

    private fun groupThousands(digits: String): String =
        digits.reversed().chunked(3).joinToString(",").reversed()

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}

package com.neteinstein.networth.feature.future.ui

import com.neteinstein.networth.core.domain.model.CurrencyCode
import kotlin.math.absoluteValue

/** Pure formatting helpers shared by the Future and Year ahead screens. */
internal object FutureFormat {
    /**
     * Rounds to the nearest [units] whole currency units, as the artboards show estimates
     * (`€30,250` to tens, "About €570,000" to thousands).
     */
    fun roundTo(minor: Long, units: Long, fractionDigits: Int): Long {
        var step = units
        repeat(fractionDigits) { step *= 10 }
        val rounded = (minor.absoluteValue + step / 2) / step * step
        return if (minor < 0) -rounded else rounded
    }

    /**
     * A round top for a chart axis: the next multiple of 2 × 10ⁿ⁻¹ whole units, where n is the number
     * of digits (87,000 → 100,000; 3,000 → 4,000), as minor units. At least one step.
     */
    fun chartMax(minor: Long, fractionDigits: Int): Long {
        val factor = pow10(fractionDigits)
        val units = (minor / factor).coerceAtLeast(1)
        val step = 2 * pow10(units.toString().length - 1)
        return ((units + step - 1) / step).coerceAtLeast(1) * step * factor
    }

    /** Axis labels in thousands as on the artboard: `€100k`, `€50k`, or `€800` below a thousand. */
    fun thousands(minor: Long, currency: CurrencyCode): String {
        val units = minor / pow10(currency.fractionDigits)
        return if (units >= 1_000) "${currency.symbol}${(units + 500) / 1_000}k" else "${currency.symbol}$units"
    }

    /** An assumption as the copy writes it: 3 → "3", 2.5 → "2.5". */
    fun rate(pct: Double): String = if (pct % 1.0 == 0.0) pct.toLong().toString() else pct.toString()

    /** "Dec", "Dec and Jul", "Dec, Jul and Sep". */
    fun joinWithAnd(items: List<String>, and: (String, String) -> String): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> and(items.dropLast(1).joinToString(", "), items.last())
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }
}

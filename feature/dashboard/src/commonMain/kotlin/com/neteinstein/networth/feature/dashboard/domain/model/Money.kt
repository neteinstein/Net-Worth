package com.neteinstein.networth.feature.dashboard.domain.model

/**
 * An amount of money in the currency's minor units (e.g. cents) to avoid floating-point drift.
 * Arithmetic is only defined between amounts of the same currency.
 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode = CurrencyCode.DEFAULT,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    val isNegative: Boolean get() = minorUnits < 0

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
    }

    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.DEFAULT) = Money(0, currency)
    }
}

/** ISO-4217 code plus the display bits we need without a platform locale API. */
data class CurrencyCode(
    val code: String,
    val symbol: String,
    val fractionDigits: Int = 2,
) {
    companion object {
        val EUR = CurrencyCode("EUR", "€")
        val USD = CurrencyCode("USD", "$")
        val GBP = CurrencyCode("GBP", "£")
        val DEFAULT = EUR
    }
}

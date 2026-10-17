package com.neteinstein.networth.feature.future.domain.model

import com.neteinstein.networth.core.domain.model.CalendarMonth

/** The savings chips on Future: every savings account, the adults' (joint) savings, or the kids'. */
enum class FutureScope { ALL, JOINT, KIDS }

/** The "What if…" switches. [setAside] only smooths spending, so it does not change the totals. */
data class WhatIfs(
    val cutDining: Boolean = false,
    val addExtra: Boolean = false,
    val setAside: Boolean = false,
)

data class KidProjection(
    val memberId: String,
    val name: String,
    val nowMinor: Long,
    /** Index n is the balance after n years, with the return assumption. */
    val withReturn: List<Long>,
)

/** "Set aside €27/month for September": a seasonal category's usual excess, spread over the year. */
data class SetAside(
    val categoryName: String,
    val month: Int,
    val monthlyMinor: Long,
)

data class EmergencyFund(
    val months: Int,
    val targetMinor: Long,
    val currentMinor: Long,
    /** When joint savings reach the target at this pace; null when already there or never at this pace. */
    val reachedOn: CalendarMonth?,
) {
    val covered: Boolean get() = currentMinor >= targetMinor

    val fraction: Float get() = if (targetMinor <= 0) 1f else (currentMinor.toFloat() / targetMinor).coerceIn(0f, 1f)
}

data class Future(
    val currencyCode: String,
    /** The latest month with data, "now" for every projection. Null without history. */
    val latest: CalendarMonth?,
    val scope: FutureScope,
    val scopes: List<FutureScope>,
    /** Saved per month on average in this scope, what-ifs included. */
    val monthlyMinor: Long,
    /** Index n is the balance after n years (0 = today), saving [monthlyMinor] with no return. */
    val flat: List<Long>,
    /** Same, earning [returnRatePct] a year. */
    val withReturn: List<Long>,
    val returnRatePct: Double,
    val kids: List<KidProjection>,
    /** 20 % of the dining & leisure average, or null without that category or history. */
    val diningCutMinor: Long?,
    val extraMinor: Long,
    val setAside: SetAside?,
    val whatIfs: WhatIfs,
    val emergencyFund: EmergencyFund?,
    val tenYearSpendMinor: Long,
    val tenYearSpendWithPricesMinor: Long,
    val priceRisePct: Double,
) {
    companion object {
        const val YEARS = 10
    }
}

package com.neteinstein.networth.core.domain.calc

import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import kotlin.math.roundToLong

/**
 * Year-by-year savings: each year the balance earns [annualRatePct] and gains twelve months of
 * [monthlySavingsMinor] — the model on the "Future" artboard. Index 0 is today, index n is year n.
 */
fun projectSavings(startMinor: Long, monthlySavingsMinor: Long, years: Int, annualRatePct: Double = 0.0): List<Long> {
    val rate = annualRatePct / 100
    var value = startMinor.toDouble()
    return buildList {
        add(startMinor)
        repeat(years) {
            value = value * (1 + rate) + 12 * monthlySavingsMinor
            add(value.roundToLong())
        }
    }
}

/** Average saved per month (income minus spending) over the months with data before [month]. */
fun FamilyDatabase.averageMonthlySavings(month: CalendarMonth, scope: AccountScope = AccountScope.All): Long =
    trailingAverage(scope, month.next()).savedMinor

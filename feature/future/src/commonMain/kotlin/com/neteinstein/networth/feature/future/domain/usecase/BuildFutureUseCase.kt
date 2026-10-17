package com.neteinstein.networth.feature.future.domain.usecase

import com.neteinstein.networth.core.domain.calc.AccountScope
import com.neteinstein.networth.core.domain.calc.averageMonthlySavings
import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.calc.coveredMonths
import com.neteinstein.networth.core.domain.calc.isChildAccount
import com.neteinstein.networth.core.domain.calc.isSavings
import com.neteinstein.networth.core.domain.calc.latestMonth
import com.neteinstein.networth.core.domain.calc.projectSavings
import com.neteinstein.networth.core.domain.calc.savingsBalance
import com.neteinstein.networth.core.domain.calc.totals
import com.neteinstein.networth.core.domain.calc.trailingAverage
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.future.domain.model.EmergencyFund
import com.neteinstein.networth.feature.future.domain.model.Future
import com.neteinstein.networth.feature.future.domain.model.FutureScope
import com.neteinstein.networth.feature.future.domain.model.KidProjection
import com.neteinstein.networth.feature.future.domain.model.SetAside
import com.neteinstein.networth.feature.future.domain.model.WhatIfs
import com.neteinstein.networth.feature.future.domain.model.unitsToMinor
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The "Future" artboard: if the coming years look like the last 12 months, where do savings go?
 *
 * - All savings grow by what the family saves a month (income minus spending, as on Overview).
 * - Joint and kids' savings grow by the money that actually reached those accounts each month.
 * - Each is shown with no return and with the household's return assumption.
 */
class BuildFutureUseCase {
    operator fun invoke(database: FamilyDatabase, scope: FutureScope = FutureScope.ALL, whatIfs: WhatIfs = WhatIfs()): Future {
        val assumptions = database.settings.assumptions
        val fraction = database.currency.fractionDigits
        val joint = jointIds(database)
        val kids = kidIds(database)
        val scopes = buildList {
            add(FutureScope.ALL)
            if (joint.isNotEmpty()) add(FutureScope.JOINT)
            if (kids.isNotEmpty()) add(FutureScope.KIDS)
        }
        val effective = scope.takeIf { it in scopes } ?: FutureScope.ALL
        val latest = database.latestMonth()
        val extraMinor = unitsToMinor(EXTRA_TO_JOINT_UNITS, fraction)
        val averageSpend = latest?.let { database.trailingAverage(AccountScope.All, it.next()).spentMinor } ?: 0L
        val diningCut = latest?.let { diningCut(database, it) }
        val extras = (if (whatIfs.cutDining) diningCut ?: 0L else 0L) + (if (whatIfs.addExtra) extraMinor else 0L)

        val jointMonthly = latest?.let { monthlyInflow(database, joint, it) } ?: 0L
        val (start, monthly) = when (effective) {
            FutureScope.ALL -> database.savingsBalance() to (latest?.let { database.averageMonthlySavings(it) } ?: 0L) + extras
            FutureScope.JOINT -> balance(database, joint) to jointMonthly + extras
            FutureScope.KIDS -> balance(database, kids) to (latest?.let { monthlyInflow(database, kids, it) } ?: 0L)
        }
        val rate = assumptions.returnRatePct
        return Future(
            currencyCode = database.settings.currencyCode,
            latest = latest,
            scope = effective,
            scopes = scopes,
            monthlyMinor = monthly,
            flat = projectSavings(start, monthly, Future.YEARS),
            withReturn = projectSavings(start, monthly, Future.YEARS, rate),
            returnRatePct = rate,
            kids = if (effective == FutureScope.KIDS && latest != null) kidProjections(database, latest, rate) else emptyList(),
            diningCutMinor = diningCut,
            extraMinor = extraMinor,
            setAside = latest?.let { setAside(database, it) },
            whatIfs = whatIfs,
            emergencyFund = latest?.takeIf { joint.isNotEmpty() }?.let {
                emergencyFund(database, it, joint, averageSpend, jointMonthly + extras)
            },
            tenYearSpendMinor = averageSpend * 12 * Future.YEARS,
            tenYearSpendWithPricesMinor = (0 until Future.YEARS).sumOf { k ->
                (averageSpend * 12 * (1 + assumptions.priceRisePct / 100).pow(k)).roundToLong()
            },
            priceRisePct = assumptions.priceRisePct,
        )
    }

    /** Savings accounts not owned only by children: the joint (family) savings. */
    private fun jointIds(database: FamilyDatabase): Set<String> =
        database.accounts.filter { it.type.isSavings && !database.isChildAccount(it) }.map { it.id }.toSet()

    private fun kidIds(database: FamilyDatabase): Set<String> =
        database.accounts.filter { !it.type.isLiability && database.isChildAccount(it) }.map { it.id }.toSet()

    private fun balance(database: FamilyDatabase, ids: Set<String>): Long =
        database.accounts.filter { it.id in ids }.sumOf { database.balanceOf(it) }

    /**
     * Net money into [ids] per month (transfers included: that is how savings grow), averaged over
     * the up-to-12 months ending at [latest] that have any data.
     */
    private fun monthlyInflow(database: FamilyDatabase, ids: Set<String>, latest: CalendarMonth): Long {
        val covered = database.coveredMonths()
        val months = (0 until 12).map { latest.plus(-it) }.filter { it in covered }
        if (months.isEmpty() || ids.isEmpty()) return 0L
        val total = database.transactions
            .filter { it.accountId in ids && months.any { month -> it.date in month } }
            .sumOf { it.amountMinor }
        return total / months.size
    }

    private fun kidProjections(database: FamilyDatabase, latest: CalendarMonth, rate: Double): List<KidProjection> =
        database.members.filter { it.role == MemberRole.CHILD }.mapNotNull { child ->
            val ids = database.accounts
                .filter { child.id in it.ownerIds && database.isChildAccount(it) && !it.type.isLiability }
                .map { it.id }
                .toSet()
            if (ids.isEmpty()) return@mapNotNull null
            val now = balance(database, ids)
            KidProjection(child.id, child.name, now, projectSavings(now, monthlyInflow(database, ids, latest), Future.YEARS, rate))
        }

    /** 20 % of the dining & leisure category's 12-month average. */
    private fun diningCut(database: FamilyDatabase, latest: CalendarMonth): Long? {
        val dining = database.categories.firstOrNull { it.name.equals(DINING_CATEGORY, ignoreCase = true) } ?: return null
        val average = database.trailingAverage(AccountScope.All, latest.next()).spentByCategory[dining.id] ?: return null
        return (average * DINING_CUT_PCT / 100).takeIf { it > 0 }
    }

    /**
     * The seasonal category whose peak month last ran furthest above its average, spread over twelve
     * months. Seasonal means a peak month set on the category or a "Mark as seasonal" for a month.
     */
    private fun setAside(database: FamilyDatabase, latest: CalendarMonth): SetAside? {
        val covered = database.coveredMonths()
        val marked = database.settings.alerts.seasonalMarks.map { it.categoryId to it.month }
        val peaks = database.categories.mapNotNull { c -> c.seasonalPeakMonth?.let { c.id to it } } + marked
        return peaks.distinct().mapNotNull { (categoryId, month) ->
            // The most recent occurrence of that month, up to the latest one with data.
            val occurrence = (0 until 12).map { latest.plus(-it) }.firstOrNull { it.month == month } ?: return@mapNotNull null
            if (occurrence !in covered) return@mapNotNull null
            val spent = database.totals(AccountScope.All, occurrence).spentByCategory[categoryId] ?: 0L
            val average = database.trailingAverage(AccountScope.All, occurrence).spentByCategory[categoryId] ?: 0L
            val excess = spent - average
            val category = database.category(categoryId) ?: return@mapNotNull null
            if (excess <= 0) null else SetAside(category.name, month, excess / 12)
        }.maxByOrNull { it.monthlyMinor }
    }

    private fun emergencyFund(
        database: FamilyDatabase,
        latest: CalendarMonth,
        joint: Set<String>,
        averageSpend: Long,
        monthly: Long,
    ): EmergencyFund {
        val months = database.settings.assumptions.emergencyFundMonths
        val target = averageSpend * months
        val current = balance(database, joint)
        val reached = when {
            current >= target || monthly <= 0 -> null
            else -> latest.plus(((target - current + monthly - 1) / monthly).toInt())
        }
        return EmergencyFund(months, target, current, reached)
    }

    companion object {
        /** The seeded category the "Dining & leisure −20%" what-if is about. */
        const val DINING_CATEGORY = "Dining & leisure"
        const val DINING_CUT_PCT = 20
        const val EXTRA_TO_JOINT_UNITS = 100L
    }
}

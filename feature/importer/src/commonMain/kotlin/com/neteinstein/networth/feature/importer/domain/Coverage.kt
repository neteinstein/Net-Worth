package com.neteinstein.networth.feature.importer.domain

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.feature.importer.domain.model.AccountCoverage
import com.neteinstein.networth.feature.importer.domain.model.MonthCoverage

/**
 * Which months each account has statements for — so the family can see at a glance which
 * extracts are still missing.
 *
 * A month is [MonthCoverage.FULL] when imported statement periods cover every day of it,
 * [MonthCoverage.PARTIAL] when they cover some days or the account has movements in it anyway, and
 * [MonthCoverage.MISSING] otherwise. Mortgages, loans and funds are left out: their statements
 * carry a balance, not movements.
 */
object Coverage {
    fun of(
        database: FamilyDatabase,
        last: CalendarMonth,
        count: Int = 12,
    ): List<AccountCoverage> {
        val months = (count - 1 downTo 0).map { last.plus(-it) }
        return database.accounts.filter { it.tracksMovements }.map { account ->
            val periods = database.imports.filter { it.accountId == account.id }.map {
                it.periodStart.toEpochDays() to
                    it.periodEnd.toEpochDays()
            }
            val activeMonths = database.transactions.filter { it.accountId == account.id }.map { CalendarMonth.of(it.date) }.toSet()
            AccountCoverage(
                account,
                months.map { month ->
                    val first = month.firstDay.toEpochDays()
                    val lastDay = month.lastDay.toEpochDays()
                    val covered = (first..lastDay).count { day -> periods.any { (start, end) -> day in start..end } }
                    month to
                        when {
                            covered == (lastDay - first + 1).toInt() -> MonthCoverage.FULL
                            covered > 0 || month in activeMonths -> MonthCoverage.PARTIAL
                            else -> MonthCoverage.MISSING
                        }
                },
            )
        }
    }

    private val Account.tracksMovements: Boolean
        get() = type == AccountType.CURRENT || type == AccountType.SAVINGS || type == AccountType.TERM_DEPOSIT
}

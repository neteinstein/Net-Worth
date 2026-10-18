package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.runtime.Composable
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_months_long
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_months_short
import org.jetbrains.compose.resources.stringArrayResource

/** Localised month name, [month] in 1..12 ("September"). */
@Composable
fun monthName(month: Int): String = monthNames()[month - 1]

/** Localised short month name ("Sep"). */
@Composable
fun shortMonthName(month: Int): String = shortMonthNames()[month - 1]

/** "Sep 2026". */
@Composable
fun shortMonthYear(month: Int, year: Int): String = "${shortMonthName(month)} $year"

/** All 12 localised month names, January first. */
@Composable
fun monthNames(): List<String> = stringArrayResource(Res.array.designsystem_months_long).orBlankMonths()

/** All 12 localised short month names, "Jan" first. */
@Composable
fun shortMonthNames(): List<String> = stringArrayResource(Res.array.designsystem_months_short).orBlankMonths()

/**
 * On the web, resources load asynchronously and `stringArrayResource` is an empty list until they
 * arrive. Callers index by month, so stand in 12 blank names for that first frame instead of
 * crashing the whole composition.
 */
internal fun List<String>.orBlankMonths(): List<String> = if (size == MONTHS) this else List(MONTHS) { "" }

private const val MONTHS = 12

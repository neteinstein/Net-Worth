package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.runtime.Composable
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_months_long
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_months_short
import org.jetbrains.compose.resources.stringArrayResource

/** Localised month name, [month] in 1..12 ("September"). */
@Composable
fun monthName(month: Int): String = stringArrayResource(Res.array.designsystem_months_long)[month - 1]

/** Localised short month name ("Sep"). */
@Composable
fun shortMonthName(month: Int): String = stringArrayResource(Res.array.designsystem_months_short)[month - 1]

/** "Sep 2026". */
@Composable
fun shortMonthYear(month: Int, year: Int): String = "${shortMonthName(month)} $year"

/** All 12 localised month names, January first. */
@Composable
fun monthNames(): List<String> = stringArrayResource(Res.array.designsystem_months_long)

/** All 12 localised short month names, "Jan" first. */
@Composable
fun shortMonthNames(): List<String> = stringArrayResource(Res.array.designsystem_months_short)

package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.runtime.Composable
import com.neteinstein.networth.core.designsystem.ui.components.monthNames
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.generated.resources.Res
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_current
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_investment
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_loan
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_mortgage
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_savings
import com.neteinstein.networth.feature.importer.generated.resources.importer_type_term_deposit
import com.neteinstein.networth.feature.importer.ui.Periods
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/** "22–28 Sep 2026", "August 2026". */
@Composable
internal fun periodLabel(
    start: LocalDate,
    end: LocalDate,
): String = Periods.label(start, end, shortMonthNames(), monthNames())

internal val AccountType.label: StringResource
    get() =
        when (this) {
            AccountType.CURRENT -> Res.string.importer_type_current
            AccountType.SAVINGS -> Res.string.importer_type_savings
            AccountType.TERM_DEPOSIT -> Res.string.importer_type_term_deposit
            AccountType.INVESTMENT -> Res.string.importer_type_investment
            AccountType.MORTGAGE -> Res.string.importer_type_mortgage
            AccountType.LOAN -> Res.string.importer_type_loan
        }

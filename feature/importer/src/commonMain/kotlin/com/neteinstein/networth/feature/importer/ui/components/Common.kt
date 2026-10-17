package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.components.monthNames
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
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

@Composable
internal fun ScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(NetWorthIcons.ChevronLeft, contentDescription = backLabel, modifier = Modifier.size(20.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        actions()
    }
}

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

package com.neteinstein.networth.feature.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.feature.dashboard.domain.model.Account
import com.neteinstein.networth.feature.dashboard.domain.model.AccountCategory
import com.neteinstein.networth.feature.dashboard.domain.model.AccountKind
import com.neteinstein.networth.feature.dashboard.generated.resources.Res
import com.neteinstein.networth.feature.dashboard.generated.resources.category_cash
import com.neteinstein.networth.feature.dashboard.generated.resources.category_credit_card
import com.neteinstein.networth.feature.dashboard.generated.resources.category_investments
import com.neteinstein.networth.feature.dashboard.generated.resources.category_loan
import com.neteinstein.networth.feature.dashboard.generated.resources.category_mortgage
import com.neteinstein.networth.feature.dashboard.generated.resources.category_other_asset
import com.neteinstein.networth.feature.dashboard.generated.resources.category_other_liability
import com.neteinstein.networth.feature.dashboard.generated.resources.category_property
import com.neteinstein.networth.feature.dashboard.ui.MoneyFormatter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountRow(
    account: Account,
    modifier: Modifier = Modifier,
) {
    val finance = NetWorthTheme.financeColors
    val isAsset = account.kind == AccountKind.ASSET
    val accent = if (isAsset) finance.positive else finance.negative
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(12.dp).background(accent, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(account.category.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormatter.format(if (isAsset) account.balance else -account.balance),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
    }
}

private val AccountCategory.label: StringResource
    get() = when (this) {
        AccountCategory.CASH -> Res.string.category_cash
        AccountCategory.INVESTMENTS -> Res.string.category_investments
        AccountCategory.PROPERTY -> Res.string.category_property
        AccountCategory.OTHER_ASSET -> Res.string.category_other_asset
        AccountCategory.MORTGAGE -> Res.string.category_mortgage
        AccountCategory.LOAN -> Res.string.category_loan
        AccountCategory.CREDIT_CARD -> Res.string.category_credit_card
        AccountCategory.OTHER_LIABILITY -> Res.string.category_other_liability
    }

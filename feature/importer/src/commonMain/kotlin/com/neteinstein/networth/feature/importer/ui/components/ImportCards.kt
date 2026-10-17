package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neteinstein.networth.core.designsystem.ui.components.CardTitle
import com.neteinstein.networth.core.designsystem.ui.components.ChipRow
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.OwnerPicker
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.importer.domain.model.AccountCoverage
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.MonthCoverage
import com.neteinstein.networth.feature.importer.generated.resources.Res
import com.neteinstein.networth.feature.importer.generated.resources.importer_browse
import com.neteinstein.networth.feature.importer.generated.resources.importer_categorised
import com.neteinstein.networth.feature.importer.generated.resources.importer_categorised_value
import com.neteinstein.networth.feature.importer.generated.resources.importer_choose
import com.neteinstein.networth.feature.importer.generated.resources.importer_duplicates
import com.neteinstein.networth.feature.importer.generated.resources.importer_earlier
import com.neteinstein.networth.feature.importer.generated.resources.importer_error_dismiss
import com.neteinstein.networth.feature.importer.generated.resources.importer_formats
import com.neteinstein.networth.feature.importer.generated.resources.importer_import_another
import com.neteinstein.networth.feature.importer.generated.resources.importer_include_section
import com.neteinstein.networth.feature.importer.generated.resources.importer_month_full
import com.neteinstein.networth.feature.importer.generated.resources.importer_month_missing
import com.neteinstein.networth.feature.importer.generated.resources.importer_month_partial
import com.neteinstein.networth.feature.importer.generated.resources.importer_months_body
import com.neteinstein.networth.feature.importer.generated.resources.importer_months_complete
import com.neteinstein.networth.feature.importer.generated.resources.importer_months_missing
import com.neteinstein.networth.feature.importer.generated.resources.importer_months_title
import com.neteinstein.networth.feature.importer.generated.resources.importer_need_you
import com.neteinstein.networth.feature.importer.generated.resources.importer_new_account
import com.neteinstein.networth.feature.importer.generated.resources.importer_new_account_name
import com.neteinstein.networth.feature.importer.generated.resources.importer_overlap_note
import com.neteinstein.networth.feature.importer.generated.resources.importer_period_monthly
import com.neteinstein.networth.feature.importer.generated.resources.importer_period_other
import com.neteinstein.networth.feature.importer.generated.resources.importer_period_weekly
import com.neteinstein.networth.feature.importer.generated.resources.importer_preselected
import com.neteinstein.networth.feature.importer.generated.resources.importer_review_count
import com.neteinstein.networth.feature.importer.generated.resources.importer_rows_read
import com.neteinstein.networth.feature.importer.generated.resources.importer_section_rows
import com.neteinstein.networth.feature.importer.generated.resources.importer_transactions
import com.neteinstein.networth.feature.importer.generated.resources.importer_transfers_note
import com.neteinstein.networth.feature.importer.generated.resources.importer_unreconciled
import com.neteinstein.networth.feature.importer.generated.resources.importer_which_account
import com.neteinstein.networth.feature.importer.generated.resources.importer_whose
import com.neteinstein.networth.feature.importer.generated.resources.importer_whose_hint
import com.neteinstein.networth.feature.importer.ui.Periods
import com.neteinstein.networth.feature.importer.ui.screens.ImportHistoryLine
import com.neteinstein.networth.feature.importer.ui.screens.SectionUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The dashed drop zone of the artboard: "Choose a bank extract · CSV · XLSX · OFX · PDF · Browse files". */
@Composable
internal fun PickFileCard(
    busyLabel: String?,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(vertical = 26.dp, horizontal = 18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (busyLabel != null) {
                CircularProgressIndicator(Modifier.size(32.dp))
                Text(busyLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Icon(
                NetWorthIcons.Download,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(Res.string.importer_choose), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.importer_formats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Button(onClick = onBrowse, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(Res.string.importer_browse)) }
        }
    }
}

@Composable
internal fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finance = NetWorthTheme.financeColors
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = finance.warningContainer,
        contentColor = finance.onWarningContainer,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(stringResource(Res.string.importer_error_dismiss), color = finance.onWarningContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * One account in the file: which family account it goes to (pre-selected by IBAN/number), or a
 * new one, and who owns it. Files with several accounts get one card each.
 */
@Composable
internal fun SectionCardContent(
    section: SectionUi,
    multiple: Boolean,
    accounts: List<Account>,
    members: List<Member>,
    onAccount: (String?) -> Unit,
    onNewName: (String) -> Unit,
    onNewType: (AccountType) -> Unit,
    onInclude: (Boolean) -> Unit,
    onOwnerToggled: (String) -> Unit,
    onAddPerson: (String, MemberRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier, spacing = 10) {
        if (multiple) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(section.label, section.accountNumber).joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SectionMeta(section)
                }
                val description = stringResource(Res.string.importer_include_section)
                Switch(
                    checked = section.choice.include,
                    onCheckedChange = onInclude,
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
            if (!section.choice.include) return@SectionCard
        }
        CardTitle(stringResource(Res.string.importer_which_account))
        if (!multiple) SectionMeta(section)
        if (section.matchedAccountId != null) {
            Text(
                stringResource(Res.string.importer_preselected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val selectable = accounts.filter {
            it.type == AccountType.CURRENT ||
                it.type == AccountType.SAVINGS ||
                it.type == AccountType.TERM_DEPOSIT
        }
        selectable.forEach { account ->
            AccountOption(
                title = account.name,
                subtitle = stringResource(account.type.label),
                selected = section.choice.accountId == account.id,
                onClick = { onAccount(account.id) },
            )
        }
        AccountOption(
            title = stringResource(Res.string.importer_new_account),
            subtitle = stringResource(section.choice.newType.label),
            selected = section.choice.accountId == null,
            onClick = { onAccount(null) },
        )
        if (section.choice.accountId == null) {
            OutlinedTextField(
                value = section.choice.newName,
                onValueChange = onNewName,
                label = { Text(stringResource(Res.string.importer_new_account_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ChipRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                listOf(AccountType.CURRENT, AccountType.SAVINGS, AccountType.TERM_DEPOSIT).forEach { type ->
                    ChoiceChip(stringResource(type.label), section.choice.newType == type, { onNewType(type) })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OverlineLabel(stringResource(Res.string.importer_whose))
        Text(
            stringResource(Res.string.importer_whose_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OwnerPicker(members, section.choice.ownerIds, onOwnerToggled, onAddPerson)
    }
}

@Composable
private fun SectionMeta(section: SectionUi) {
    val start = section.periodStart
    val end = section.periodEnd
    val period = if (start != null && end != null) periodLabel(start, end) else ""
    Text(
        stringResource(
            Res.string.importer_section_rows,
            period,
            pluralStringResource(Res.plurals.importer_rows_read, section.rows, section.rows),
        ).trim(' ', '·'),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(
            min = 52.dp,
        ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The result card of the artboard: file, account and period, how much was filed, what needs the family. */
@Composable
internal fun SummaryCard(
    summary: ImportSummary,
    overlaps: List<Pair<LocalDate, LocalDate>>,
    pendingReview: Int,
    onReview: () -> Unit,
    onImportAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier, spacing = 12) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(summary.fileName, style = MaterialTheme.typography.titleMedium, fontFamily = NetWorthTheme.monoFamily)
            val start = summary.periodStart
            val end = summary.periodEnd
            val period = if (start != null && end != null) periodLabel(start, end) else null
            Text(
                (summary.accountNames + listOfNotNull(period)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val total = summary.categorisable
        if (total > 0) {
            val percent = summary.autoCategorised * 100 / total
            Row {
                Text(
                    stringResource(Res.string.importer_categorised),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.importer_categorised_value, summary.autoCategorised, total, percent),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LinearProgressIndicator(
                progress = { summary.autoCategorised.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat(pluralStringResource(Res.plurals.importer_rows_read, summary.rowsRead, summary.rowsRead))
            Stat(pluralStringResource(Res.plurals.importer_duplicates, summary.duplicatesSkipped, summary.duplicatesSkipped))
            Stat(stringResource(Res.string.importer_need_you, summary.needReview), highlight = summary.needReview > 0)
        }
        val notes =
            buildList {
                val overlap = overlaps.firstOrNull()
                if (summary.duplicatesSkipped > 0 && overlap != null) {
                    add(
                        pluralStringResource(
                            Res.plurals.importer_overlap_note,
                            summary.duplicatesSkipped,
                            summary.duplicatesSkipped,
                            periodLabel(overlap.first, overlap.second),
                        ),
                    )
                }
                if (summary.internalTransfers > 0) {
                    add(pluralStringResource(Res.plurals.importer_transfers_note, summary.internalTransfers, summary.internalTransfers))
                }
                summary.unreconciledAccounts.forEach { add(stringResource(Res.string.importer_unreconciled, it)) }
            }
        if (notes.isNotEmpty()) {
            Text(notes.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (pendingReview > 0) {
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(pluralStringResource(Res.plurals.importer_review_count, pendingReview, pendingReview))
            }
        }
        OutlinedButton(onClick = onImportAnother, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
            Text(stringResource(Res.string.importer_import_another))
        }
    }
}

@Composable
private fun Stat(
    text: String,
    highlight: Boolean = false,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlight) NetWorthTheme.financeColors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
    )
}

/** Per account, the last 12 months: uploaded, partly uploaded or missing. */
@Composable
internal fun CoverageCard(
    coverage: List<AccountCoverage>,
    members: List<Member>,
    modifier: Modifier = Modifier,
) {
    val finance = NetWorthTheme.financeColors
    val full = finance.positive
    val partial = finance.warningOutline
    val missing = MaterialTheme.colorScheme.outlineVariant
    val months = shortMonthNames()
    val labels =
        mapOf(
            MonthCoverage.FULL to stringResource(Res.string.importer_month_full),
            MonthCoverage.PARTIAL to stringResource(Res.string.importer_month_partial),
            MonthCoverage.MISSING to stringResource(Res.string.importer_month_missing),
        )
    SectionCard(modifier, spacing = 14) {
        CardTitle(stringResource(Res.string.importer_months_title))
        Text(
            stringResource(Res.string.importer_months_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        coverage.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val owners = row.account.ownerIds.mapNotNull { id -> members.firstOrNull { it.id == id }?.name }.joinToString(" & ")
                    Text(
                        listOf(row.account.name, owners).filter { it.isNotEmpty() }.distinct().joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (row.missingCount == 0) {
                            stringResource(Res.string.importer_months_complete)
                        } else {
                            pluralStringResource(Res.plurals.importer_months_missing, row.missingCount, row.missingCount)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.missingCount == 0) full else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.months.forEach { (month, state) ->
                        val name = months[month.month - 1]
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (state) {
                                            MonthCoverage.FULL -> full
                                            MonthCoverage.PARTIAL -> partial
                                            MonthCoverage.MISSING -> missing
                                        },
                                    ).semantics {
                                        contentDescription = "$name ${month.year}: ${labels.getValue(state)}"
                                    },
                            )
                            Text(
                                name.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                MonthCoverage.FULL to full,
                MonthCoverage.PARTIAL to partial,
                MonthCoverage.MISSING to missing,
            ).forEach { (state, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Text(
                        labels.getValue(state),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HistoryCard(
    history: List<ImportHistoryLine>,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier, spacing = 10) {
        CardTitle(stringResource(Res.string.importer_earlier))
        history.forEach { line ->
            val record = line.record
            val kind =
                when (Periods.kind(record.periodStart, record.periodEnd)) {
                    Periods.Kind.WEEKLY -> Res.string.importer_period_weekly
                    Periods.Kind.MONTHLY -> Res.string.importer_period_monthly
                    Periods.Kind.OTHER -> Res.string.importer_period_other
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(periodLabel(record.periodStart, record.periodEnd), style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOf(stringResource(kind), line.accountName, line.owners).filter {
                            it.isNotEmpty()
                        }.distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val count = record.rowsRead - record.duplicatesSkipped
                Text(pluralStringResource(Res.plurals.importer_transactions, count, count), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

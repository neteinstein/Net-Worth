package com.neteinstein.networth.feature.spending.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.ChoiceChip
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.EmptyState
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.categoryName
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.categoryColor
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.feature.spending.domain.model.UNCATEGORISED
import com.neteinstein.networth.feature.spending.domain.usecase.TransactionEdit
import com.neteinstein.networth.feature.spending.generated.resources.Res
import com.neteinstein.networth.feature.spending.generated.resources.filters_back
import com.neteinstein.networth.feature.spending.generated.resources.spending_uncategorised
import com.neteinstein.networth.feature.spending.generated.resources.transactions_cancel
import com.neteinstein.networth.feature.spending.generated.resources.transactions_category
import com.neteinstein.networth.feature.spending.generated.resources.transactions_edit
import com.neteinstein.networth.feature.spending.generated.resources.transactions_empty_body
import com.neteinstein.networth.feature.spending.generated.resources.transactions_empty_title
import com.neteinstein.networth.feature.spending.generated.resources.transactions_internal
import com.neteinstein.networth.feature.spending.generated.resources.transactions_internal_label
import com.neteinstein.networth.feature.spending.generated.resources.transactions_none
import com.neteinstein.networth.feature.spending.generated.resources.transactions_note
import com.neteinstein.networth.feature.spending.generated.resources.transactions_save
import com.neteinstein.networth.feature.spending.generated.resources.transactions_subcategory
import com.neteinstein.networth.feature.spending.generated.resources.transactions_summary
import com.neteinstein.networth.feature.spending.generated.resources.transactions_tagged_for
import com.neteinstein.networth.feature.spending.generated.resources.transactions_title
import kotlinx.datetime.number
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionsScreen(
    request: TransactionsRequest,
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = koinViewModel(),
) {
    LaunchedEffect(request) { viewModel.open(request) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionsContent(
        state = state,
        onBack = onBack,
        onEdit = viewModel::onEdit,
        onEditorChange = viewModel::onEditorChange,
        onSave = viewModel::onSaveEdit,
        onDismiss = viewModel::onDismissEditor,
    )
}

@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onEditorChange: ((TransactionEdit) -> TransactionEdit) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        if (!state.ready) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(state.currencyCode)
        val months = shortMonthNames()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val cards = Modifier.padding(horizontal = 20.dp)
                item {
                    Row(cards.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                            Icon(
                                NetWorthIcons.ChevronLeft,
                                contentDescription = stringResource(Res.string.filters_back),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            val title =
                                when {
                                    state.category != null -> categoryName(state.category.name)
                                    state.request.categoryId == UNCATEGORISED -> stringResource(Res.string.spending_uncategorised)
                                    else -> stringResource(Res.string.transactions_title)
                                }
                            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                            Text(
                                pluralStringResource(
                                    Res.plurals.transactions_summary,
                                    state.lines.size,
                                    state.lines.size,
                                    MoneyFormatter.format(state.totalMinor, currency),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.lines.isEmpty()) {
                    item {
                        EmptyState(
                            stringResource(Res.string.transactions_empty_title),
                            stringResource(Res.string.transactions_empty_body),
                            cards,
                        )
                    }
                } else {
                    item {
                        SectionCard(cards, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), spacing = 0) {
                            state.lines.forEachIndexed { index, line ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                LineRow(line, currency, "${line.transaction.date.day} ${months[line.transaction.date.month.number - 1]}") {
                                    onEdit(line.transaction.id)
                                }
                            }
                        }
                    }
                }
            }
        }
        state.editor?.let { editor -> EditDialog(editor, state.categories, state.members, currency, onEditorChange, onSave, onDismiss) }
    }
}

@Composable
private fun LineRow(
    line: TransactionLine,
    currency: CurrencyCode,
    date: String,
    onClick: () -> Unit,
) {
    val tx = line.transaction
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorKey(line.category?.colorHex?.let { categoryColor(it) } ?: NetWorthTheme.financeColors.chartMuted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                tx.description.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = NetWorthTheme.monoFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val label =
                when {
                    tx.isInternalTransfer -> stringResource(Res.string.transactions_internal_label)
                    line.category != null -> categoryName(line.category.name)
                    else -> stringResource(Res.string.spending_uncategorised)
                }
            Text(
                listOfNotNull(date, line.accountName.takeIf { it.isNotEmpty() }, label, tx.note).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            MoneyFormatter.format(tx.amountMinor, currency),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (tx.amountMinor > 0) NetWorthTheme.financeColors.positive else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditDialog(
    editor: TransactionEditor,
    categories: List<Category>,
    members: List<Member>,
    currency: CurrencyCode,
    onChange: ((TransactionEdit) -> TransactionEdit) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val edit = editor.edit
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.transactions_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${editor.description.uppercase()} · ${MoneyFormatter.format(editor.amountMinor, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = NetWorthTheme.monoFamily,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.transactions_internal),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = edit.isInternalTransfer, onCheckedChange = { value ->
                        onChange { it.copy(isInternalTransfer = value) }
                    })
                }
                if (!edit.isInternalTransfer) {
                    OverlineLabel(stringResource(Res.string.transactions_category))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip(stringResource(Res.string.transactions_none), edit.categoryId == null, {
                            onChange { it.copy(categoryId = null, subcategoryId = null) }
                        })
                        categories.forEach { category ->
                            ChoiceChip(categoryName(category.name), edit.categoryId == category.id, {
                                onChange {
                                    it.copy(
                                        categoryId = if (it.categoryId ==
                                            category.id
                                        ) {
                                            null
                                        } else {
                                            category.id
                                        },
                                        subcategoryId = null,
                                    )
                                }
                            })
                        }
                    }
                    val subcategories = categories.firstOrNull { it.id == edit.categoryId }?.subcategories.orEmpty()
                    if (subcategories.isNotEmpty()) {
                        OverlineLabel(stringResource(Res.string.transactions_subcategory))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            subcategories.forEach { sub ->
                                ChoiceChip(categoryName(sub.name), edit.subcategoryId == sub.id, {
                                    onChange { it.copy(subcategoryId = if (it.subcategoryId == sub.id) null else sub.id) }
                                })
                            }
                        }
                    }
                }
                if (members.isNotEmpty()) {
                    OverlineLabel(stringResource(Res.string.transactions_tagged_for))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            val on = member.id in edit.memberIds
                            ChoiceChip(member.name, on, {
                                onChange {
                                    it.copy(
                                        memberIds = if (on) {
                                            it.memberIds - member.id
                                        } else {
                                            it.memberIds +
                                                member.id
                                        },
                                    )
                                }
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = edit.note.orEmpty(),
                    onValueChange = { value -> onChange { it.copy(note = value) } },
                    label = { Text(stringResource(Res.string.transactions_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(Res.string.transactions_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.transactions_cancel)) } },
    )
}

@Preview
@Composable
private fun TransactionsPreview() {
    NetWorthTheme {
        TransactionsContent(
            TransactionsUiState(ready = true, category = previewCategories().first(), lines = previewTransactions(), totalMinor = -430_00),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun TransactionEditPreview() {
    NetWorthTheme(darkTheme = true) {
        TransactionsContent(
            TransactionsUiState(
                ready = true,
                lines = previewTransactions(),
                categories = previewCategories(),
                members = previewMembers(),
                editor = TransactionEditor("1", "LIVRARIA ESCOLAR", -385_00, TransactionEdit("s", null, listOf("i"), null, false)),
            ),
            {},
            {},
            {},
            {},
            {},
        )
    }
}

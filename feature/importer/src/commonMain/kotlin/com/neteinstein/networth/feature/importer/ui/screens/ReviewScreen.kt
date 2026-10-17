package com.neteinstein.networth.feature.importer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.EmptyState
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.SectionCard
import com.neteinstein.networth.core.designsystem.ui.components.shortMonthNames
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.feature.importer.domain.model.ReviewItem
import com.neteinstein.networth.feature.importer.domain.parse.Descriptions
import com.neteinstein.networth.feature.importer.domain.parse.MovementKind
import com.neteinstein.networth.feature.importer.generated.resources.Res
import com.neteinstein.networth.feature.importer.generated.resources.importer_back
import com.neteinstein.networth.feature.importer.generated.resources.review_accept_all
import com.neteinstein.networth.feature.importer.generated.resources.review_done
import com.neteinstein.networth.feature.importer.generated.resources.review_empty_body
import com.neteinstein.networth.feature.importer.generated.resources.review_empty_title
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_card
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_cash
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_debit
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_fee
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_history
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_low_value
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_phone
import com.neteinstein.networth.feature.importer.generated.resources.review_hint_transfer
import com.neteinstein.networth.feature.importer.generated.resources.review_intro
import com.neteinstein.networth.feature.importer.generated.resources.review_more
import com.neteinstein.networth.feature.importer.generated.resources.review_note
import com.neteinstein.networth.feature.importer.generated.resources.review_remember_merchant
import com.neteinstein.networth.feature.importer.generated.resources.review_remember_person
import com.neteinstein.networth.feature.importer.generated.resources.review_remember_phone
import com.neteinstein.networth.feature.importer.generated.resources.review_save
import com.neteinstein.networth.feature.importer.generated.resources.review_save_count
import com.neteinstein.networth.feature.importer.generated.resources.review_saved
import com.neteinstein.networth.feature.importer.generated.resources.review_showing
import com.neteinstein.networth.feature.importer.generated.resources.review_suggested
import com.neteinstein.networth.feature.importer.generated.resources.review_title
import com.neteinstein.networth.feature.importer.ui.components.ScreenHeader
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewContent(
        state = state,
        onBack = onBack,
        onPick = viewModel::onPick,
        onNote = viewModel::onNote,
        onRemember = viewModel::onRemember,
        onAcceptAll = viewModel::onAcceptAll,
        onSave = viewModel::onSave,
    )
}

@Composable
fun ReviewContent(
    state: ReviewUiState,
    onBack: () -> Unit,
    onPick: (String, String) -> Unit,
    onNote: (String, String) -> Unit,
    onRemember: (String, Boolean) -> Unit,
    onAcceptAll: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onSave,
                        enabled = state.done > 0 && !state.saving,
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            if (state.done >
                                0
                            ) {
                                stringResource(Res.string.review_save_count, state.done)
                            } else {
                                stringResource(Res.string.review_save)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (!state.ready) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val currency = CurrencyCode.of(state.currencyCode)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val cards = Modifier.padding(horizontal = 20.dp)
                item {
                    ScreenHeader(stringResource(Res.string.review_title), stringResource(Res.string.importer_back), onBack) {
                        if (state.total > 0) {
                            Text(
                                stringResource(Res.string.review_done, state.done, state.total),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(Res.string.review_intro, state.knownMerchants),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = cards,
                    )
                }
                state.lastSaved?.let { saved ->
                    item {
                        Text(
                            pluralStringResource(Res.plurals.review_saved, saved, saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = NetWorthTheme.financeColors.positive,
                            modifier = cards,
                        )
                    }
                }
                if (state.items.isEmpty()) {
                    item { EmptyState(stringResource(Res.string.review_empty_title), stringResource(Res.string.review_empty_body), cards) }
                    return@LazyColumn
                }
                item {
                    Row(cards, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.review_showing, state.items.size, state.total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.items.any { it.item.suggestedCategoryId != null && it.draft.categoryId == null }) {
                            TextButton(onClick = onAcceptAll) { Text(stringResource(Res.string.review_accept_all)) }
                        }
                    }
                }
                items(state.items, key = { it.item.transaction.id }) { ui ->
                    ReviewItemCard(ui, state.categories, currency, onPick, onNote, onRemember, cards)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewItemCard(
    ui: ReviewItemUi,
    categories: List<Category>,
    currency: CurrencyCode,
    onPick: (String, String) -> Unit,
    onNote: (String, String) -> Unit,
    onRemember: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = ui.item
    val tx = item.transaction
    SectionCard(modifier, spacing = 10) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tx.description.uppercase(), style = MaterialTheme.typography.bodyMedium, fontFamily = NetWorthTheme.monoFamily)
                Text(
                    listOf(dayMonth(tx.date), hint(item, categories)).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(MoneyFormatter.format(tx.amountMinor, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.choiceIds.forEach { id ->
                val category = categories.firstOrNull { it.id == id } ?: return@forEach
                val shown = ui.shownCategoryId == id
                val confirmed = ui.draft.categoryId == id
                CategoryChip(
                    label = if (shown && !confirmed) stringResource(Res.string.review_suggested, category.name) else category.name,
                    confirmed = confirmed,
                    suggested = shown && !confirmed,
                    onClick = { onPick(tx.id, id) },
                )
            }
            categories.filter { it.id !in item.choiceIds }.takeIf { it.isNotEmpty() }?.let { others ->
                OtherCategories(others, ui.draft.categoryId, onPick = { onPick(tx.id, it) })
            }
        }
        if (item.needsNote) {
            OutlinedTextField(
                value = ui.draft.note,
                onValueChange = { onNote(tx.id, it) },
                label = { Text(stringResource(Res.string.review_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val key = item.ruleKey
        if (key != null) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).selectable(ui.remember, role = Role.Checkbox, onClick = {
                    onRemember(tx.id, !ui.remember)
                }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = ui.remember, onCheckedChange = null)
                Text(
                    when {
                        item.info.phone != null -> stringResource(Res.string.review_remember_phone)
                        item.info.merchant == null -> stringResource(Res.string.review_remember_person, item.info.counterparty.orEmpty())
                        else -> stringResource(Res.string.review_remember_merchant, key)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    confirmed: Boolean,
    suggested: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.heightIn(min = 40.dp).selectable(selected = confirmed, role = Role.RadioButton, onClick = onClick),
        shape = CircleShape,
        color =
            when {
                confirmed -> accent
                suggested -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        contentColor =
            when {
                confirmed -> MaterialTheme.colorScheme.onPrimary
                suggested -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
        border = BorderStroke(1.dp, if (confirmed || suggested) accent else MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** "Other…": every category not offered as a chip. */
@Composable
private fun OtherCategories(
    categories: List<Category>,
    selectedId: String?,
    onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    Box {
        CategoryChip(
            label = selected?.name ?: stringResource(
                Res.string.review_more,
            ),
            confirmed = selected != null,
            suggested = false,
            onClick = {
                expanded =
                    true
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onPick(category.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun dayMonth(date: LocalDate): String = "${date.day} ${shortMonthNames()[date.month.number - 1]}"

@Composable
private fun hint(
    item: ReviewItem,
    categories: List<Category>,
): String {
    val history = item.history
    val suggested = categories.firstOrNull { it.id == item.suggestedCategoryId }
    if (history != null &&
        suggested != null
    ) {
        return stringResource(Res.string.review_hint_history, history.first, history.second, suggested.name)
    }
    val info = item.info
    return when (info.kind) {
        MovementKind.MB_WAY_PERSON -> if (info.phone !=
            null
        ) {
            stringResource(Res.string.review_hint_phone)
        } else {
            stringResource(Res.string.review_hint_transfer, info.counterparty.orEmpty())
        }
        MovementKind.LOW_VALUE_CARD -> stringResource(Res.string.review_hint_low_value)
        MovementKind.CARD_PURCHASE -> stringResource(Res.string.review_hint_card)
        MovementKind.DIRECT_DEBIT, MovementKind.SERVICE_PAYMENT -> stringResource(Res.string.review_hint_debit)
        MovementKind.TRANSFER_OUT -> stringResource(Res.string.review_hint_transfer, info.counterparty.orEmpty())
        MovementKind.CASH_WITHDRAWAL -> stringResource(Res.string.review_hint_cash)
        MovementKind.FEE, MovementKind.TAX -> stringResource(Res.string.review_hint_fee)
        else -> ""
    }
}

internal fun previewReviewState(): ReviewUiState {
    val categories =
        listOf(
            Category("k", "Kids’ activities", "#F0A964"),
            Category("c", "Clothes", "#A8487A"),
            Category("s", "School", "#D9652B"),
            Category("o", "Other", "#A3ABBA"),
        )

    fun item(
        id: String,
        raw: String,
        day: Int,
        amount: Long,
        suggestion: String?,
        history: Pair<Int, Int>?,
    ): ReviewItemUi {
        val tx = Transaction(id, "a1", LocalDate(2026, 9, day), amount, raw)
        val info = Descriptions.describe(raw)
        return ReviewItemUi(
            ReviewItem(
                tx,
                info,
                suggestion,
                history,
                listOfNotNull(suggestion) + listOf("c", "o").filter { it != suggestion },
                info.kind == MovementKind.MB_WAY_PERSON,
                info.key,
            ),
            ReviewDraft(),
        )
    }
    return ReviewUiState(
        ready = true,
        items =
            listOf(
                item("d1", "COMPRA 4417 DECATHLON MATOSINHOS", 12, -8640, "k", 3 to 4),
                item("d2", "MB WAY P/ 9** *** 412", 14, -3500, null, null),
                item("d3", "TRF ASSOC PAIS ESC BASICA", 3, -4500, "s", null),
            ),
        total = 18,
        categories = categories,
        knownMerchants = 146,
    )
}

@Preview
@Composable
private fun ReviewPreview() {
    NetWorthTheme { ReviewContent(previewReviewState(), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}) }
}

@Preview
@Composable
private fun ReviewEmptyDarkPreview() {
    NetWorthTheme(darkTheme = true) { ReviewContent(ReviewUiState(ready = true), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}) }
}

package com.neteinstein.networth.feature.manage.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.ColorKey
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.components.OverlineLabel
import com.neteinstein.networth.core.designsystem.ui.components.ScreenHeader
import com.neteinstein.networth.core.designsystem.ui.components.categoryName
import com.neteinstein.networth.core.designsystem.ui.components.monthName
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.designsystem.ui.theme.categoryColor
import com.neteinstein.networth.core.domain.format.MoneyFormatter
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.feature.manage.generated.resources.Res
import com.neteinstein.networth.feature.manage.generated.resources.manage_add_subcategory
import com.neteinstein.networth.feature.manage.generated.resources.manage_add_subcategory_button
import com.neteinstein.networth.feature.manage.generated.resources.manage_back
import com.neteinstein.networth.feature.manage.generated.resources.manage_budget_per_month
import com.neteinstein.networth.feature.manage.generated.resources.manage_cancel
import com.neteinstein.networth.feature.manage.generated.resources.manage_categories
import com.neteinstein.networth.feature.manage.generated.resources.manage_categories_intro
import com.neteinstein.networth.feature.manage.generated.resources.manage_category_name
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_blue
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_ink
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_orange
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_other
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_plum
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_teal
import com.neteinstein.networth.feature.manage.generated.resources.manage_colour_name_violet
import com.neteinstein.networth.feature.manage.generated.resources.manage_delete
import com.neteinstein.networth.feature.manage.generated.resources.manage_delete_category_body
import com.neteinstein.networth.feature.manage.generated.resources.manage_delete_category_title
import com.neteinstein.networth.feature.manage.generated.resources.manage_merchant_rule_count
import com.neteinstein.networth.feature.manage.generated.resources.manage_merge_body
import com.neteinstein.networth.feature.manage.generated.resources.manage_merge_into
import com.neteinstein.networth.feature.manage.generated.resources.manage_merge_title
import com.neteinstein.networth.feature.manage.generated.resources.manage_monthly_budget
import com.neteinstein.networth.feature.manage.generated.resources.manage_new_category
import com.neteinstein.networth.feature.manage.generated.resources.manage_new_category_title
import com.neteinstein.networth.feature.manage.generated.resources.manage_no_budget
import com.neteinstein.networth.feature.manage.generated.resources.manage_none
import com.neteinstein.networth.feature.manage.generated.resources.manage_peaks_in
import com.neteinstein.networth.feature.manage.generated.resources.manage_remove_subcategory
import com.neteinstein.networth.feature.manage.generated.resources.manage_rule_count
import com.neteinstein.networth.feature.manage.generated.resources.manage_save
import com.neteinstein.networth.feature.manage.generated.resources.manage_seasonal_peak
import com.neteinstein.networth.feature.manage.generated.resources.manage_subcategories
import com.neteinstein.networth.feature.manage.generated.resources.manage_subcategory_count
import com.neteinstein.networth.feature.manage.generated.resources.manage_warn_spikes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesContent(
        state = state,
        actions = CategoryActions(
            onBack = onBack,
            onToggle = viewModel::onToggle,
            onRename = viewModel::onRename,
            onColorSelected = viewModel::onColorSelected,
            onBudgetChange = viewModel::onBudgetChange,
            onSeasonalPeakChange = viewModel::onSeasonalPeakChange,
            onWarnOnSpikeChange = viewModel::onWarnOnSpikeChange,
            onAddSubcategory = viewModel::onAddSubcategory,
            onRemoveSubcategory = viewModel::onRemoveSubcategory,
            onAddCategory = viewModel::onAddCategory,
            onMerge = viewModel::onMerge,
            onDelete = viewModel::onDelete,
        ),
    )
}

data class CategoryActions(
    val onBack: () -> Unit = {},
    val onToggle: (String) -> Unit = {},
    val onRename: (String, String) -> Unit = { _, _ -> },
    val onColorSelected: (String, String) -> Unit = { _, _ -> },
    val onBudgetChange: (String, String) -> Unit = { _, _ -> },
    val onSeasonalPeakChange: (String, Int?) -> Unit = { _, _ -> },
    val onWarnOnSpikeChange: (String, Boolean) -> Unit = { _, _ -> },
    val onAddSubcategory: (String, String) -> Unit = { _, _ -> },
    val onRemoveSubcategory: (String, String) -> Unit = { _, _ -> },
    val onAddCategory: (String) -> Unit = {},
    val onMerge: (String, String) -> Unit = { _, _ -> },
    val onDelete: (String) -> Unit = {},
)

@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    actions: CategoryActions,
    modifier: Modifier = Modifier,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var merging by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val currency = CurrencyCode.of(state.currencyCode)
    Scaffold(modifier = modifier) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ScreenHeader(stringResource(Res.string.manage_categories), stringResource(Res.string.manage_back), actions.onBack) {
                        Button(onClick = { creating = true }, shape = CircleShape, contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(stringResource(Res.string.manage_new_category), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                item {
                    Text(
                        stringResource(Res.string.manage_categories_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            state.categories.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                                CategoryRow(
                                    item = item,
                                    expanded = item.category.id == state.expandedId,
                                    palette = state.palette,
                                    currency = currency,
                                    actions = actions,
                                    onMerge = { merging = item.category.id },
                                    onDelete = { deleting = item.category.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (creating) {
        NameDialog(onConfirm = {
            creating = false
            actions.onAddCategory(it)
        }, onDismiss = { creating = false })
    }
    merging?.let { sourceId ->
        val source = state.categories.firstOrNull { it.category.id == sourceId }?.category ?: return@let
        MergeDialog(source, state.categories.map { it.category }.filterNot { it.id == sourceId }, onPick = { target ->
            merging = null
            actions.onMerge(sourceId, target)
        }, onDismiss = { merging = null })
    }
    deleting?.let { id ->
        val item = state.categories.firstOrNull { it.category.id == id } ?: return@let
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.manage_delete_category_title, categoryName(item.category.name))) },
            text = { Text(pluralStringResource(Res.plurals.manage_delete_category_body, item.transactionCount, item.transactionCount)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    actions.onDelete(id)
                }) {
                    Text(stringResource(Res.string.manage_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(Res.string.manage_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryRow(
    item: CategoryItem,
    expanded: Boolean,
    palette: List<String>,
    currency: CurrencyCode,
    actions: CategoryActions,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = item.category
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button) { actions.onToggle(category.id) }
                .semantics { stateDescription = if (expanded) "expanded" else "collapsed" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorKey(categoryColor(category.colorHex), size = 14.dp)
            Column(Modifier.weight(1f)) {
                Text(categoryName(category.name), style = MaterialTheme.typography.titleSmall)
                Text(metaLine(item), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                category.monthlyBudgetMinor?.let { stringResource(Res.string.manage_budget_per_month, MoneyFormatter.whole(it, currency)) }
                    ?: stringResource(Res.string.manage_no_budget),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                NetWorthIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f),
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 4.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val shown = categoryName(category.name)
                var name by remember(category.id, shown) { mutableStateOf(shown) }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        actions.onRename(category.id, it)
                    },
                    label = { Text(stringResource(Res.string.manage_category_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OverlineLabel(stringResource(Res.string.manage_colour))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { hex ->
                        val selected = hex.equals(category.colorHex, ignoreCase = true)
                        val label = colourName(hex)
                        Box(
                            Modifier
                                .size(44.dp)
                                .border(3.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .padding(5.dp)
                                .background(categoryColor(hex), CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton) { actions.onColorSelected(category.id, hex) }
                                .semantics { contentDescription = label },
                        )
                    }
                }
                OverlineLabel(stringResource(Res.string.manage_subcategories))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    category.subcategories.forEach { sub -> SubcategoryChip(sub) { actions.onRemoveSubcategory(category.id, sub.id) } }
                }
                var newSub by remember(category.id) { mutableStateOf("") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSub,
                        onValueChange = { newSub = it },
                        placeholder = { Text(stringResource(Res.string.manage_add_subcategory)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = {
                        actions.onAddSubcategory(category.id, newSub)
                        newSub = ""
                    }, enabled = newSub.isNotBlank()) { Text(stringResource(Res.string.manage_add_subcategory_button)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    var budget by remember(category.id) {
                        mutableStateOf(
                            category.monthlyBudgetMinor?.let {
                                MoneyFormatter.format(it, currency, showFraction = false).removePrefix(currency.symbol)
                            }.orEmpty(),
                        )
                    }
                    OutlinedTextField(
                        value = budget,
                        onValueChange = {
                            budget = it
                            actions.onBudgetChange(category.id, it)
                        },
                        label = { Text(stringResource(Res.string.manage_monthly_budget)) },
                        prefix = { Text(currency.symbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    SeasonalPeakPicker(category.seasonalPeakMonth, { actions.onSeasonalPeakChange(category.id, it) }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = category.warnOnSpike, onCheckedChange = { actions.onWarnOnSpikeChange(category.id, it) })
                    Text(stringResource(Res.string.manage_warn_spikes), style = MaterialTheme.typography.bodyMedium)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        pluralStringResource(Res.plurals.manage_merchant_rule_count, item.ruleCount, item.ruleCount),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
                    )
                    OutlinedButton(onClick = onMerge, shape = CircleShape) { Text(stringResource(Res.string.manage_merge_into)) }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, NetWorthTheme.financeColors.warningOutline),
                    ) { Text(stringResource(Res.string.manage_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun metaLine(item: CategoryItem): String {
    val category = item.category
    val parts = listOfNotNull(
        pluralStringResource(Res.plurals.manage_subcategory_count, category.subcategories.size, category.subcategories.size),
        pluralStringResource(Res.plurals.manage_rule_count, item.ruleCount, item.ruleCount),
        category.seasonalPeakMonth?.let { stringResource(Res.string.manage_peaks_in, monthName(it)) },
    )
    return parts.joinToString(" · ")
}

@Composable
private fun SubcategoryChip(sub: Subcategory, onRemove: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.background) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(categoryName(sub.name), style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.manage_remove_subcategory, categoryName(sub.name)),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonalPeakPicker(month: Int?, onChange: (Int?) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(Res.string.manage_none)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = month?.let { monthName(it) } ?: none,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.manage_seasonal_peak)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(none) }, onClick = {
                expanded = false
                onChange(null)
            })
            (1..12).forEach { m ->
                DropdownMenuItem(text = { Text(monthName(m)) }, onClick = {
                    expanded = false
                    onChange(m)
                })
            }
        }
    }
}

@Composable
private fun colourName(hex: String): String = when (hex.uppercase()) {
    "#D9652B" -> stringResource(Res.string.manage_colour_name_orange)
    "#2D55D6" -> stringResource(Res.string.manage_colour_name_blue)
    "#3E8C7E" -> stringResource(Res.string.manage_colour_name_teal)
    "#A8487A" -> stringResource(Res.string.manage_colour_name_plum)
    "#6A4FC9" -> stringResource(Res.string.manage_colour_name_violet)
    "#141B2D" -> stringResource(Res.string.manage_colour_name_ink)
    else -> stringResource(Res.string.manage_colour_name_other, hex)
}

@Composable
private fun NameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manage_new_category_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.manage_category_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(Res.string.manage_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.manage_cancel)) } },
    )
}

@Composable
private fun MergeDialog(source: Category, targets: List<Category>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manage_merge_title, source.name)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        stringResource(Res.string.manage_merge_body),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(targets, key = { it.id }) { target ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { onPick(target.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ColorKey(categoryColor(target.colorHex), size = 12.dp)
                        Text(target.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.manage_cancel)) } },
    )
}

@Preview
@Composable
private fun CategoriesPreview() {
    val school = Category(
        "s",
        "School",
        "#D9652B",
        listOf(Subcategory("a", "Books & materials"), Subcategory("b", "Canteen"), Subcategory("c", "School trips")),
        monthlyBudgetMinor = 300_00,
        seasonalPeakMonth = 9,
    )
    NetWorthTheme {
        CategoriesContent(
            CategoriesUiState(
                loading = false,
                categories = listOf(
                    CategoryItem(Category("h", "Housing", "#141B2D", monthlyBudgetMinor = 1_200_00), 18, 40),
                    CategoryItem(school, 12, 31),
                    CategoryItem(Category("g", "Groceries", "#2D55D6"), 22, 90),
                ),
                expandedId = "s",
            ),
            CategoryActions(),
        )
    }
}

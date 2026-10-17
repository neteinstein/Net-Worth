package com.neteinstein.networth.feature.importer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neteinstein.networth.core.designsystem.ui.components.LoadingState
import com.neteinstein.networth.core.designsystem.ui.theme.NetWorthTheme
import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.feature.importer.domain.model.AccountCoverage
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.MonthCoverage
import com.neteinstein.networth.feature.importer.generated.resources.Res
import com.neteinstein.networth.feature.importer.generated.resources.importer_back
import com.neteinstein.networth.feature.importer.generated.resources.importer_error_unknown
import com.neteinstein.networth.feature.importer.generated.resources.importer_import
import com.neteinstein.networth.feature.importer.generated.resources.importer_importing
import com.neteinstein.networth.feature.importer.generated.resources.importer_intro
import com.neteinstein.networth.feature.importer.generated.resources.importer_reading
import com.neteinstein.networth.feature.importer.generated.resources.importer_title
import com.neteinstein.networth.feature.importer.ui.components.CoverageCard
import com.neteinstein.networth.feature.importer.ui.components.ErrorCard
import com.neteinstein.networth.feature.importer.ui.components.HistoryCard
import com.neteinstein.networth.feature.importer.ui.components.PickFileCard
import com.neteinstein.networth.feature.importer.ui.components.ScreenHeader
import com.neteinstein.networth.feature.importer.ui.components.SectionCardContent
import com.neteinstein.networth.feature.importer.ui.components.SummaryCard
import com.neteinstein.networth.feature.importer.ui.components.rememberStatementPicker
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pick = rememberStatementPicker(onPicked = viewModel::onFilePicked, onFailed = viewModel::onPickFailed)
    ImportContent(
        state = state,
        actions =
            ImportActions(
                onBack = onBack,
                onBrowse = pick,
                onAccount = viewModel::onAccountSelected,
                onNewName = viewModel::onNewAccountName,
                onNewType = viewModel::onNewAccountType,
                onInclude = viewModel::onIncludeSection,
                onOwnerToggled = viewModel::onOwnerToggled,
                onAddPerson = viewModel::onAddPerson,
                onImport = viewModel::onImport,
                onReview = onOpenReview,
                onStartOver = viewModel::onStartOver,
                onDismissError = viewModel::onDismissError,
            ),
    )
}

/** Everything the Import screen can ask for. Defaults make previews short. */
data class ImportActions(
    val onBack: () -> Unit = {},
    val onBrowse: () -> Unit = {},
    val onAccount: (Int, String?) -> Unit = { _, _ -> },
    val onNewName: (Int, String) -> Unit = { _, _ -> },
    val onNewType: (Int, AccountType) -> Unit = { _, _ -> },
    val onInclude: (Int, Boolean) -> Unit = { _, _ -> },
    val onOwnerToggled: (Int, String) -> Unit = { _, _ -> },
    val onAddPerson: (Int, String, MemberRole) -> Unit = { _, _, _ -> },
    val onImport: () -> Unit = {},
    val onReview: () -> Unit = {},
    val onStartOver: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

@Composable
fun ImportContent(
    state: ImportUiState,
    actions: ImportActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        if (!state.ready) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom =
                        padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val cards = Modifier.padding(horizontal = 20.dp)
                item { ScreenHeader(stringResource(Res.string.importer_title), stringResource(Res.string.importer_back), actions.onBack) }
                item {
                    Text(
                        stringResource(Res.string.importer_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = cards,
                    )
                }
                val error = state.error ?: if (state.errorUnknown) "" else null
                if (error != null) {
                    item { ErrorCard(error.ifEmpty { stringResource(Res.string.importer_error_unknown) }, actions.onDismissError, cards) }
                }
                when (state.stage) {
                    ImportStage.PICK, ImportStage.READING -> {
                        item {
                            val busy = if (state.stage ==
                                ImportStage.READING
                            ) {
                                stringResource(Res.string.importer_reading, state.fileName.orEmpty())
                            } else {
                                null
                            }
                            PickFileCard(busy, actions.onBrowse, cards)
                        }
                    }
                    ImportStage.CONFIRM, ImportStage.IMPORTING -> {
                        val multiple = state.sections.size > 1
                        items(state.sections, key = { it.index }) { section ->
                            SectionCardContent(
                                section = section,
                                multiple = multiple,
                                accounts = state.accounts,
                                members = state.members,
                                onAccount = { actions.onAccount(section.index, it) },
                                onNewName = { actions.onNewName(section.index, it) },
                                onNewType = { actions.onNewType(section.index, it) },
                                onInclude = { actions.onInclude(section.index, it) },
                                onOwnerToggled = { actions.onOwnerToggled(section.index, it) },
                                onAddPerson = { name, role -> actions.onAddPerson(section.index, name, role) },
                                modifier = cards,
                            )
                        }
                        item {
                            val importing = state.stage == ImportStage.IMPORTING
                            Button(
                                onClick = actions.onImport,
                                enabled = state.canImport && !importing,
                                modifier = cards.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(if (importing) Res.string.importer_importing else Res.string.importer_import))
                            }
                        }
                    }
                    ImportStage.DONE -> {
                        state.summary?.let { summary ->
                            item { SummaryCard(summary, state.overlaps, state.pendingReview, actions.onReview, actions.onStartOver, cards) }
                        }
                    }
                }
                if (state.coverage.isNotEmpty()) item { CoverageCard(state.coverage, state.members, cards) }
                if (state.history.isNotEmpty()) item { HistoryCard(state.history, cards) }
            }
        }
    }
}

internal fun previewImportState(stage: ImportStage): ImportUiState {
    val pedro = Member("p", "Pedro", MemberRole.ADULT)
    val maria = Member("m", "Maria", MemberRole.ADULT)
    val current = Account("a1", "Pedro", AccountType.CURRENT, listOf("p"))
    val joint = Account("joint", "Joint savings", AccountType.SAVINGS, listOf("p", "m"))
    val month = CalendarMonth(2026, 9)
    val coverage =
        listOf(
            AccountCoverage(
                current,
                (11 downTo 0).map {
                    month.plus(-it) to
                        if (it < 3) {
                            MonthCoverage.FULL
                        } else if (it == 3) {
                            MonthCoverage.PARTIAL
                        } else {
                            MonthCoverage.MISSING
                        }
                },
            ),
            AccountCoverage(joint, (11 downTo 0).map { month.plus(-it) to if (it % 4 == 0) MonthCoverage.MISSING else MonthCoverage.FULL }),
        )
    val record = ImportRecord("i1", "a1", "extrato_agosto.csv", "CSV", LocalDate(2026, 8, 1), LocalDate(2026, 8, 31), 231, 0, 0, "Maria")
    return ImportUiState(
        ready = true,
        stage = stage,
        fileName = "extrato_setembro_2026.csv",
        sections =
            listOf(
                SectionUi(
                    0,
                    "Conta à ordem",
                    "000123456789",
                    LocalDate(2026, 9, 1),
                    LocalDate(2026, 9, 30),
                    214,
                    "a1",
                    SectionChoice(0, "a1", "Conta à ordem", AccountType.CURRENT, listOf("p")),
                ),
            ),
        accounts = listOf(current, joint),
        members = listOf(pedro, maria),
        summary =
            ImportSummary(
                "extrato_setembro_2026.csv",
                listOf("Pedro"),
                LocalDate(2026, 9, 1),
                LocalDate(2026, 9, 30),
                rowsRead = 214,
                duplicatesSkipped = 6,
                imported = 208,
                autoCategorised = 190,
                internalTransfers = 0,
                needReview = 18,
                overlappedImportIds = listOf("w"),
                unreconciledAccounts = emptyList(),
            ),
        overlaps = listOf(LocalDate(2026, 9, 22) to LocalDate(2026, 9, 28)),
        coverage = coverage,
        history = listOf(ImportHistoryLine(record, "Maria", "Maria")),
        pendingReview = 18,
    )
}

@Preview
@Composable
private fun ImportPickPreview() {
    NetWorthTheme { ImportContent(previewImportState(ImportStage.PICK), ImportActions()) }
}

@Preview
@Composable
private fun ImportConfirmPreview() {
    NetWorthTheme { ImportContent(previewImportState(ImportStage.CONFIRM), ImportActions()) }
}

@Preview
@Composable
private fun ImportDoneDarkPreview() {
    NetWorthTheme(darkTheme = true) { ImportContent(previewImportState(ImportStage.DONE), ImportActions()) }
}

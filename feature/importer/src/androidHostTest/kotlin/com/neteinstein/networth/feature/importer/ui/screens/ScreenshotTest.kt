package com.neteinstein.networth.feature.importer.ui.screens

import com.neteinstein.networth.core.designsystem.ui.locale.AppLocaleEnvironment
import com.neteinstein.networth.core.testing.screenshot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun importPick() = screenshot("import_pick") { ImportContent(previewImportState(ImportStage.PICK), ImportActions()) }

    @Test
    fun importConfirm() = screenshot("import_confirm") { ImportContent(previewImportState(ImportStage.CONFIRM), ImportActions()) }

    @Test
    fun importConfirmBatch() = screenshot("import_confirm_batch") { ImportContent(batchState(), ImportActions()) }

    /** The batch strings are the longest on this screen; checks they fit in Portuguese. */
    @Test
    fun importConfirmBatchInPortuguese() {
        val original = Locale.getDefault()
        try {
            screenshot("import_confirm_batch_pt") { AppLocaleEnvironment("pt") { ImportContent(batchState(), ImportActions()) } }
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun batchState() = previewImportState(ImportStage.CONFIRM).copy(
        fileNumber = 2,
        fileCount = 12,
        hasMoreFiles = true,
        errors = listOf(FileError("notes.csv", null)),
        coverage = emptyList(),
        history = emptyList(),
    )

    @Test
    fun importDone() = screenshot("import_done") { ImportContent(previewImportState(ImportStage.DONE), ImportActions()) }

    @Test
    fun review() = screenshot("review") { ReviewContent(previewReviewState(), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}) }

    @Test
    fun reviewEmpty() = screenshot("review_empty") {
        ReviewContent(ReviewUiState(ready = true), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})
    }
}

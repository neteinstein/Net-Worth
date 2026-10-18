package com.neteinstein.networth.feature.importer.ui.screens

import com.neteinstein.networth.core.testing.screenshot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    @Test
    fun importPick() = screenshot("import_pick") { ImportContent(previewImportState(ImportStage.PICK), ImportActions()) }

    @Test
    fun importConfirm() = screenshot("import_confirm") { ImportContent(previewImportState(ImportStage.CONFIRM), ImportActions()) }

    @Test
    fun importConfirmBatch() = screenshot("import_confirm_batch") {
        val state = previewImportState(ImportStage.CONFIRM)
        ImportContent(
            state.copy(
                fileNumber = 2,
                fileCount = 12,
                hasMoreFiles = true,
                errors = listOf(FileError("notes.csv", null)),
                coverage = emptyList(),
                history = emptyList(),
            ),
            ImportActions(),
        )
    }

    @Test
    fun importDone() = screenshot("import_done") { ImportContent(previewImportState(ImportStage.DONE), ImportActions()) }

    @Test
    fun review() = screenshot("review") { ReviewContent(previewReviewState(), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}) }

    @Test
    fun reviewEmpty() = screenshot("review_empty") {
        ReviewContent(ReviewUiState(ready = true), {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})
    }
}

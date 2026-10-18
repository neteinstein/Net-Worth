package com.neteinstein.networth.core.designsystem.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberFilePicker(
    kind: FileKind,
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    // The picker only holds its delegate weakly, so keep it for as long as the screen is shown.
    val delegate =
        remember {
            PickerDelegate pick@{ urls ->
                val files =
                    urls.map { url ->
                        val bytes = NSData.dataWithContentsOfURL(url)?.toByteArray() ?: return@pick failed()
                        PickedFile(url.lastPathComponent.orEmpty(), bytes)
                    }
                if (files.isNotEmpty()) picked(files)
            }
        }
    return {
        val picker =
            UIDocumentPickerViewController(
                forOpeningContentTypes = when (kind) {
                    FileKind.STATEMENTS -> listOf(UTTypePDF, UTTypeCommaSeparatedText, UTTypePlainText, UTTypeData)
                    FileKind.JSON -> listOf(UTTypeJSON, UTTypePlainText, UTTypeData)
                },
                asCopy = true,
            )
        picker.allowsMultipleSelection = kind.multiple
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class PickerDelegate(
    private val onUrls: (List<NSURL>) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onUrls(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

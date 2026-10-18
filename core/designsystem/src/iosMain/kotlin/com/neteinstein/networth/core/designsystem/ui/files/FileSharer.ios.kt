package com.neteinstein.networth.core.designsystem.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberFileSharer(): (SharedFile) -> Unit = remember {
    { file ->
        val path = NSTemporaryDirectory() + file.name
        val data = NSString.create(string = file.text).dataUsingEncoding(NSUTF8StringEncoding)
        NSFileManager.defaultManager.createFileAtPath(path, contents = data, attributes = null)
        topViewController()?.let { top ->
            val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
            // On iPad the sheet is a popover and needs an anchor.
            sheet.popoverPresentationController?.sourceView = top.view
            top.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

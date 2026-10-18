@file:OptIn(ExperimentalWasmJsInterop::class)

package com.neteinstein.networth.core.designsystem.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFileSharer(): (SharedFile) -> Unit = remember { { file -> jsDownload(file.name, file.mimeType, file.text) } }

/** Saves [text] as a download named [name]. */
@JsFun(
    """(name, type, text) => {
        const url = URL.createObjectURL(new Blob([text], { type }));
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }""",
)
private external fun jsDownload(
    name: String,
    type: String,
    text: String,
)

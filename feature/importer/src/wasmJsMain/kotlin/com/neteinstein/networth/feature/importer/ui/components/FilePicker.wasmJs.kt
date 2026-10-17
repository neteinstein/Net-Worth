@file:OptIn(ExperimentalWasmJsInterop::class)

package com.neteinstein.networth.feature.importer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

@Composable
actual fun rememberStatementPicker(
    onPicked: (name: String, bytes: ByteArray) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    return {
        scope.launch {
            val result = runCatching { jsPickFile().await<JsString?>()?.toString() }
            if (result.isFailure) {
                failed()
                return@launch
            }
            val file = result.getOrNull() ?: return@launch
            // "name\u0000contents", with each byte as one char (0–255).
            val separator = file.indexOf('\u0000')
            val bytes = ByteArray(file.length - separator - 1) { file[separator + 1 + it].code.toByte() }
            picked(file.substring(0, separator), bytes)
        }
    }
}

/** Opens the browser's file dialog; resolves to "name\u0000bytes-as-chars", or null if cancelled. */
@JsFun(
    """() => new Promise((resolve, reject) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.pdf,.csv,.txt,.xlsx,.ofx,.qfx';
        input.onchange = () => {
            const file = input.files && input.files[0];
            if (!file) { resolve(null); return; }
            const reader = new FileReader();
            reader.onload = () => {
                const bytes = new Uint8Array(reader.result);
                const chunks = [];
                for (let i = 0; i < bytes.length; i += 8192) {
                    chunks.push(String.fromCharCode.apply(null, bytes.subarray(i, i + 8192)));
                }
                resolve(file.name + '\u0000' + chunks.join(''));
            };
            reader.onerror = () => reject(reader.error);
            reader.readAsArrayBuffer(file);
        };
        input.click();
    })""",
)
private external fun jsPickFile(): Promise<JsString?>

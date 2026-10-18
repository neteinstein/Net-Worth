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
    onPicked: (files: List<PickedFile>) -> Unit,
    onFailed: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    return {
        scope.launch {
            val result = runCatching { jsPickFiles().await<JsArray<JsString>?>() }
            if (result.isFailure) {
                failed()
                return@launch
            }
            val array = result.getOrNull() ?: return@launch
            val files =
                (0 until array.length).map { index ->
                    // "name\u0000contents", with each byte as one char (0–255).
                    val file = array[index].toString()
                    val separator = file.indexOf('\u0000')
                    val bytes = ByteArray(file.length - separator - 1) { file[separator + 1 + it].code.toByte() }
                    PickedFile(file.substring(0, separator), bytes)
                }
            if (files.isNotEmpty()) picked(files)
        }
    }
}

/** Opens the browser's file dialog; resolves to one "name\u0000bytes-as-chars" per file, or null if cancelled. */
@JsFun(
    """() => new Promise((resolve, reject) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = '.pdf,.csv,.txt,.xlsx,.ofx,.qfx';
        input.onchange = () => {
            const files = input.files ? Array.from(input.files) : [];
            if (files.length === 0) { resolve(null); return; }
            const read = (file) => new Promise((done, fail) => {
                const reader = new FileReader();
                reader.onload = () => {
                    const bytes = new Uint8Array(reader.result);
                    const chunks = [];
                    for (let i = 0; i < bytes.length; i += 8192) {
                        chunks.push(String.fromCharCode.apply(null, bytes.subarray(i, i + 8192)));
                    }
                    done(file.name + '\u0000' + chunks.join(''));
                };
                reader.onerror = () => fail(reader.error);
                reader.readAsArrayBuffer(file);
            });
            Promise.all(files.map(read)).then(resolve, reject);
        };
        input.click();
    })""",
)
private external fun jsPickFiles(): Promise<JsArray<JsString>?>

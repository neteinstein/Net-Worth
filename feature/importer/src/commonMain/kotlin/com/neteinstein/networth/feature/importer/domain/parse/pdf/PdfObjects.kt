package com.neteinstein.networth.feature.importer.domain.parse.pdf

/** The PDF object model (ISO 32000-1 §7.3), reduced to what text extraction needs. */
internal sealed interface PdfObject

internal data object PdfNull : PdfObject

internal data class PdfBool(
    val value: Boolean,
) : PdfObject

internal data class PdfNumber(
    val value: Double,
) : PdfObject {
    val int: Int get() = value.toInt()
}

internal class PdfString(
    val bytes: ByteArray,
) : PdfObject {
    override fun toString(): String = "PdfString(${bytes.size})"
}

internal data class PdfName(
    val name: String,
) : PdfObject

internal data class PdfArray(
    val items: List<PdfObject>,
) : PdfObject

internal data class PdfDict(
    val entries: Map<String, PdfObject>,
) : PdfObject {
    operator fun get(key: String): PdfObject? = entries[key]
}

internal data class PdfRef(
    val number: Int,
    val generation: Int,
) : PdfObject

/** A stream with its still-encoded bytes; [number]/[generation] are needed to decrypt it. */
internal class PdfStream(
    val dict: PdfDict,
    val raw: ByteArray,
    val number: Int = 0,
    val generation: Int = 0,
) : PdfObject

/** A content-stream operator (only produced when lexing content streams). */
internal data class PdfOperator(
    val name: String,
) : PdfObject

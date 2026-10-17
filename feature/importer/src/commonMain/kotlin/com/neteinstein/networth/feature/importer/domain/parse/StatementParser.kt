package com.neteinstein.networth.feature.importer.domain.parse

import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import com.neteinstein.networth.feature.importer.domain.parse.bank.PdfStatements
import com.neteinstein.networth.feature.importer.domain.parse.pdf.GlyphNames
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfText

/** Reads any supported bank file. The format comes from the content first, the extension second. */
class StatementParser {
    fun parse(
        fileName: String,
        bytes: ByteArray,
    ): ParsedStatement {
        if (bytes.isEmpty()) throw ParseException("The file is empty")
        return when (formatOf(fileName, bytes)) {
            StatementFormat.PDF -> PdfStatements.parse(fileName, PdfText.lines(bytes))
            StatementFormat.XLSX -> TableStatement.parse(fileName, StatementFormat.XLSX, Xlsx.table(bytes))
            StatementFormat.OFX -> Ofx.parse(fileName, text(bytes))
            StatementFormat.CSV -> TableStatement.parse(fileName, StatementFormat.CSV, Csv.parse(text(bytes)))
        }
    }

    fun formatOf(
        fileName: String,
        bytes: ByteArray,
    ): StatementFormat {
        val head = bytes.copyOf(minOf(bytes.size, 512)).decodeToString()
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            head.contains("%PDF") -> StatementFormat.PDF
            bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> StatementFormat.XLSX
            isOfx(head, extension) -> StatementFormat.OFX
            extension == "xls" -> throw ParseException("Old .xls files aren't supported. Save it as .xlsx or CSV and try again")
            else -> StatementFormat.CSV
        }
    }

    private fun isOfx(
        head: String,
        extension: String,
    ): Boolean = head.contains("OFXHEADER") || head.contains("<OFX", ignoreCase = true) || extension == "ofx" || extension == "qfx"

    /** UTF-8 (with or without BOM); anything that isn't valid UTF-8 is read as Windows-1252, the usual fallback for PT banks. */
    internal fun text(bytes: ByteArray): String {
        val utf8 = bytes.decodeToString()
        if (!utf8.contains('\uFFFD')) return utf8.removePrefix("\uFEFF")
        val chars = CharArray(bytes.size) { i -> (bytes[i].toInt() and 0xFF).let { GlyphNames.winAnsi(it) ?: it.toChar() } }
        return chars.concatToString()
    }
}

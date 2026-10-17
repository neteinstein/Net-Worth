package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextCell
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine

/** Builders for synthetic fixtures. Real bank statements are personal documents and never committed. */
internal fun latin1(text: String): ByteArray = ByteArray(text.length) { text[it].code.toByte() }

internal fun hex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

internal fun unhex(text: String): ByteArray = ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

/** zlib stream made of stored (uncompressed) deflate blocks: valid input for any inflater. */
internal fun storedZlib(data: ByteArray): ByteArray {
    val out = ArrayList<Byte>()
    out += 0x78.toByte()
    out += 0x01.toByte()
    var offset = 0
    do {
        val len = minOf(65_535, data.size - offset)
        val last = offset + len >= data.size
        out += (if (last) 1 else 0).toByte()
        out += (len and 0xFF).toByte()
        out += (len shr 8).toByte()
        out += (len.inv() and 0xFF).toByte()
        out += ((len.inv() shr 8) and 0xFF).toByte()
        for (i in 0 until len) out += data[offset + i]
        offset += len
    } while (offset < data.size)
    repeat(4) { out += 0 } // Adler-32 isn't checked
    return out.toByteArray()
}

/** Raw deflate with stored blocks (ZIP method 8). */
internal fun storedDeflate(data: ByteArray): ByteArray = storedZlib(data).let { it.copyOfRange(2, it.size - 4) }

/** Writes PDF objects by number; streams get a correct /Length. No xref is needed: the reader scans. */
internal class PdfBuilder {
    private val parts = ArrayList<ByteArray>()

    fun obj(
        number: Int,
        body: String,
    ): PdfBuilder = apply { parts += latin1("$number 0 obj\n$body\nendobj\n") }

    fun stream(
        number: Int,
        dict: String,
        data: ByteArray,
    ): PdfBuilder =
        apply {
            parts += latin1("$number 0 obj\n<<$dict /Length ${data.size}>>\nstream\n")
            parts += data
            parts += latin1("\nendstream\nendobj\n")
        }

    fun build(trailer: String? = "<< /Root 1 0 R >>"): ByteArray {
        val all = ArrayList<Byte>()
        latin1("%PDF-1.4\n").forEach { all += it }
        parts.forEach { part -> part.forEach { all += it } }
        if (trailer != null) latin1("trailer\n$trailer\n").forEach { all += it }
        latin1("%%EOF\n").forEach { all += it }
        return all.toByteArray()
    }

    companion object {
        /** A one-page document with Helvetica as /F1 and [content] as its content stream. */
        fun simple(content: String): ByteArray =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 /Resources << /Font << /F1 5 0 R >> >> >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>")
                .stream(4, "", latin1(content))
                .obj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
                .build()
    }
}

/** A ZIP archive with the given entries; [deflate] stores them with method 8 (stored deflate blocks). */
internal fun zip(
    entries: Map<String, String>,
    deflate: Boolean = true,
): ByteArray {
    val out = ArrayList<Byte>()

    fun u16(v: Int) {
        out += (v and 0xFF).toByte()
        out += ((v shr 8) and 0xFF).toByte()
    }

    fun u32(v: Int) {
        u16(v and 0xFFFF)
        u16((v ushr 16) and 0xFFFF)
    }
    val central = ArrayList<Triple<String, Int, Pair<Int, Int>>>()
    for ((name, text) in entries) {
        val raw = text.encodeToByteArray()
        val data = if (deflate) storedDeflate(raw) else raw
        val offset = out.size
        u32(0x04034b50)
        u16(20)
        u16(0)
        u16(if (deflate) 8 else 0)
        u16(0)
        u16(0)
        u32(0)
        u32(data.size)
        u32(raw.size)
        u16(name.length)
        u16(0)
        name.encodeToByteArray().forEach { out += it }
        data.forEach { out += it }
        central += Triple(name, offset, data.size to raw.size)
    }
    val cdStart = out.size
    for ((name, offset, sizes) in central) {
        u32(0x02014b50)
        u16(20)
        u16(20)
        u16(0)
        u16(if (deflate) 8 else 0)
        u16(0)
        u16(0)
        u32(0)
        u32(sizes.first)
        u32(sizes.second)
        u16(name.length)
        u16(0)
        u16(0)
        u16(0)
        u16(0)
        u32(0)
        u32(offset)
        name.encodeToByteArray().forEach { out += it }
    }
    val cdSize = out.size - cdStart
    u32(0x06054b50)
    u16(0)
    u16(0)
    u16(central.size)
    u16(central.size)
    u32(cdSize)
    u32(cdStart)
    u16(0)
    return out.toByteArray()
}

/** A PDF text line with cells at the given x positions (each cell ~5pt per character). */
internal fun line(
    page: Int,
    y: Number,
    vararg cells: Pair<Double, String>,
): PdfTextLine = PdfTextLine(page, y.toDouble(), cells.map { (x, text) -> PdfTextCell(x, x + text.length * 5.0, text) })

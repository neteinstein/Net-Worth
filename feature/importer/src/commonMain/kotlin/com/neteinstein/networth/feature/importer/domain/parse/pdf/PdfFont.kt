package com.neteinstein.networth.feature.importer.domain.parse.pdf

/**
 * Maps a font's character codes to text and widths.
 *
 * Order of trust: a ToUnicode CMap, then glyph names from `/Differences` (standard names,
 * `uniXXXX`, and the two conventions bank statement generators use for Type 3 fonts: `C2c`
 * — a hex WinAnsi code — and `32` — a decimal one), then the base encoding (WinAnsi).
 */
internal class PdfFont(
    private val twoByte: Boolean,
    private val toUnicode: Map<Int, String>,
    private val differences: Map<Int, String>,
    private val widths: Map<Int, Double>,
    private val defaultWidth: Double,
    /** Glyph space → text space for widths (0.001 for normal fonts; `FontMatrix[0]` for Type 3). */
    val widthScale: Double,
    /** Approximate height of the em in text space units, per unit of font size. */
    val heightScale: Double,
) {
    /** Splits [bytes] into (code, text, width in glyph units) triples. */
    fun decode(bytes: ByteArray): List<Glyph> {
        val glyphs = ArrayList<Glyph>(bytes.size)
        var i = 0
        while (i < bytes.size) {
            val code: Int
            if (twoByte && i + 1 < bytes.size) {
                code = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                i += 2
            } else {
                code = bytes[i].toInt() and 0xFF
                i++
            }
            glyphs += Glyph(code, text(code), widths[code] ?: defaultWidth, isSpace = !twoByte && code == 32)
        }
        return glyphs
    }

    private fun text(code: Int): String {
        toUnicode[code]?.let { return it }
        differences[code]?.let { name -> GlyphNames.toText(name)?.let { return it } }
        if (twoByte) return ""
        return GlyphNames.winAnsi(code)?.toString() ?: ""
    }

    class Glyph(
        val code: Int,
        val text: String,
        val width: Double,
        val isSpace: Boolean,
    )

    companion object {
        val FALLBACK = PdfFont(false, emptyMap(), emptyMap(), emptyMap(), 500.0, 0.001, 1.0)

        fun load(
            doc: PdfDocument,
            dict: PdfDict,
        ): PdfFont {
            val subtype = dict.name("Subtype")
            val toUnicode = (doc.resolve(dict["ToUnicode"]) as? PdfStream)?.let { CMap.parse(doc.streamData(it)) } ?: emptyMap()
            if (subtype == "Type0") {
                val descendant = (doc.resolve(dict["DescendantFonts"]) as? PdfArray)?.items?.firstOrNull()?.let(doc::resolve) as? PdfDict
                val widths = HashMap<Int, Double>()
                val w = (descendant?.get("W")?.let(doc::resolve) as? PdfArray)?.items?.map(doc::resolve) ?: emptyList()
                var i = 0
                while (i < w.size) {
                    val first = (w[i] as? PdfNumber)?.int ?: break
                    val next = w.getOrNull(i + 1)
                    if (next is PdfArray) {
                        next.items.forEachIndexed { k, v -> widths[first + k] = (doc.resolve(v) as? PdfNumber)?.value ?: 1000.0 }
                        i += 2
                    } else {
                        val last = (next as? PdfNumber)?.int ?: break
                        val width = (w.getOrNull(i + 2) as? PdfNumber)?.value ?: 1000.0
                        for (c in first..last) widths[c] = width
                        i += 3
                    }
                }
                val dw = (descendant?.get("DW")?.let(doc::resolve) as? PdfNumber)?.value ?: 1000.0
                return PdfFont(true, toUnicode, emptyMap(), widths, dw, 0.001, 1.0)
            }
            val differences = HashMap<Int, String>()
            when (val encoding = doc.resolve(dict["Encoding"])) {
                is PdfDict -> {
                    val diffs = (doc.resolve(encoding["Differences"]) as? PdfArray)?.items ?: emptyList()
                    var code = 0
                    for (item in diffs) {
                        when (val v = doc.resolve(item)) {
                            is PdfNumber -> code = v.int
                            is PdfName -> differences[code++] = v.name
                            else -> Unit
                        }
                    }
                }
                else -> Unit
            }
            val firstChar = (doc.resolve(dict["FirstChar"]) as? PdfNumber)?.int ?: 0
            val widths = HashMap<Int, Double>()
            (doc.resolve(dict["Widths"]) as? PdfArray)?.items?.forEachIndexed { k, v ->
                widths[firstChar + k] = (doc.resolve(v) as? PdfNumber)?.value ?: 0.0
            }
            var widthScale = 0.001
            var heightScale = 1.0
            var defaultWidth = 500.0
            if (subtype == "Type3") {
                val matrix = (doc.resolve(dict["FontMatrix"]) as? PdfArray)?.items?.map { (doc.resolve(it) as? PdfNumber)?.value ?: 0.0 }
                widthScale = matrix?.getOrNull(0)?.takeIf { it != 0.0 } ?: 0.001
                val bbox = (doc.resolve(dict["FontBBox"]) as? PdfArray)?.items?.map { (doc.resolve(it) as? PdfNumber)?.value ?: 0.0 }
                val yScale = matrix?.getOrNull(3)?.takeIf { it != 0.0 } ?: widthScale
                val height = if (bbox != null && bbox.size == 4) bbox[3] - bbox[1] else 0.0
                heightScale = if (height > 0) height * kotlin.math.abs(yScale) else 1.0
                defaultWidth = widths.values.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: (0.5 / widthScale)
            }
            return PdfFont(false, toUnicode, differences, widths, defaultWidth, widthScale, heightScale)
        }
    }
}

/** Parses the bfchar/bfrange sections of a ToUnicode CMap. */
internal object CMap {
    fun parse(data: ByteArray): Map<Int, String> {
        val map = HashMap<Int, String>()
        val lexer = PdfLexer(data)
        var mode = ""
        val pending = ArrayList<PdfObject>()
        while (true) {
            val obj = lexer.next() ?: break
            if (obj is PdfOperator) {
                when (obj.name) {
                    "beginbfchar", "beginbfrange" -> {
                        mode = obj.name
                        pending.clear()
                    }
                    "endbfchar" -> {
                        pending.chunked(2).forEach { pair ->
                            val src = pair.getOrNull(0) as? PdfString
                            val dst = pair.getOrNull(1) as? PdfString
                            if (src != null && dst != null) map[code(src.bytes)] = utf16(dst.bytes)
                        }
                        mode = ""
                    }
                    "endbfrange" -> {
                        pending.chunked(3).forEach { triple ->
                            val lo = (triple.getOrNull(0) as? PdfString)?.let { code(it.bytes) } ?: return@forEach
                            val hi = (triple.getOrNull(1) as? PdfString)?.let { code(it.bytes) } ?: return@forEach
                            when (val dst = triple.getOrNull(2)) {
                                is PdfString -> {
                                    val base = dst.bytes
                                    for (c in lo..minOf(hi, lo + 0xFFFF)) {
                                        val bytes = base.copyOf()
                                        val add = c - lo
                                        val last = bytes.size - 1
                                        if (last >= 0) {
                                            val v = ((bytes[last].toInt() and 0xFF) + add)
                                            bytes[last] = (v and 0xFF).toByte()
                                            if (last >=
                                                1
                                            ) {
                                                bytes[last - 1] = (((bytes[last - 1].toInt() and 0xFF) + (v shr 8)) and 0xFF).toByte()
                                            }
                                        }
                                        map[c] = utf16(bytes)
                                    }
                                }
                                is PdfArray ->
                                    dst.items.forEachIndexed { k, item ->
                                        (item as? PdfString)?.let { map[lo + k] = utf16(it.bytes) }
                                    }
                                else -> Unit
                            }
                        }
                        mode = ""
                    }
                }
            } else if (mode.isNotEmpty()) {
                pending += obj
            }
        }
        return map
    }

    private fun code(bytes: ByteArray): Int = bytes.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }

    private fun utf16(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 1 < bytes.size) {
            sb.append((((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)).toChar())
            i += 2
        }
        if (bytes.size == 1) sb.append((bytes[0].toInt() and 0xFF).toChar())
        return sb.toString()
    }
}

package com.neteinstein.networth.feature.importer.domain.parse.pdf

import com.neteinstein.networth.feature.importer.domain.parse.Inflate

/** Stream filters that can carry text. Image codecs (DCT, CCITT, JBIG2, JPX) return `null`: there is no text to find. */
internal object PdfFilters {
    fun decode(
        filter: String,
        data: ByteArray,
        parms: PdfDict?,
    ): ByteArray? =
        when (filter) {
            "FlateDecode", "Fl" -> predict(Inflate.zlib(data), parms)
            "LZWDecode", "LZW" -> predict(lzw(data, (parms?.int("EarlyChange") ?: 1) == 1), parms)
            "ASCIIHexDecode", "AHx" -> asciiHex(data)
            "ASCII85Decode", "A85" -> ascii85(data)
            "RunLengthDecode", "RL" -> runLength(data)
            else -> null
        }

    /** PNG (10–15) and TIFF (2) predictors, used by some writers even on content and object streams. */
    private fun predict(
        data: ByteArray,
        parms: PdfDict?,
    ): ByteArray {
        val predictor = parms?.int("Predictor") ?: 1
        if (predictor < 2) return data
        val colors = parms?.int("Colors") ?: 1
        val bpc = parms?.int("BitsPerComponent") ?: 8
        val columns = parms?.int("Columns") ?: 1
        val bpp = maxOf(1, (colors * bpc + 7) / 8)
        val rowLength = (colors * bpc * columns + 7) / 8
        if (predictor == 2) {
            val out = data.copyOf()
            var row = 0
            while (row < out.size) {
                for (i in bpp until minOf(rowLength, out.size - row)) {
                    out[row + i] = (out[row + i] + out[row + i - bpp]).toByte()
                }
                row += rowLength
            }
            return out
        }
        val out = ByteBuilder(data.size)
        var prev = IntArray(rowLength)
        var pos = 0
        while (pos < data.size) {
            val type = data[pos++].toInt() and 0xFF
            val cur = IntArray(rowLength)
            for (i in 0 until rowLength) {
                val raw = if (pos < data.size) data[pos++].toInt() and 0xFF else 0
                val left = if (i >= bpp) cur[i - bpp] else 0
                val up = prev[i]
                val upLeft = if (i >= bpp) prev[i - bpp] else 0
                cur[i] =
                    when (type) {
                        1 -> raw + left
                        2 -> raw + up
                        3 -> raw + (left + up) / 2
                        4 -> raw + paeth(left, up, upLeft)
                        else -> raw
                    } and 0xFF
            }
            for (v in cur) out.add(v)
            prev = cur
        }
        return out.toByteArray()
    }

    private fun paeth(
        a: Int,
        b: Int,
        c: Int,
    ): Int {
        val p = a + b - c
        val pa = kotlin.math.abs(p - a)
        val pb = kotlin.math.abs(p - b)
        val pc = kotlin.math.abs(p - c)
        return if (pa <= pb && pa <= pc) {
            a
        } else if (pb <= pc) {
            b
        } else {
            c
        }
    }

    fun lzw(
        data: ByteArray,
        earlyChange: Boolean,
    ): ByteArray {
        val out = ByteBuilder(data.size * 3)
        val table = ArrayList<ByteArray>(4096)

        fun reset() {
            table.clear()
            for (i in 0 until 256) table += byteArrayOf(i.toByte())
            table += ByteArray(0) // 256 clear
            table += ByteArray(0) // 257 end
        }
        reset()
        var codeLength = 9
        var bitBuf = 0L
        var bitCnt = 0
        var prev: ByteArray? = null
        var pos = 0
        while (true) {
            while (bitCnt < codeLength && pos < data.size) {
                bitBuf = (bitBuf shl 8) or (data[pos++].toLong() and 0xFF)
                bitCnt += 8
            }
            if (bitCnt < codeLength) break
            val code = ((bitBuf shr (bitCnt - codeLength)) and ((1L shl codeLength) - 1)).toInt()
            bitCnt -= codeLength
            when {
                code == 256 -> {
                    reset()
                    codeLength = 9
                    prev = null
                    continue
                }
                code == 257 -> break
            }
            val entry =
                when {
                    code < table.size -> table[code]
                    prev != null -> prev + prev[0]
                    else -> break
                }
            out.addAll(entry)
            if (prev != null && table.size < 4096) table += prev + entry[0]
            prev = entry
            val limit = if (earlyChange) table.size + 1 else table.size
            codeLength =
                when {
                    limit >= 2048 -> 12
                    limit >= 1024 -> 11
                    limit >= 512 -> 10
                    else -> 9
                }
        }
        return out.toByteArray()
    }

    private fun asciiHex(data: ByteArray): ByteArray {
        val out = ByteBuilder(data.size / 2)
        var high = -1
        for (b in data) {
            val c = b.toInt()
            if (c == '>'.code) break
            val v = PdfLexer.hexValue(c)
            if (v < 0) continue
            if (high < 0) {
                high = v
            } else {
                out.add(high * 16 + v)
                high = -1
            }
        }
        if (high >= 0) out.add(high * 16)
        return out.toByteArray()
    }

    private fun ascii85(data: ByteArray): ByteArray {
        val out = ByteBuilder(data.size)
        val group = IntArray(5)
        var n = 0
        var i = 0
        if (data.size >= 2 && data[0].toInt() == '<'.code && data[1].toInt() == '~'.code) i = 2
        while (i < data.size) {
            val c = data[i++].toInt()
            if (c == '~'.code) break
            if (PdfLexer.isWhite(c)) continue
            if (c == 'z'.code && n == 0) {
                repeat(4) { out.add(0) }
                continue
            }
            if (c < '!'.code || c > 'u'.code) continue
            group[n++] = c - '!'.code
            if (n == 5) {
                emit85(group, 4, out)
                n = 0
            }
        }
        if (n > 1) {
            for (k in n until 5) group[k] = 84
            emit85(group, n - 1, out)
        }
        return out.toByteArray()
    }

    private fun emit85(
        group: IntArray,
        count: Int,
        out: ByteBuilder,
    ) {
        var value = 0L
        for (g in group) value = value * 85 + g
        for (k in 0 until count) out.add(((value shr (24 - 8 * k)) and 0xFF).toInt())
    }

    private fun runLength(data: ByteArray): ByteArray {
        val out = ByteBuilder(data.size * 2)
        var i = 0
        while (i < data.size) {
            val len = data[i++].toInt() and 0xFF
            when {
                len == 128 -> break
                len < 128 -> {
                    for (k in 0..len) if (i < data.size) out.add(data[i++].toInt())
                }
                else -> {
                    if (i < data.size) {
                        val b = data[i++].toInt()
                        repeat(257 - len) { out.add(b) }
                    }
                }
            }
        }
        return out.toByteArray()
    }
}

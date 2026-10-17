package com.neteinstein.networth.feature.importer.domain.parse

/**
 * DEFLATE decoder (RFC 1951), with the zlib wrapper (RFC 1950) handled by [zlib].
 *
 * Bank PDFs compress their content streams with FlateDecode and XLSX files are ZIPs of deflated
 * XML, so the importer needs this on every platform. It is small enough to keep in common code.
 */
object Inflate {
    /** Inflates a zlib stream (2-byte header, deflate data, Adler-32). The checksum is not verified. */
    fun zlib(data: ByteArray): ByteArray {
        if (data.size < 2) throw ParseException("Compressed data is truncated")
        val cmf = data[0].toInt() and 0xFF
        val flg = data[1].toInt() and 0xFF
        val looksLikeZlib = cmf and 0x0F == 8 && ((cmf shl 8) or flg) % 31 == 0
        // Some PDF writers emit raw deflate in FlateDecode streams; accept that too.
        return if (looksLikeZlib) raw(data, 2) else raw(data, 0)
    }

    /**
     * Inflates raw deflate data starting at [offset]. Truncated input returns what was decoded so
     * far, as PDF readers do, because damaged streams are common in bank exports.
     */
    fun raw(
        data: ByteArray,
        offset: Int = 0,
    ): ByteArray {
        val decoder = Decoder(data, offset)
        decoder.run()
        return decoder.output()
    }

    private class Decoder(
        private val input: ByteArray,
        private var pos: Int,
    ) {
        private var bitBuf = 0
        private var bitCnt = 0
        private var out = ByteArray(maxOf(1024, (input.size - pos) * 4))
        private var outLen = 0

        fun output(): ByteArray = out.copyOf(outLen)

        fun run() {
            try {
                do {
                    val last = bits(1)
                    when (bits(2)) {
                        0 -> stored()
                        1 -> codes(FIXED_LIT, FIXED_DIST)
                        2 -> dynamic()
                        else -> throw ParseException("Invalid deflate block type")
                    }
                } while (last == 0)
            } catch (_: EndOfInput) {
                // Keep the partial output.
            }
        }

        private fun bits(need: Int): Int {
            var value = bitBuf
            while (bitCnt < need) {
                if (pos >= input.size) throw EndOfInput()
                value = value or ((input[pos++].toInt() and 0xFF) shl bitCnt)
                bitCnt += 8
            }
            bitBuf = value ushr need
            bitCnt -= need
            return value and ((1 shl need) - 1)
        }

        private fun put(b: Int) {
            if (outLen == out.size) out = out.copyOf(out.size * 2)
            out[outLen++] = b.toByte()
        }

        private fun stored() {
            bitBuf = 0
            bitCnt = 0
            if (pos + 4 > input.size) throw EndOfInput()
            val len = (input[pos].toInt() and 0xFF) or ((input[pos + 1].toInt() and 0xFF) shl 8)
            pos += 4
            repeat(len) {
                if (pos >= input.size) throw EndOfInput()
                put(input[pos++].toInt())
            }
        }

        private fun decode(h: Huffman): Int {
            var code = 0
            var first = 0
            var index = 0
            for (len in 1..MAX_BITS) {
                code = code or bits(1)
                val count = h.count[len]
                if (code - count < first) return h.symbol[index + (code - first)]
                index += count
                first += count
                first = first shl 1
                code = code shl 1
            }
            throw ParseException("Invalid deflate code")
        }

        private fun codes(
            lit: Huffman,
            dist: Huffman,
        ) {
            while (true) {
                val symbol = decode(lit)
                when {
                    symbol < 256 -> put(symbol)
                    symbol == 256 -> return
                    else -> {
                        val s = symbol - 257
                        if (s >= 29) throw ParseException("Invalid deflate length")
                        val len = LENGTH_BASE[s] + bits(LENGTH_EXTRA[s])
                        val d = decode(dist)
                        if (d >= 30) throw ParseException("Invalid deflate distance")
                        val distance = DIST_BASE[d] + bits(DIST_EXTRA[d])
                        if (distance > outLen) throw ParseException("Deflate distance too far back")
                        repeat(len) { put(out[outLen - distance].toInt()) }
                    }
                }
            }
        }

        private fun dynamic() {
            val nlen = bits(5) + 257
            val ndist = bits(5) + 1
            val ncode = bits(4) + 4
            val lengths = IntArray(320)
            for (i in 0 until ncode) lengths[CODE_ORDER[i]] = bits(3)
            val lenCode = Huffman(lengths.copyOf(19))
            val all = IntArray(nlen + ndist)
            var index = 0
            while (index < nlen + ndist) {
                val symbol = decode(lenCode)
                if (symbol < 16) {
                    all[index++] = symbol
                } else {
                    var repeatValue = 0
                    val times =
                        when (symbol) {
                            16 -> {
                                if (index == 0) throw ParseException("Invalid deflate repeat")
                                repeatValue = all[index - 1]
                                3 + bits(2)
                            }
                            17 -> 3 + bits(3)
                            else -> 11 + bits(7)
                        }
                    if (index + times > nlen + ndist) throw ParseException("Invalid deflate lengths")
                    repeat(times) { all[index++] = repeatValue }
                }
            }
            codes(Huffman(all.copyOfRange(0, nlen)), Huffman(all.copyOfRange(nlen, nlen + ndist)))
        }
    }

    private class EndOfInput : RuntimeException()

    private class Huffman(
        lengths: IntArray,
    ) {
        val count = IntArray(MAX_BITS + 1)
        val symbol = IntArray(lengths.size)

        init {
            for (len in lengths) count[len]++
            count[0] = 0
            val offsets = IntArray(MAX_BITS + 2)
            for (len in 1..MAX_BITS) offsets[len + 1] = offsets[len] + count[len]
            for (s in lengths.indices) {
                if (lengths[s] != 0) symbol[offsets[lengths[s]]++] = s
            }
        }
    }

    private const val MAX_BITS = 15
    private val CODE_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
    private val LENGTH_BASE =
        intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
    private val LENGTH_EXTRA = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
    private val DIST_BASE =
        ints("1 2 3 4 5 7 9 13 17 25 33 49 65 97 129 193 257 385 513 769 1025 1537 2049 3073 4097 6145 8193 12289 16385 24577")
    private val DIST_EXTRA = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)

    private val FIXED_LIT =
        Huffman(
            IntArray(288) { i ->
                if (i < 144) {
                    8
                } else if (i < 256) {
                    9
                } else if (i < 280) {
                    7
                } else {
                    8
                }
            },
        )
    private val FIXED_DIST = Huffman(IntArray(30) { 5 })
}

/** A statement, archive or document that could not be read. The message is shown to the user. */
class ParseException(
    message: String,
) : Exception(message)

private fun ints(values: String): IntArray = values.split(' ').map { it.toInt() }.toIntArray()

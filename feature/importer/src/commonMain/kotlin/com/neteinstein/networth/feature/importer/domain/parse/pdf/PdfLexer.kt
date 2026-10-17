package com.neteinstein.networth.feature.importer.domain.parse.pdf

/** Tokenises PDF syntax from [data], starting at [pos]. Used for both file objects and content streams. */
internal class PdfLexer(
    private val data: ByteArray,
    var pos: Int = 0,
) {
    /** Reads the next object, or `null` at the end. Bare keywords come back as [PdfOperator]. */
    fun next(): PdfObject? {
        skipWhitespace()
        if (pos >= data.size) return null
        return when (val c = data[pos].toInt().toChar()) {
            '/' -> name()
            '(' -> literalString()
            '<' ->
                if (peek(1) == '<') {
                    pos += 2
                    dictionary()
                } else {
                    hexString()
                }
            '[' -> {
                pos++
                array()
            }
            ']', '>', ')', '{', '}' -> {
                pos++
                PdfOperator(c.toString())
            }
            else ->
                if (c.isDigit() || c == '-' || c == '+' || c == '.') {
                    numberOrRef()
                } else {
                    keyword()
                }
        }
    }

    /** Reads an object, resolving `n g R` into a [PdfRef]; operators other than `null`/booleans are errors. */
    fun value(): PdfObject =
        when (val obj = next()) {
            null -> PdfNull
            is PdfOperator -> PdfNull
            else -> obj
        }

    private fun peek(offset: Int): Char = if (pos + offset < data.size) data[pos + offset].toInt().toChar() else '\u0000'

    fun skipWhitespace(): PdfLexer {
        while (pos < data.size) {
            val c = data[pos].toInt()
            if (c == '%'.code) {
                while (pos < data.size && data[pos].toInt() != 10 && data[pos].toInt() != 13) pos++
            } else if (isWhite(c)) {
                pos++
            } else {
                break
            }
        }
        return this
    }

    private fun dictionary(): PdfDict {
        val entries = LinkedHashMap<String, PdfObject>()
        while (true) {
            skipWhitespace()
            if (pos >= data.size) break
            if (data[pos].toInt() == '>'.code && peek(1) == '>') {
                pos += 2
                break
            }
            val key = next() as? PdfName ?: continue
            val value = next() ?: break
            if (value is PdfOperator) {
                if (value.name == ">") break
                continue
            }
            entries[key.name] = value
        }
        return PdfDict(entries)
    }

    private fun array(): PdfArray {
        val items = ArrayList<PdfObject>()
        while (true) {
            val obj = next() ?: break
            if (obj is PdfOperator) {
                if (obj.name == "]") break
                continue
            }
            items += obj
        }
        return PdfArray(items)
    }

    private fun name(): PdfName {
        pos++
        val sb = StringBuilder()
        while (pos < data.size) {
            val c = data[pos].toInt() and 0xFF
            if (isWhite(c) || isDelimiter(c)) break
            if (c == '#'.code && pos + 2 < data.size) {
                val hex = hexValue(data[pos + 1].toInt()) * 16 + hexValue(data[pos + 2].toInt())
                if (hex >= 0) {
                    sb.append(hex.toChar())
                    pos += 3
                    continue
                }
            }
            sb.append(c.toChar())
            pos++
        }
        return PdfName(sb.toString())
    }

    private fun literalString(): PdfString {
        pos++
        val out = ByteBuilder()
        var depth = 1
        while (pos < data.size) {
            val c = data[pos++].toInt() and 0xFF
            when (c) {
                '('.code -> {
                    depth++
                    out.add(c)
                }
                ')'.code -> {
                    depth--
                    if (depth == 0) break
                    out.add(c)
                }
                '\\'.code -> escape(out)
                else -> out.add(c)
            }
        }
        return PdfString(out.toByteArray())
    }

    private fun escape(out: ByteBuilder) {
        if (pos >= data.size) return
        val c = data[pos++].toInt() and 0xFF
        when (c) {
            'n'.code -> out.add(10)
            'r'.code -> out.add(13)
            't'.code -> out.add(9)
            'b'.code -> out.add(8)
            'f'.code -> out.add(12)
            13 -> if (pos < data.size && data[pos].toInt() == 10) pos++
            10 -> Unit
            in '0'.code..'7'.code -> {
                var value = c - '0'.code
                repeat(2) {
                    if (pos < data.size && data[pos].toInt() in '0'.code..'7'.code) {
                        value = value * 8 + (data[pos++].toInt() - '0'.code)
                    }
                }
                out.add(value and 0xFF)
            }
            else -> out.add(c)
        }
    }

    private fun hexString(): PdfString {
        pos++
        val out = ByteBuilder()
        var high = -1
        while (pos < data.size) {
            val c = data[pos++].toInt()
            if (c == '>'.code) break
            val v = hexValue(c)
            if (v < 0) continue
            if (high < 0) {
                high = v
            } else {
                out.add(high * 16 + v)
                high = -1
            }
        }
        if (high >= 0) out.add(high * 16)
        return PdfString(out.toByteArray())
    }

    private fun numberOrRef(): PdfObject {
        val first = number()
        // "n g R" — look ahead without consuming if it isn't a reference.
        val save = pos
        if (first.value >= 0 && first.value == first.value.toInt().toDouble()) {
            skipWhitespace()
            if (pos < data.size && data[pos].toInt().toChar().isDigit()) {
                val second = number()
                skipWhitespace()
                if (pos < data.size &&
                    data[pos].toInt() == 'R'.code &&
                    (pos + 1 >= data.size || isWhite(data[pos + 1].toInt()) || isDelimiter(data[pos + 1].toInt()))
                ) {
                    pos++
                    return PdfRef(first.int, second.int)
                }
            }
        }
        pos = save
        return first
    }

    private fun number(): PdfNumber {
        val start = pos
        if (data[pos].toInt() == '-'.code || data[pos].toInt() == '+'.code) pos++
        while (pos < data.size) {
            val c = data[pos].toInt().toChar()
            if (c.isDigit() || c == '.') pos++ else break
        }
        val text = data.decodeLatin1(start, pos).replace("--", "-")
        return PdfNumber(text.toDoubleOrNull() ?: 0.0)
    }

    private fun keyword(): PdfObject {
        val start = pos
        while (pos < data.size) {
            val c = data[pos].toInt() and 0xFF
            if (isWhite(c) || isDelimiter(c)) break
            pos++
        }
        if (pos == start) {
            pos++
            return PdfOperator(data[start].toInt().toChar().toString())
        }
        return when (val word = data.decodeLatin1(start, pos)) {
            "null" -> PdfNull
            "true" -> PdfBool(true)
            "false" -> PdfBool(false)
            else -> PdfOperator(word)
        }
    }

    /** Reads raw bytes of an inline image (`BI … ID <data> EI`), skipping them. */
    fun skipInlineImage() {
        while (pos + 2 < data.size) {
            if (isWhite(data[pos].toInt()) &&
                data[pos + 1].toInt() == 'E'.code &&
                data[pos + 2].toInt() == 'I'.code &&
                (pos + 3 >= data.size || isWhite(data[pos + 3].toInt()))
            ) {
                pos += 3
                return
            }
            pos++
        }
        pos = data.size
    }

    companion object {
        fun isWhite(c: Int): Boolean = c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32

        fun isDelimiter(c: Int): Boolean =
            c == '('.code ||
                c == ')'.code ||
                c == '<'.code ||
                c == '>'.code ||
                c == '['.code ||
                c == ']'.code ||
                c == '{'.code ||
                c == '}'.code ||
                c == '/'.code ||
                c == '%'.code

        fun hexValue(c: Int): Int =
            when (c) {
                in '0'.code..'9'.code -> c - '0'.code
                in 'a'.code..'f'.code -> c - 'a'.code + 10
                in 'A'.code..'F'.code -> c - 'A'.code + 10
                else -> -1
            }
    }
}

internal fun ByteArray.decodeLatin1(
    from: Int = 0,
    to: Int = size,
): String {
    val chars = CharArray(to - from) { (this[from + it].toInt() and 0xFF).toChar() }
    return chars.concatToString()
}

internal class ByteBuilder(
    capacity: Int = 64,
) {
    private var buf = ByteArray(capacity)
    var size = 0
        private set

    fun add(b: Int) {
        if (size == buf.size) buf = buf.copyOf(maxOf(16, buf.size * 2))
        buf[size++] = b.toByte()
    }

    fun addAll(bytes: ByteArray) {
        for (b in bytes) add(b.toInt())
    }

    fun toByteArray(): ByteArray = buf.copyOf(size)
}

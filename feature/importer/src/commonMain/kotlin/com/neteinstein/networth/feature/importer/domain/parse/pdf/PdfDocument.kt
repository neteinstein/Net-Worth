package com.neteinstein.networth.feature.importer.domain.parse.pdf

import com.neteinstein.networth.feature.importer.domain.parse.ParseException

/**
 * Reads a PDF file well enough to walk its pages and decode their content streams.
 *
 * Instead of trusting the cross-reference table (often wrong in generated statements), it scans
 * the file for `n g obj` headers; later definitions win, which also covers incremental updates.
 * Compressed object streams (PDF 1.5+) are unpacked. Files encrypted with the Standard security
 * handler and an empty user password (RC4 or AES-128) are decrypted.
 */
internal class PdfDocument private constructor(
    private val data: ByteArray,
) {
    private val offsets = HashMap<Int, Int>()
    private val generations = HashMap<Int, Int>()
    private val cache = HashMap<Int, PdfObject>()
    private val packed = HashMap<Int, Pair<Int, Int>>() // object → (object stream, index)
    private var trailer = PdfDict(emptyMap())
    private var decryptor: Decryptor? = null

    private fun load() {
        if (!startsWith("%PDF")) throw ParseException("This file isn't a PDF")
        scanObjects()
        trailer = findTrailer()
        val encrypt = trailer["Encrypt"]
        if (encrypt != null) {
            decryptor = Decryptor.create(resolve(encrypt) as? PdfDict, resolve(trailer["ID"]))
            cache.clear() // Objects read while finding the trailer were not decrypted yet.
        }
        // Object streams are only known after the plain objects are indexed (and decryption is ready).
        for (number in offsets.keys.toList()) {
            val obj = get(number)
            if (obj is PdfStream && obj.dict.name("Type") == "ObjStm") indexObjectStream(number, obj)
        }
    }

    private fun startsWith(prefix: String): Boolean {
        val head = data.decodeLatin1(0, minOf(data.size, 1024))
        return head.contains(prefix)
    }

    private fun scanObjects() {
        var i = 0
        val size = data.size
        while (i < size - 3) {
            // Find "obj" preceded by whitespace, a generation number and an object number.
            if (data[i] == 'o'.code.toByte() &&
                data[i + 1] == 'b'.code.toByte() &&
                data[i + 2] == 'j'.code.toByte() &&
                (i + 3 >= size || !isRegular(data[i + 3].toInt()))
            ) {
                val header = objectHeaderBefore(i)
                if (header != null) {
                    offsets[header.first] = i + 3
                    generations[header.first] = header.second
                }
            }
            i++
        }
        if (offsets.isEmpty()) throw ParseException("This PDF has no readable content")
    }

    /** Parses "<num> <gen> " backwards from the "obj" keyword at [objAt]; returns (number, generation). */
    private fun objectHeaderBefore(objAt: Int): Pair<Int, Int>? {
        var p = objAt - 1
        if (p < 0 || !PdfLexer.isWhite(data[p].toInt())) return null
        while (p >= 0 && PdfLexer.isWhite(data[p].toInt())) p--
        val genEnd = p + 1
        while (p >= 0 && data[p].toInt().toChar().isDigit()) p--
        if (p + 1 == genEnd) return null
        if (p < 0 || !PdfLexer.isWhite(data[p].toInt())) return null
        while (p >= 0 && PdfLexer.isWhite(data[p].toInt())) p--
        val numEnd = p + 1
        while (p >= 0 && data[p].toInt().toChar().isDigit()) p--
        if (p + 1 == numEnd) return null
        val number = data.decodeLatin1(p + 1, numEnd).toIntOrNull() ?: return null
        return number to (data.decodeLatin1(numEnd, genEnd).trim().toIntOrNull() ?: 0)
    }

    private fun isRegular(c: Int): Boolean = !PdfLexer.isWhite(c) && !PdfLexer.isDelimiter(c)

    private fun findTrailer(): PdfDict {
        // Classic trailers: take the last one that names a Root.
        val text = data.decodeLatin1()
        var best: PdfDict? = null
        var index = text.indexOf("trailer")
        while (index >= 0) {
            val dict = PdfLexer(data, index + "trailer".length).next() as? PdfDict
            if (dict != null && (dict["Root"] != null || best == null)) best = merge(best, dict)
            index = text.indexOf("trailer", index + 1)
        }
        // Cross-reference streams carry the trailer keys in their dictionary.
        for (number in offsets.keys.sorted()) {
            val obj = get(number)
            if (obj is PdfStream && obj.dict.name("Type") == "XRef") best = merge(best, obj.dict)
        }
        return best ?: findRootFallback()
    }

    private fun merge(
        older: PdfDict?,
        newer: PdfDict,
    ): PdfDict = PdfDict((older?.entries ?: emptyMap()) + newer.entries)

    private fun findRootFallback(): PdfDict {
        val catalog = offsets.keys.firstOrNull { (get(it) as? PdfDict)?.name("Type") == "Catalog" }
            ?: throw ParseException("This PDF has no page tree")
        return PdfDict(mapOf("Root" to PdfRef(catalog, 0)))
    }

    private fun indexObjectStream(
        streamNumber: Int,
        stream: PdfStream,
    ) {
        val count = stream.dict.int("N") ?: return
        val decoded = streamData(stream)
        val lexer = PdfLexer(decoded)
        repeat(count) { index ->
            val number = (lexer.next() as? PdfNumber)?.int ?: return
            lexer.next()
            if (number !in offsets) packed[number] = streamNumber to index
        }
    }

    /** Returns the object with [number], parsing (and decrypting) it on first use. */
    fun get(number: Int): PdfObject {
        cache[number]?.let { return it }
        val obj =
            offsets[number]?.let { parseAt(number, it) } ?: packed[number]?.let { (stream, index) -> fromObjectStream(stream, index) }
                ?: PdfNull
        cache[number] = obj
        return obj
    }

    private fun parseAt(
        number: Int,
        offset: Int,
    ): PdfObject {
        val lexer = PdfLexer(data, offset)
        val value = lexer.value()
        lexer.skipWhitespace()
        val generation = generations[number] ?: 0
        val decrypted = decryptor?.let { decryptStrings(value, number, generation) } ?: value
        if (value is PdfDict && startsWithAt(lexer.pos, "stream")) {
            var start = lexer.pos + "stream".length
            if (start < data.size && data[start].toInt() == 13) start++
            if (start < data.size && data[start].toInt() == 10) start++
            val declared = (value["Length"]?.let { if (it is PdfRef && it.number == number) null else resolve(it) } as? PdfNumber)?.int
            val end =
                if (declared != null && start + declared <= data.size && endstreamNear(start + declared)) {
                    start + declared
                } else {
                    findEndstream(start)
                }
            return PdfStream(decrypted as PdfDict, data.copyOfRange(start, end), number, generation)
        }
        return decrypted
    }

    private fun startsWithAt(
        at: Int,
        word: String,
    ): Boolean {
        if (at + word.length > data.size) return false
        for (i in word.indices) if (data[at + i].toInt() != word[i].code) return false
        return true
    }

    private fun endstreamNear(at: Int): Boolean {
        var p = at
        while (p < data.size && PdfLexer.isWhite(data[p].toInt())) p++
        return startsWithAt(p, "endstream")
    }

    private fun findEndstream(from: Int): Int {
        var p = from
        while (p < data.size && !startsWithAt(p, "endstream")) p++
        var end = p
        if (end > from && data[end - 1].toInt() == 10) end--
        if (end > from && data[end - 1].toInt() == 13) end--
        return end
    }

    private fun fromObjectStream(
        streamNumber: Int,
        index: Int,
    ): PdfObject {
        val stream = get(streamNumber) as? PdfStream ?: return PdfNull
        val count = stream.dict.int("N") ?: return PdfNull
        val first = stream.dict.int("First") ?: return PdfNull
        val decoded = streamData(stream)
        val lexer = PdfLexer(decoded)
        var offset = -1
        for (i in 0 until count) {
            lexer.next()
            val o = (lexer.next() as? PdfNumber)?.int ?: return PdfNull
            if (i == index) offset = o
        }
        if (offset < 0) return PdfNull
        // Objects inside object streams are never encrypted on their own.
        return PdfLexer(decoded, first + offset).value()
    }

    private fun decryptStrings(
        obj: PdfObject,
        number: Int,
        generation: Int,
    ): PdfObject {
        val d = decryptor ?: return obj
        return when (obj) {
            is PdfString -> PdfString(d.decrypt(obj.bytes, number, generation))
            is PdfArray -> PdfArray(obj.items.map { decryptStrings(it, number, generation) })
            is PdfDict ->
                if (obj.name("Filter") == "Standard" && obj["O"] != null) {
                    obj
                } else {
                    PdfDict(obj.entries.mapValues { decryptStrings(it.value, number, generation) })
                }
            else -> obj
        }
    }

    fun resolve(obj: PdfObject?): PdfObject {
        var current = obj ?: return PdfNull
        var depth = 0
        while (current is PdfRef && depth++ < 32) current = get(current.number)
        return current
    }

    /** Decoded bytes of a stream object, after decryption and filters. */
    fun streamData(stream: PdfStream): ByteArray {
        var bytes = stream.raw
        val d = decryptor
        if (d != null && stream.dict.name("Type") != "XRef") bytes = d.decrypt(bytes, stream.number, stream.generation)
        val filters =
            when (val f = resolve(stream.dict["Filter"])) {
                is PdfName -> listOf(f.name)
                is PdfArray -> f.items.mapNotNull { (resolve(it) as? PdfName)?.name }
                else -> emptyList()
            }
        val parms =
            when (val p = resolve(stream.dict["DecodeParms"])) {
                is PdfDict -> listOf(p)
                is PdfArray -> p.items.map { resolve(it) as? PdfDict }
                else -> emptyList()
            }
        filters.forEachIndexed { i, filter ->
            bytes = PdfFilters.decode(filter, bytes, parms.getOrNull(i)) ?: return bytes
        }
        return bytes
    }

    /** Pages in order, each with its inherited resources. */
    fun pages(): List<PdfPage> {
        val catalog = resolve(trailer["Root"]) as? PdfDict ?: throw ParseException("This PDF has no page tree")
        val result = ArrayList<PdfPage>()
        walk(catalog["Pages"], null, result, HashSet())
        return result
    }

    private fun walk(
        node: PdfObject?,
        inherited: PdfDict?,
        out: MutableList<PdfPage>,
        seen: MutableSet<Int>,
    ) {
        if (node is PdfRef && !seen.add(node.number)) return
        val dict = resolve(node) as? PdfDict ?: return
        val resources = (resolve(dict["Resources"]) as? PdfDict) ?: inherited
        val kids = resolve(dict["Kids"]) as? PdfArray
        if (kids != null && dict.name("Type") != "Page") {
            for (kid in kids.items) walk(kid, resources, out, seen)
        } else {
            out += PdfPage(dict, resources ?: PdfDict(emptyMap()))
        }
    }

    /** Concatenated, decoded content streams of [page]. */
    fun contents(page: PdfPage): ByteArray {
        val builder = ByteBuilder(4096)
        val refs =
            when (val c = resolve(page.dict["Contents"])) {
                is PdfArray -> c.items
                is PdfStream -> listOf(c)
                else -> emptyList()
            }
        for (ref in refs) {
            val stream = resolve(ref) as? PdfStream ?: continue
            builder.addAll(streamData(stream))
            builder.add('\n'.code)
        }
        return builder.toByteArray()
    }

    companion object {
        fun open(data: ByteArray): PdfDocument = PdfDocument(data).also { it.load() }
    }
}

internal class PdfPage(
    val dict: PdfDict,
    val resources: PdfDict,
)

internal fun PdfDict.name(key: String): String? = (this[key] as? PdfName)?.name

internal fun PdfDict.int(key: String): Int? = (this[key] as? PdfNumber)?.int

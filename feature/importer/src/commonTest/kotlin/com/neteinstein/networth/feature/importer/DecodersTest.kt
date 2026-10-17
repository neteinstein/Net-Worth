package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.feature.importer.domain.parse.Inflate
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.pdf.Aes128
import com.neteinstein.networth.feature.importer.domain.parse.pdf.Md5
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfArray
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfBool
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfDict
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfFilters
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfLexer
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfName
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfNull
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfNumber
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfOperator
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfRef
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfString
import com.neteinstein.networth.feature.importer.domain.parse.pdf.Rc4
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class DecodersTest {
    @Test
    fun inflatesDynamicHuffmanZlib() {
        // zlib.compress(b'Net Worth imports bank statements. ' * 20 + bytes(range(256)), 9)
        val compressed =
            Base64.decode(
                "eNrzSy1RCM8vKslQyMwtANLFCkmJedkKxSWJJam5qXklxXoKfqNKRpUMRiUMjEzMLKxs7BycXNw8vHz8AoJCwiKiYuISklLSMrJy8gqKSsoqqmrqGppa2jq6evoGhkbGJqZm5haWVtY2tnb2Do5Ozi6ubu4enl7ePr5+/gGBQcEhoWHhEZFR0TGxcfEJiUnJKalp6RmZWdk5uXn5BYVFxSWlZeUVlVXVNbV19Q2NTc0trW3tHZ1d3T29ff0TJk6aPGXqtOkzZs6aPWfuvPkLFi5avGTpsuUrVq5avWbtuvUbNm7avGXrtu07du7avWfvvv0HDh46fOToseMnTp46febsufMXLl66fOXqtes3bt66fefuvfsPHj56/OTps+cvXr56/ebtu/cfPn76/OXrt+8/fv76/efvv/8A9tWDLA==",
            )
        val expected = latin1("Net Worth imports bank statements. ".repeat(20)) + ByteArray(256) { it.toByte() }
        assertContentEquals(expected, Inflate.zlib(compressed))
    }

    @Test
    fun inflatesFixedHuffmanRawDeflate() {
        assertEquals("hello hello hello hello", Inflate.raw(Base64.decode("y0jNyclXyEAnAQ==")).decodeToString())
        // FlateDecode streams without the zlib header are accepted too.
        assertEquals("hello hello hello hello", Inflate.zlib(Base64.decode("y0jNyclXyEAnAQ==")).decodeToString())
    }

    @Test
    fun inflatesStoredBlocksAndKeepsPartialOutput() {
        val data = ByteArray(70_000) { (it % 251).toByte() }
        assertContentEquals(data, Inflate.zlib(storedZlib(data)))
        val truncated = storedZlib(latin1("abcdef")).copyOf(9)
        assertEquals("ab", Inflate.zlib(truncated).decodeToString())
        assertFailsWith<ParseException> { Inflate.zlib(byteArrayOf(1)) }
        assertFailsWith<ParseException> { Inflate.raw(byteArrayOf(0x07)) } // block type 3
    }

    @Test
    fun md5MatchesReferenceVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex(Md5.digest(ByteArray(0))))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hex(Md5.digest(latin1("abc"))))
        assertEquals("9e107d9d372bb6826bd81d3542a419d6", hex(Md5.digest(latin1("The quick brown fox jumps over the lazy dog"))))
        assertEquals("36a92cc94a9e0fa21f625f8bfb007adf", hex(Md5.digest(latin1("a".repeat(100)))))
    }

    @Test
    fun rc4MatchesReferenceVectorAndIsSymmetric() {
        val cipher = Rc4.apply(latin1("Key"), latin1("Plaintext"))
        assertEquals("bbf316e8d940af0ad3", hex(cipher))
        assertEquals("Plaintext", Rc4.apply(latin1("Key"), cipher).decodeToString())
    }

    @Test
    fun aesDecryptsFipsVector() {
        // FIPS-197 appendix C.1
        val key = unhex("000102030405060708090a0b0c0d0e0f")
        val plain = unhex("00112233445566778899aabbccddeeff")
        val block = Aes128.decryptBlock(Aes128.expandKey(key), unhex("69c4e0d86a7b0430d8cdb78070b4c55a"), 0)
        assertContentEquals(plain, ByteArray(16) { block[it].toByte() })
    }

    @Test
    fun aesCbcRemovesIvAndPadding() {
        // Too short or misaligned input yields nothing rather than garbage.
        assertEquals(0, Aes128.decryptCbc(ByteArray(16), ByteArray(20)).size)
        val out = Aes128.decryptCbc(ByteArray(16), ByteArray(32))
        assertEquals(true, out.size <= 16)
    }

    @Test
    fun filtersDecodeAsciiHexAscii85RunLengthAndLzw() {
        assertEquals("Hi", PdfFilters.decode("ASCIIHexDecode", latin1("48 69>"), null)!!.decodeToString())
        assertContentEquals(byteArrayOf(0x40), PdfFilters.decode("AHx", latin1("4"), null))
        assertEquals(
            "Hello, statements!",
            PdfFilters.decode("ASCII85Decode", latin1("<~87cURD_*#PFCB9&D.RU,F!1~>"), null)!!.decodeToString(),
        )
        assertContentEquals(
            latin1("aaab"),
            PdfFilters.decode("RunLengthDecode", byteArrayOf(-2, 'a'.code.toByte(), 0, 'b'.code.toByte(), -128), null),
        )
        // PDF 32000-1 §7.4.4.2 example (decimal bytes 45 45 45 45 45 65 45 45 45 66).
        assertContentEquals(
            byteArrayOf(45, 45, 45, 45, 45, 65, 45, 45, 45, 66),
            PdfFilters.decode("LZWDecode", unhex("800b6050220c0c8501"), null),
        )
        assertNull(PdfFilters.decode("DCTDecode", ByteArray(4), null))
    }

    @Test
    fun flateAppliesPngPredictor() {
        // Two rows of 3 bytes with the "Up" filter (2): second row adds the first.
        val raw = byteArrayOf(2, 1, 2, 3, 2, 1, 1, 1)
        val parms = PdfDict(mapOf("Predictor" to PdfNumber(12.0), "Columns" to PdfNumber(3.0)))
        assertContentEquals(byteArrayOf(1, 2, 3, 2, 3, 4), PdfFilters.decode("FlateDecode", storedZlib(raw), parms))
        val tiff = PdfDict(mapOf("Predictor" to PdfNumber(2.0), "Columns" to PdfNumber(3.0)))
        assertContentEquals(byteArrayOf(1, 2, 4), PdfFilters.decode("FlateDecode", storedZlib(byteArrayOf(1, 1, 2)), tiff))
    }

    @Test
    fun lexerReadsEveryObjectType() {
        val lexer = PdfLexer(latin1("<< /A#20B 1 /C [1 0 R -2.5 (a\\(b\\)\\n\\101) <48 49>] /D true /E null /F false >> % comment\n BT"))
        val dict = assertIs<PdfDict>(lexer.next())
        assertEquals(PdfNumber(1.0), dict["A B"])
        val array = assertIs<PdfArray>(dict["C"])
        assertEquals(PdfRef(1, 0), array.items[0])
        assertEquals(PdfNumber(-2.5), array.items[1])
        assertEquals("a(b)\nA", assertIs<PdfString>(array.items[2]).bytes.decodeToString())
        assertEquals("HI", assertIs<PdfString>(array.items[3]).bytes.decodeToString())
        assertEquals(PdfBool(true), dict["D"])
        assertEquals(PdfNull, dict["E"])
        assertEquals(PdfOperator("BT"), lexer.next())
        assertNull(lexer.next())
        assertEquals(PdfName("x"), PdfLexer(latin1("/x")).next())
    }
}

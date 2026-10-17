package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.pdf.Md5
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfText
import com.neteinstein.networth.feature.importer.domain.parse.pdf.Rc4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PdfTextTest {
    private fun texts(pdf: ByteArray): List<List<String>> = PdfText.lines(pdf).map { line -> line.cells.map { it.text } }

    @Test
    fun groupsRunsIntoLinesAndCells() {
        val pdf =
            PdfBuilder.simple(
                """
                BT /F1 10 Tf 50 700 Td (01.09.26) Tj 60 0 Td (Compra Padaria) Tj 300 0 Td (12,50) Tj ET
                BT /F1 10 Tf 50 680 Td (second) Tj ET
                """.trimIndent(),
            )
        assertEquals(listOf(listOf("01.09.26", "Compra Padaria", "12,50"), listOf("second")), texts(pdf))
        val first = PdfText.lines(pdf).first()
        assertEquals(700.0, first.y)
        assertEquals("01.09.26 Compra Padaria 12,50", first.text)
        assertEquals(50.0, first.cells[0].x)
        assertEquals(90.0, first.cells[0].endX)
    }

    @Test
    fun kerningJoinsLettersAndSeparatesWords() {
        val pdf =
            PdfBuilder.simple(
                """
                BT /F1 10 Tf 50 700 Td [(Hel) -20 (lo)] TJ ET
                BT /F1 10 Tf 50 650 Td [(Good) -400 (day)] TJ ET
                BT /F1 10 Tf 12 TL 50 600 Td (one) Tj T* (two) Tj (three) ' 1 0 (four) " ET
                BT /F1 10 Tf 2 Tc 1 Tw 50 Tz 3 Ts 50 500 TD (spaced out) Tj ET
                BT /F1 10 Tf 1 0 0 1 300 450 Tm (moved) Tj ET
                """.trimIndent(),
            )
        val lines = texts(pdf)
        assertEquals(listOf("Hello"), lines[0])
        assertEquals(listOf("Good day"), lines[1])
        assertEquals(listOf(listOf("one"), listOf("two"), listOf("three"), listOf("four")), lines.subList(2, 6))
        assertEquals(listOf("spaced out"), lines[6])
        assertEquals(300.0, PdfText.lines(pdf).last().cells.single().x)
    }

    @Test
    fun followsPageTreeInheritedResourcesAndContentArrays() {
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R 11 0 R] /Count 2 /Resources << /Font << /F1 5 0 R >> >> >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents [4 0 R 13 0 R] >>")
                .stream(4, "", latin1("BT /F1 10 Tf 50 700 Td (page one) Tj ET"))
                .stream(13, "", latin1("BT /F1 10 Tf 50 600 Td (more) Tj ET"))
                .obj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
                .obj(11, "<< /Type /Pages /Kids [12 0 R] /Count 1 >>")
                .obj(12, "<< /Type /Page /Parent 11 0 R /Contents 14 0 R >>")
                .stream(14, "", latin1("BT /F1 10 Tf 50 700 Td (page two) Tj ET"))
                .build()
        val lines = PdfText.lines(pdf)
        assertEquals(listOf(0 to "page one", 0 to "more", 1 to "page two"), lines.map { it.page to it.text })
    }

    @Test
    fun decodesType3GlyphNamesFromBankGenerators() {
        // novobanco names glyphs "C" + hex WinAnsi code; Banco Best uses the decimal code.
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R /Resources << /Font << /T3 5 0 R >> >> >>")
                .stream(4, "", latin1("BT /T3 10 Tf 50 700 Td <0102030405> Tj ET"))
                .obj(
                    5,
                    "<< /Type /Font /Subtype /Type3 /FontMatrix [0.001 0 0 0.001 0 0] /FontBBox [0 0 1000 1000] " +
                        "/FirstChar 1 /LastChar 5 /Widths [500 500 500 500 500] " +
                        "/Encoding << /Type /Encoding /Differences [1 /C41 /C2c /32 /aacute /uni20AC] >> /CharProcs << >> >>",
                )
                .build()
        assertEquals(listOf(listOf("A, á€")), texts(pdf))
    }

    @Test
    fun usesToUnicodeForTwoByteFonts() {
        val cmap =
            """
            /CIDInit /ProcSet findresource begin 12 dict begin begincmap
            1 begincodespacerange <0000> <FFFF> endcodespacerange
            2 beginbfchar <0001> <0048> <0002> <0069> endbfchar
            2 beginbfrange <0010> <0012> <0061> <0020> <0021> [<00E9> <20AC>] endbfrange
            endcmap
            """.trimIndent()
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R /Resources << /Font << /F2 5 0 R >> >> >>")
                .stream(4, "", latin1("BT /F2 10 Tf 50 700 Td <0001 0002 0010 0011 0012 0020 0021> Tj ET"))
                .obj(5, "<< /Type /Font /Subtype /Type0 /BaseFont /X /Encoding /Identity-H /DescendantFonts [6 0 R] /ToUnicode 7 0 R >>")
                .obj(6, "<< /Type /Font /Subtype /CIDFontType2 /W [1 [600 600] 16 18 500] /DW 1000 >>")
                .stream(7, "", latin1(cmap))
                .build()
        assertEquals(listOf(listOf("Hiabcé€")), texts(pdf))
    }

    @Test
    fun runsFormXObjectsSkipsRotatedTextAndInlineImages() {
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
                .obj(
                    3,
                    "<< /Type /Page /Parent 2 0 R /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> /XObject << /X1 6 0 R /Im 7 0 R >> >> >>",
                )
                .stream(
                    4,
                    "",
                    latin1(
                        "q 1 0 0 1 0 0 cm /X1 Do /Im Do Q BI /W 1 /H 1 /BPC 8 /CS /G ID \u0000\u0001 EI " +
                            "BT /F1 10 Tf 0 1 -1 0 300 300 Tm (vertical) Tj ET BT /F1 10 Tf 50 500 Td (after) Tj ET",
                    ),
                )
                .obj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
                .stream(
                    6,
                    "/Type /XObject /Subtype /Form /BBox [0 0 100 100] /Matrix [1 0 0 1 100 0]",
                    latin1("BT /F1 10 Tf 0 600 Td (inside form) Tj ET"),
                )
                .stream(7, "/Type /XObject /Subtype /Image /Width 1 /Height 1", ByteArray(1))
                .build()
        val lines = PdfText.lines(pdf)
        assertEquals(listOf("inside form", "after"), lines.map { it.text })
        assertEquals(100.0, lines.first().cells.single().x)
    }

    @Test
    fun readsCompressedObjectStreamsAndXrefStreamTrailers() {
        val packed = latin1("5 0 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 /Resources << /Font << /F1 5 0 R >> >> >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>")
                .stream(4, "/Filter /FlateDecode", storedZlib(latin1("BT /F1 10 Tf 50 700 Td (packed) Tj ET")))
                .stream(10, "/Type /ObjStm /N 1 /First 4 /Filter [/FlateDecode]", storedZlib(packed))
                .stream(20, "/Type /XRef /Root 1 0 R /Size 21", ByteArray(0))
                .build(trailer = null)
        assertEquals(listOf(listOf("packed")), texts(pdf))
    }

    @Test
    fun findsTheCatalogWithoutAnyTrailer() {
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 /Resources << /Font << /F1 5 0 R >> >> >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>")
                .stream(4, "", latin1("BT /F1 10 Tf 50 700 Td (found) Tj ET"))
                .obj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
                .build(trailer = null)
        assertEquals(listOf(listOf("found")), texts(pdf))
    }

    @Test
    fun decryptsRc4StatementsWithAnEmptyUserPassword() {
        val owner = ByteArray(32) { 0x11 }
        val id = ByteArray(16) { 0x22 }
        val padding =
            unhex("28bf4e5e4e758a4164004e56fffa01082e2e00b6d0683e802f0ca9fe6453697a")
        val p = -4
        var key = Md5.digest(
            padding + owner + byteArrayOf(p.toByte(), (p shr 8).toByte(), (p shr 16).toByte(), (p shr 24).toByte()) + id,
        ).copyOf(16)
        repeat(50) { key = Md5.digest(key).copyOf(16) }

        fun objectKey(number: Int) = Md5.digest(key + byteArrayOf(number.toByte(), 0, 0, 0, 0)).copyOf(16)
        val content = Rc4.apply(objectKey(4), latin1("BT /F1 10 Tf 50 700 Td (secret statement) Tj ET"))
        val title = Rc4.apply(objectKey(9), latin1("Extrato"))
        val pdf =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 /Resources << /Font << /F1 5 0 R >> >> >>")
                .obj(3, "<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>")
                .stream(4, "", content)
                .obj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
                .obj(8, "<< /Filter /Standard /V 2 /R 3 /Length 128 /P -4 /O <${hex(owner)}> /U <${hex(ByteArray(32))}> >>")
                .obj(9, "<< /Title <${hex(title)}> >>")
                .build("<< /Root 1 0 R /Encrypt 8 0 R /Info 9 0 R /ID [<${hex(id)}> <${hex(id)}>] >>")
        assertEquals(listOf(listOf("secret statement")), texts(pdf))
    }

    @Test
    fun rejectsWhatIsNotAReadablePdf() {
        assertFailsWith<ParseException> { PdfText.lines(latin1("hello, not a pdf")) }
        assertFailsWith<ParseException> { PdfText.lines(latin1("%PDF-1.4\nnothing here")) }
        val noPages = PdfBuilder().obj(1, "<< /Type /Catalog /Pages 2 0 R >>").obj(2, "<< /Type /Pages /Kids [] /Count 0 >>").build()
        assertFailsWith<ParseException> { PdfText.lines(noPages) }
        val unsupported =
            PdfBuilder()
                .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                .obj(8, "<< /Filter /Standard /V 5 /R 6 /O <00> /U <00> /P -4 >>")
                .build("<< /Root 1 0 R /Encrypt 8 0 R >>")
        val error = assertFailsWith<ParseException> { PdfText.lines(unsupported) }
        assertTrue(error.message!!.contains("AES-256"))
    }
}

package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import com.neteinstein.networth.feature.importer.domain.parse.Values
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileFormatsTest {
    private val parser = StatementParser()

    @Test
    fun parsesAmountsInEveryCommonNotation() {
        assertEquals(123_456, Values.amountMinor("1.234,56"))
        assertEquals(123_456, Values.amountMinor("1,234.56"))
        assertEquals(-1_230, Values.amountMinor("-12.30"))
        assertEquals(-500, Values.amountMinor("(5,00)"))
        assertEquals(-500, Values.amountMinor("5,00-"))
        assertEquals(310, Values.amountMinor("€ 3,10"))
        assertEquals(-2_000, Values.amountMinor("20,00 D"))
        assertEquals(2_000, Values.amountMinor("20,00 C"))
        assertEquals(1_250, Values.amountMinor("12.5"))
        assertEquals(123_400, Values.amountMinor("1.234"))
        assertEquals(1_200, Values.amountMinor("+12"))
        assertEquals(1_234, Values.amountMinor("12.3456"))
        assertNull(Values.amountMinor(""))
        assertNull(Values.amountMinor("abc"))
        assertTrue(Values.isAmount("1.234,56"))
        assertTrue(Values.isAmount("-0,15"))
        assertFalse(Values.isAmount("123456789"))
        assertFalse(Values.isAmount("Loja 1,77 Eur"))
    }

    @Test
    fun parsesDatesIbansAndNibs() {
        assertEquals(LocalDate(2026, 9, 2), Values.date("02.09.26"))
        assertEquals(LocalDate(2015, 10, 5), Values.date("05-10-15"))
        assertEquals(LocalDate(2026, 9, 2), Values.date("02/09/2026"))
        assertEquals(LocalDate(2026, 9, 2), Values.date("2026-09-02"))
        assertEquals(LocalDate(2026, 9, 2), Values.date("20260902120000[0:GMT]"))
        assertNull(Values.date("31.02.26"))
        assertNull(Values.date("13.13.26"))
        assertNull(Values.date("SALDO"))
        assertEquals("PT50000700000012345678901", Values.iban("iban: pt50 0007 0000 0012 3456 7890 1"))
        assertNull(Values.iban("no iban here"))
        assertEquals("PT50006500001111222233312", Values.ibanFromNib("0065.0000.11112222333.12"))
        assertNull(Values.ibanFromNib("123"))
    }

    @Test
    fun readsPortugueseCsvWithPreambleNewestFirst() {
        val csv =
            """
            Extrato de conta;;;;
            IBAN: PT50 0035 0000 0000 0000 0000 1;;;;
            ;;;;
            Data Mov.;Data Valor;Descrição;Montante;Saldo
            03-09-2026;03-09-2026;"COMPRA CONTINENTE; LISBOA";-45,30;954,70
            02-09-2026;02-09-2026;TRF DE EMPRESA;1.000,00;1.000,00
            """.trimIndent()
        val statement = parser.parse("extrato.csv", csv.encodeToByteArray())
        assertEquals(StatementFormat.CSV, statement.format)
        val section = statement.sections.single()
        assertEquals("PT50003500000000000000001", section.iban)
        assertEquals(listOf(100_000L, -4_530L), section.rows.map { it.amountMinor })
        assertEquals("COMPRA CONTINENTE; LISBOA", section.rows[1].description)
        assertEquals(0, section.openingBalanceMinor)
        assertEquals(95_470, section.closingBalanceMinor)
        assertEquals(LocalDate(2026, 9, 2), section.start)
        assertEquals(LocalDate(2026, 9, 3), section.end)
    }

    @Test
    fun readsDebitCreditColumnsAndWindows1252() {
        val csv = "Date,Details,Debit,Credit\r\n2026-09-01,Café \"Central\",3.20,\r\n2026-09-02,Salary,,2500.00\r\n"
        val bytes = csv.encodeToByteArray().toMutableList()
        // Re-encode "é" as Windows-1252 (0xE9) so the file isn't valid UTF-8.
        val utf8 = "é".encodeToByteArray()
        val index = (0 until bytes.size - 1).first { bytes[it] == utf8[0] && bytes[it + 1] == utf8[1] }
        bytes.removeAt(index + 1)
        bytes[index] = 0xE9.toByte()
        val section = parser.parse("export.csv", bytes.toByteArray()).sections.single()
        assertEquals(listOf(-320L, 250_000L), section.rows.map { it.amountMinor })
        assertEquals("Café \"Central\"", section.rows[0].description)
        assertNull(section.openingBalanceMinor)
    }

    @Test
    fun csvWithoutHeaderOrRowsIsRejected() {
        assertFailsWith<ParseException> { parser.parse("a.csv", "just,some,text\n1,2,3".encodeToByteArray()) }
        assertFailsWith<ParseException> { parser.parse("a.csv", "Data;Montante\nfoo;bar".encodeToByteArray()) }
        assertFailsWith<ParseException> { parser.parse("a.csv", ByteArray(0)) }
        assertFailsWith<ParseException> { parser.parse("old.xls", "anything".encodeToByteArray()) }
    }

    @Test
    fun readsXlsxWithSharedStringsAndDateCells() {
        val sheet =
            """
            <worksheet><sheetData>
            <row r="1"><c r="A1" t="s"><v>0</v></c></row>
            <row r="2"><c r="A2" t="s"><v>1</v></c><c r="B2" t="s"><v>2</v></c><c r="C2" t="inlineStr"><is><t>Valor</t></is></c><c r="D2" t="str"><v>Saldo</v></c></row>
            <row r="3"><c r="A3" s="1"><v>46268</v></c><c r="B3" t="s"><v>3</v></c><c r="C3"><v>-12.5</v></c><c r="D3"><v>987.5</v></c></row>
            <row r="4"><c r="A4" s="1"><v>46269</v></c><c r="C4"><v>0.1</v></c><c r="D4"><v>987.6</v></c></row>
            </sheetData></worksheet>
            """.trimIndent()
        val strings =
            "<sst><si><t>Conta PT50 0007 0000 0012 3456 7890 1</t></si><si><t>Data</t></si><si><r><t>Descri</t></r><r><t>ção</t></r></si>" +
                "<si><t>Pingo Doce &amp; Co</t></si></sst>"
        val styles = "<styleSheet><cellXfs count=\"2\"><xf numFmtId=\"0\"/><xf numFmtId=\"14\"/></cellXfs></styleSheet>"
        val file =
            zip(
                mapOf(
                    "[Content_Types].xml" to "<Types/>",
                    "xl/sharedStrings.xml" to strings,
                    "xl/styles.xml" to styles,
                    "xl/worksheets/sheet1.xml" to sheet,
                ),
            )
        val statement = parser.parse("movimentos.xlsx", file)
        assertEquals(StatementFormat.XLSX, statement.format)
        val section = statement.sections.single()
        assertEquals("PT50000700000012345678901", section.iban)
        assertEquals(LocalDate(2026, 9, 3), section.rows[0].date)
        assertEquals("Pingo Doce & Co", section.rows[0].description)
        assertEquals(listOf(-1_250L, 10L), section.rows.map { it.amountMinor })
        assertEquals(98_760, section.closingBalanceMinor)
        // Stored (method 0) entries work too.
        val stored =
            zip(mapOf("xl/worksheets/sheet1.xml" to sheet, "xl/sharedStrings.xml" to strings, "xl/styles.xml" to styles), deflate = false)
        assertEquals(2, parser.parse("m.xlsx", stored).rowCount)
        assertFailsWith<ParseException> { parser.parse("broken.xlsx", byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4, 5)) }
    }

    @Test
    fun readsOfxSgmlWithSeveralStatements() {
        val ofx =
            """
            OFXHEADER:100
            DATA:OFXSGML
            <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>EUR
            <BANKACCTFROM><BANKID>0007<ACCTID>PT50000700000012345678901<ACCTTYPE>SAVINGS</BANKACCTFROM>
            <BANKTRANLIST><DTSTART>20260901<DTEND>20260930
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260905120000[0:GMT]<TRNAMT>-10.50<FITID>A1<NAME>LIDL &amp; CIA<MEMO>LIDL &amp; CIA
            <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260903<TRNAMT>100<FITID>A2<MEMO>Transfer
            </BANKTRANLIST><LEDGERBAL><BALAMT>89.50<DTASOF>20260930</LEDGERBAL>
            </STMTRS></STMTTRNRS></BANKMSGSRSV1>
            <CREDITCARDMSGSRSV1><CCSTMTTRNRS><CCSTMTRS><CCACCTFROM><ACCTID>4111</CCACCTFROM>
            <BANKTRANLIST><STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260910<TRNAMT>-5.00<FITID>C1</STMTTRN></BANKTRANLIST>
            </CCSTMTRS></CCSTMTTRNRS></CREDITCARDMSGSRSV1></OFX>
            """.trimIndent()
        val statement = parser.parse("bank.ofx", ofx.encodeToByteArray())
        assertEquals(StatementFormat.OFX, statement.format)
        val (bank, card) = statement.sections
        assertEquals(AccountType.SAVINGS, bank.suggestedType)
        assertEquals("PT50000700000012345678901", bank.iban)
        assertEquals(listOf("A2", "A1"), bank.rows.map { it.externalId })
        assertEquals(listOf(10_000L, -1_050L), bank.rows.map { it.amountMinor })
        assertEquals("LIDL & CIA", bank.rows[1].description)
        assertEquals(8_950, bank.closingBalanceMinor)
        assertEquals(LocalDate(2026, 9, 1), bank.periodStart)
        assertEquals("Credit card", card.label)
        assertEquals("DEBIT", card.rows.single().description)
        assertFailsWith<ParseException> { parser.parse("empty.ofx", "<OFX></OFX>".encodeToByteArray()) }
    }

    @Test
    fun detectsFormatFromContentFirst() {
        assertEquals(StatementFormat.PDF, parser.formatOf("statement.csv", "%PDF-1.4".encodeToByteArray()))
        assertEquals(StatementFormat.XLSX, parser.formatOf("x.bin", byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4, 0)))
        assertEquals(StatementFormat.OFX, parser.formatOf("x.qfx", "garbage".encodeToByteArray()))
        assertEquals(StatementFormat.CSV, parser.formatOf("x.txt", "a;b".encodeToByteArray()))
        assertEquals("é", parser.text(byteArrayOf(0xE9.toByte())))
        assertEquals("€", parser.text(byteArrayOf(0x80.toByte())))
        assertEquals("ok", parser.text(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), 'o'.code.toByte(), 'k'.code.toByte())))
    }

    @Test
    fun parsesSyntheticPdfStatementEndToEnd() {
        val content =
            """
            BT /F1 8 Tf 30 800 Td (Data) Tj 80 0 Td (Descritivo) Tj 270 0 Td (Débito) Tj 70 0 Td (Crédito) Tj 70 0 Td (Saldo) Tj ET
            BT /F1 8 Tf 30 780 Td (SALDO ANTERIOR) Tj 490 0 Td (100,00) Tj ET
            BT /F1 8 Tf 30 760 Td (02.09.26) Tj 80 0 Td (Compra Padaria) Tj 270 0 Td (2,50) Tj 140 0 Td (97,50) Tj ET
            BT /F1 8 Tf 30 740 Td (SALDO CONTABILÍSTICO) Tj 490 0 Td (97,50) Tj ET
            """.trimIndent()
        val statement = parser.parse("statement.pdf", PdfBuilder.simple(content))
        assertEquals(StatementFormat.PDF, statement.format)
        assertEquals(listOf(-250L), statement.sections.single().rows.map { it.amountMinor })
    }
}

package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import com.neteinstein.networth.feature.importer.domain.parse.StatementParser
import com.neteinstein.networth.feature.importer.domain.parse.bank.PdfStatements
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One example PDF per bank layout, written from [BankStatementFixtures] with every cell where the fixture puts it,
 * read back through the whole pipeline (file → PDF text → bank profile) like a picked file.
 */
class BankPdfExamplesTest {
    private fun importPdf(
        name: String,
        lines: List<PdfTextLine>,
    ) = StatementParser().parse(name, examplePdf(lines))

    private fun assertReadsLikeTheLayout(
        name: String,
        lines: List<PdfTextLine>,
    ) {
        val statement = importPdf(name, lines)
        assertEquals(StatementFormat.PDF, statement.format)
        assertEquals(PdfStatements.parse(name, lines), statement)
        assertTrue(statement.sections.any { it.rows.isNotEmpty() }, "$name has movements")
    }

    @Test
    fun novobancoExtratoIntegrado() = assertReadsLikeTheLayout("novobanco.pdf", BankStatementFixtures.novobanco)

    @Test
    fun bancoBestExtratoIntegrado() = assertReadsLikeTheLayout("best.pdf", BankStatementFixtures.best)

    @Test
    fun activoBankExtratoCombinado() = assertReadsLikeTheLayout("activobank.pdf", BankStatementFixtures.activoBank)

    @Test
    fun activoBankAcrossTheNewYear() = assertReadsLikeTheLayout("activobank-2020.pdf", BankStatementFixtures.activoBankNewYear)

    @Test
    fun millenniumExtratoCombinado() = assertReadsLikeTheLayout("millennium.pdf", BankStatementFixtures.millennium)

    @Test
    fun santanderExtrato() = assertReadsLikeTheLayout("santander.pdf", BankStatementFixtures.santander)

    @Test
    fun bpiExtractoDeConta() = assertReadsLikeTheLayout("bpi.pdf", BankStatementFixtures.bpi)

    @Test
    fun montepioExtrato() = assertReadsLikeTheLayout("montepio.pdf", BankStatementFixtures.montepio)

    @Test
    fun montepioOnlineMovements() = assertReadsLikeTheLayout("net24.pdf", BankStatementFixtures.montepioOnline)

    @Test
    fun cgdExtracto() = assertReadsLikeTheLayout("cgd.pdf", BankStatementFixtures.cgd)

    @Test
    fun creditoAgricolaOnlineMovements() = assertReadsLikeTheLayout("ca.pdf", BankStatementFixtures.creditoAgricola)

    @Test
    fun novobancoBusinessExtrato() = assertReadsLikeTheLayout("empresa.pdf", BankStatementFixtures.novobancoBusiness)

    @Test
    fun otherBanksFallBackToTheGenericLayout() = assertReadsLikeTheLayout("other.pdf", BankStatementFixtures.generic)

    @Test
    fun eachBankGetsItsOwnProfile() {
        assertEquals("novobanco", importPdf("a.pdf", BankStatementFixtures.novobanco).bank)
        assertEquals("Banco Best", importPdf("b.pdf", BankStatementFixtures.best).bank)
        assertEquals("ActivoBank", importPdf("c.pdf", BankStatementFixtures.activoBank).bank)
        assertEquals("Millennium bcp", importPdf("e.pdf", BankStatementFixtures.millennium).bank)
        assertEquals("Santander", importPdf("f.pdf", BankStatementFixtures.santander).bank)
        assertEquals("BPI", importPdf("g.pdf", BankStatementFixtures.bpi).bank)
        assertEquals("Montepio", importPdf("h.pdf", BankStatementFixtures.montepio).bank)
        assertEquals("CGD", importPdf("i.pdf", BankStatementFixtures.cgd).bank)
        assertEquals("Crédito Agrícola", importPdf("j.pdf", BankStatementFixtures.creditoAgricola).bank)
        assertEquals(null, importPdf("d.pdf", BankStatementFixtures.generic).bank)
    }

    private companion object {
        /** 8pt Helvetica: about 4pt per character, so the fixtures' column gaps stay visible gaps. */
        const val FONT_SIZE = 8

        /** A PDF with one page per fixture page and each cell drawn as its own text object (WinAnsi). */
        fun examplePdf(lines: List<PdfTextLine>): ByteArray {
            val pages = lines.groupBy { it.page }.toSortedMap().values.toList()
            val firstPage = 4
            val kids = pages.indices.joinToString(" ") { "${firstPage + it * 2} 0 R" }
            val builder =
                PdfBuilder()
                    .obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
                    .obj(2, "<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>")
                    .obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
            pages.forEachIndexed { i, page ->
                val content =
                    page.joinToString("\n") { line ->
                        line.cells.joinToString("\n") { cell ->
                            "BT /F1 $FONT_SIZE Tf ${cell.x} ${line.y} Td (${escape(cell.text)}) Tj ET"
                        }
                    }
                builder
                    .obj(
                        firstPage + i * 2,
                        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> " +
                            "/Contents ${firstPage + i * 2 + 1} 0 R >>",
                    ).stream(firstPage + i * 2 + 1, "", latin1(content))
            }
            return builder.build()
        }

        private fun escape(text: String): String = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    }
}

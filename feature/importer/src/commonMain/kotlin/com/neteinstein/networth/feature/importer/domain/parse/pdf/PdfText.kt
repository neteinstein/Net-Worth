package com.neteinstein.networth.feature.importer.domain.parse.pdf

import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import kotlin.math.abs
import kotlin.math.sqrt

/** One line of a page as a reader sees it: cells left to right, separated by visible gaps. */
data class PdfTextLine(
    val page: Int,
    val y: Double,
    val cells: List<PdfTextCell>,
) {
    val text: String get() = cells.joinToString(" ") { it.text }
}

data class PdfTextCell(
    val x: Double,
    val endX: Double,
    val text: String,
)

/**
 * Extracts positioned text from a PDF: runs every page's content stream (and form XObjects),
 * tracks the text and graphics matrices, then groups the shown strings into lines by baseline.
 */
object PdfText {
    fun lines(data: ByteArray): List<PdfTextLine> {
        val doc = PdfDocument.open(data)
        val pages = doc.pages()
        if (pages.isEmpty()) throw ParseException("This PDF has no pages")
        return pages.flatMapIndexed { index, page ->
            val runs = ArrayList<Run>()
            Interpreter(doc, runs).run(doc.contents(page), page.resources, IDENTITY, depth = 0)
            assemble(index, runs)
        }
    }

    internal class Run(
        val x: Double,
        val y: Double,
        val endX: Double,
        val size: Double,
        val text: String,
    )

    private val IDENTITY = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    private fun multiply(
        a: DoubleArray,
        b: DoubleArray,
    ): DoubleArray =
        doubleArrayOf(
            a[0] * b[0] + a[1] * b[2],
            a[0] * b[1] + a[1] * b[3],
            a[2] * b[0] + a[3] * b[2],
            a[2] * b[1] + a[3] * b[3],
            a[4] * b[0] + a[5] * b[2] + b[4],
            a[4] * b[1] + a[5] * b[3] + b[5],
        )

    private class Interpreter(
        private val doc: PdfDocument,
        private val out: MutableList<Run>,
    ) {
        private val fonts = HashMap<Any, PdfFont>()

        private class State(
            var ctm: DoubleArray,
            var font: PdfFont = PdfFont.FALLBACK,
            var fontSize: Double = 1.0,
            var charSpacing: Double = 0.0,
            var wordSpacing: Double = 0.0,
            var hScale: Double = 1.0,
            var leading: Double = 0.0,
            var rise: Double = 0.0,
        ) {
            fun copy() = State(ctm, font, fontSize, charSpacing, wordSpacing, hScale, leading, rise)
        }

        fun run(
            content: ByteArray,
            resources: PdfDict,
            ctm: DoubleArray,
            depth: Int,
        ) {
            val lexer = PdfLexer(content)
            val operands = ArrayList<PdfObject>()
            val stack = ArrayList<State>()
            var state = State(ctm)
            var tm = IDENTITY
            var tlm = IDENTITY

            fun nums(): List<Double> = operands.map { (it as? PdfNumber)?.value ?: 0.0 }

            fun moveLine(
                tx: Double,
                ty: Double,
            ) {
                tlm = multiply(doubleArrayOf(1.0, 0.0, 0.0, 1.0, tx, ty), tlm)
                tm = tlm
            }

            fun show(bytes: ByteArray) {
                tm = show(state, tm, bytes)
            }

            while (true) {
                val obj = lexer.next() ?: break
                if (obj !is PdfOperator) {
                    operands += obj
                    continue
                }
                val n = nums()
                when (obj.name) {
                    "q" -> stack += state.copy()
                    "Q" -> if (stack.isNotEmpty()) state = stack.removeAt(stack.lastIndex)
                    "cm" -> if (n.size == 6) state.ctm = multiply(n.toDoubleArray(), state.ctm)
                    "BT" -> {
                        tm = IDENTITY
                        tlm = IDENTITY
                    }
                    "Tf" -> {
                        val name = (operands.getOrNull(0) as? PdfName)?.name
                        state.font = name?.let { font(resources, it) } ?: PdfFont.FALLBACK
                        state.fontSize = n.getOrNull(1) ?: 1.0
                    }
                    "Tc" -> state.charSpacing = n.firstOrNull() ?: 0.0
                    "Tw" -> state.wordSpacing = n.firstOrNull() ?: 0.0
                    "Tz" -> state.hScale = (n.firstOrNull() ?: 100.0) / 100.0
                    "TL" -> state.leading = n.firstOrNull() ?: 0.0
                    "Ts" -> state.rise = n.firstOrNull() ?: 0.0
                    "Td" -> if (n.size == 2) moveLine(n[0], n[1])
                    "TD" ->
                        if (n.size == 2) {
                            state.leading = -n[1]
                            moveLine(n[0], n[1])
                        }
                    "Tm" ->
                        if (n.size == 6) {
                            tlm = n.toDoubleArray()
                            tm = tlm
                        }
                    "T*" -> moveLine(0.0, -state.leading)
                    "Tj" -> (operands.lastOrNull() as? PdfString)?.let { show(it.bytes) }
                    "'" -> {
                        moveLine(0.0, -state.leading)
                        (operands.lastOrNull() as? PdfString)?.let { show(it.bytes) }
                    }
                    "\"" -> {
                        if (n.size >= 2) {
                            state.wordSpacing = n[0]
                            state.charSpacing = n[1]
                        }
                        moveLine(0.0, -state.leading)
                        (operands.lastOrNull() as? PdfString)?.let { show(it.bytes) }
                    }
                    "TJ" ->
                        (operands.lastOrNull() as? PdfArray)?.items?.forEach { item ->
                            when (item) {
                                is PdfString -> show(item.bytes)
                                is PdfNumber -> {
                                    val tx = -item.value / 1000.0 * state.fontSize * state.hScale
                                    tm = multiply(doubleArrayOf(1.0, 0.0, 0.0, 1.0, tx, 0.0), tm)
                                }
                                else -> Unit
                            }
                        }
                    "Do" -> (operands.lastOrNull() as? PdfName)?.let { form(resources, it.name, state.ctm, depth) }
                    "BI" -> lexer.skipInlineImage()
                }
                operands.clear()
            }
        }

        private fun show(
            state: State,
            tm: DoubleArray,
            bytes: ByteArray,
        ): DoubleArray {
            val font = state.font
            val glyphs = font.decode(bytes)
            var advance = 0.0
            val text = StringBuilder()
            for (g in glyphs) {
                text.append(g.text)
                val w = g.width * font.widthScale * state.fontSize
                advance += (w + state.charSpacing + if (g.isSpace) state.wordSpacing else 0.0) * state.hScale
            }
            val trm = multiply(tm, state.ctm)
            val x = trm[4] + trm[2] * state.rise
            val y = trm[5] + trm[3] * state.rise
            val scaleX = sqrt(trm[0] * trm[0] + trm[1] * trm[1])
            val scaleY = sqrt(trm[2] * trm[2] + trm[3] * trm[3])
            val size = abs(state.fontSize * font.heightScale * scaleY)
            val horizontal = abs(trm[1]) <= abs(trm[0]) * 0.1
            if (text.isNotEmpty() && horizontal) out += Run(x, y, x + advance * scaleX, size, text.toString())
            return multiply(doubleArrayOf(1.0, 0.0, 0.0, 1.0, advance, 0.0), tm)
        }

        private fun font(
            resources: PdfDict,
            name: String,
        ): PdfFont {
            val fontDict = doc.resolve(resources["Font"]) as? PdfDict ?: return PdfFont.FALLBACK
            val ref = fontDict[name] ?: return PdfFont.FALLBACK
            val key: Any = (ref as? PdfRef)?.number ?: ref
            return fonts.getOrPut(key) {
                (doc.resolve(ref) as? PdfDict)?.let { PdfFont.load(doc, it) } ?: PdfFont.FALLBACK
            }
        }

        private fun form(
            resources: PdfDict,
            name: String,
            ctm: DoubleArray,
            depth: Int,
        ) {
            if (depth > 8) return
            val xObjects = doc.resolve(resources["XObject"]) as? PdfDict ?: return
            val stream = doc.resolve(xObjects[name]) as? PdfStream ?: return
            if (stream.dict.name("Subtype") != "Form") return
            val matrix =
                (doc.resolve(stream.dict["Matrix"]) as? PdfArray)?.items
                    ?.map { (doc.resolve(it) as? PdfNumber)?.value ?: 0.0 }
                    ?.takeIf { it.size == 6 }
                    ?.toDoubleArray() ?: IDENTITY
            val formResources = doc.resolve(stream.dict["Resources"]) as? PdfDict ?: resources
            run(doc.streamData(stream), formResources, multiply(matrix, ctm), depth + 1)
        }
    }

    /** Groups runs into lines (same baseline within a third of the font size) and cells (gaps wider than a space). */
    internal fun assemble(
        page: Int,
        runs: List<Run>,
    ): List<PdfTextLine> {
        val visible = runs.filter { it.text.isNotBlank() }
        val sorted = visible.sortedWith(compareByDescending<Run> { it.y }.thenBy { it.x })
        val lines = ArrayList<MutableList<Run>>()
        for (run in sorted) {
            val line = lines.lastOrNull()
            val tolerance = maxOf(1.0, run.size * 0.35)
            if (line != null && abs(line.first().y - run.y) <= tolerance) line += run else lines += mutableListOf(run)
        }
        return lines.map { line ->
            val ordered = line.sortedBy { it.x }
            val cells = ArrayList<PdfTextCell>()
            var current: StringBuilder? = null
            var start = 0.0
            var end = 0.0
            for (run in ordered) {
                val gap = run.x - end
                val avgChar = (run.endX - run.x) / maxOf(1, run.text.length)
                val spaceWidth = maxOf(avgChar * 0.6, run.size * 0.15, 0.5)
                val builder = current
                if (builder != null && gap <= spaceWidth * 2.2) {
                    if (gap > spaceWidth * 0.5 && !builder.endsWith(" ") && !run.text.startsWith(" ")) builder.append(' ')
                    builder.append(run.text)
                    end = maxOf(end, run.endX)
                } else {
                    if (builder != null) cells += PdfTextCell(start, end, builder.toString().trim().replace(spaces, " "))
                    current = StringBuilder(run.text)
                    start = run.x
                    end = run.endX
                }
            }
            current?.let { cells += PdfTextCell(start, end, it.toString().trim().replace(spaces, " ")) }
            PdfTextLine(page, ordered.first().y, cells.filter { it.text.isNotEmpty() })
        }.filter { it.cells.isNotEmpty() }
    }

    private val spaces = Regex("\\s+")
}

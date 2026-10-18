package com.neteinstein.networth.feature.importer.domain.parse.bank

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.model.ParsedRow
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.parse.Values
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextCell
import com.neteinstein.networth.feature.importer.domain.parse.pdf.PdfTextLine
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Reads the movements table of one account from PDF lines, the way bank statements lay it out:
 * `Date | Value date | Description | Debit | Credit | Balance`.
 *
 * - A row starts with a date. Its trailing amount cells are the movement and (if present) the
 *   running balance; amounts inside the description ("Loja 13,44 Eur") stay in the description
 *   because they are part of that cell.
 * - Lines without a date that start in the description column continue the previous row's
 *   description (long descriptions wrap).
 * - The sign comes from the change in running balance, starting at the opening balance. Without
 *   a balance it comes from whether the amount sits under the Debit or the Credit header.
 */
internal class MovementTable(
    private val label: String,
    private val accountNumber: String?,
    private val type: AccountType,
    /** Settable: some statements print the period after the account's heading. */
    var periodStart: LocalDate?,
    var periodEnd: LocalDate?,
    /** Settable: some banks print the IBAN under the account's heading. */
    var iban: String? = null,
) {
    private class Draft(
        val date: LocalDate,
        val valueDate: LocalDate?,
        val description: StringBuilder,
        val amountMinor: Long,
        val amountX: Double,
        val balanceMinor: Long?,
        val explicitSign: Boolean,
        /** Where the description column starts on this row's page (margins differ between pages). */
        val descriptionX: Double,
    )

    private val rows = ArrayList<Draft>()
    private var opening: Long? = null
    private var closing: Long? = null
    private var debitX: Double? = null
    private var creditX: Double? = null
    private var continuing = false

    /** True once a row carries its own sign: then the amounts are one signed column and unsigned ones are credits. */
    private var signedColumn = false
    var closed = false
        private set

    /** Feeds one line; returns false if the line is not part of a movements table. */
    fun accept(raw: PdfTextLine): Boolean {
        val line = raw.copy(cells = normalise(raw.cells))
        val cells = line.cells
        val text = line.text
        if (isHeader(line)) {
            cells.firstOrNull { it.text.startsWith("Débit", true) || it.text.startsWith("Debit", true) }?.let {
                debitX =
                    (it.x + it.endX) / 2
            }
            cells.firstOrNull { it.text.startsWith("Crédit", true) || it.text.startsWith("Credit", true) }?.let {
                creditX =
                    (it.x + it.endX) / 2
            }
            continuing = false
            return true
        }
        val lastAmount = cells.lastOrNull()?.takeIf { Values.isAmount(it.text) }?.let { Values.amountMinor(it.text) }
        if (OPENING.containsMatchIn(text) && lastAmount != null) {
            opening = lastAmount
            continuing = false
            return true
        }
        if (CLOSING.containsMatchIn(text) && lastAmount != null) {
            closing = lastAmount
            closed = true
            continuing = false
            return true
        }
        if (cells.size >= 2 && Values.isDate(cells[0].text)) {
            val row = row(line)
            if (row != null) {
                rows += row
                if (row.explicitSign) signedColumn = true
                continuing = true
                return true
            }
        }
        val x = rows.lastOrNull()?.descriptionX
        if (continuing &&
            x != null &&
            cells.none { Values.isAmount(it.text) } &&
            abs(cells.first().x - x) <= 4.0 &&
            !Values.isDate(cells.first().text)
        ) {
            val description = rows.last().description
            if (!description.endsWith("-")) description.append(' ')
            description.append(line.text)
            return true
        }
        continuing = false
        return false
    }

    /**
     * Drops a trailing currency column ("EUR") and empty "-" cells, and folds a Débito/Crédito marker cell ("D", "C") into the amount
     * just before it, so `36,90 EUR | D | 1.343,90 EUR` reads as `-36,90 EUR | 1.343,90 EUR`.
     */
    private fun normalise(cells: List<PdfTextCell>): List<PdfTextCell> {
        val out = ArrayList<PdfTextCell>(cells.size)
        for (cell in cells) {
            val marker = cell.text.trim().uppercase()
            if (marker == "-") continue
            val previous = out.lastOrNull()
            if (marker in SIGN_MARKERS && previous != null && Values.isAmount(previous.text)) {
                val amount = previous.text.trim()
                val text = if (marker.startsWith("D") && !amount.startsWith("-")) "-$amount" else amount
                out[out.lastIndex] = previous.copy(text = text, endX = cell.endX)
                continue
            }
            out += cell
        }
        while (out.size > 1 && out.last().text.trim().uppercase() in CURRENCIES) out.removeAt(out.lastIndex)
        return out
    }

    private fun isHeader(line: PdfTextLine): Boolean {
        val lower = line.text.lowercase()
        return (lower.contains("descritivo") || lower.contains("descrição") || lower.contains("description")) &&
            (lower.contains("débit") || lower.contains("debit") || lower.contains("saldo") || lower.contains("balance"))
    }

    private fun row(line: PdfTextLine): Draft? {
        val cells = line.cells
        val date = Values.date(cells[0].text) ?: return null
        var index = 1
        val valueDate = Values.date(cells[1].text)
        if (valueDate != null) index = 2
        val amounts = cells.drop(index).takeLastWhile { Values.isAmount(it.text) }
        if (amounts.isEmpty()) return null
        val descriptionCells = cells.subList(index, cells.size - amounts.size)
        if (descriptionCells.isEmpty()) return null
        val movement = if (amounts.size >= 2) amounts[amounts.size - 2] else amounts.last()
        val balance = if (amounts.size >= 2) Values.amountMinor(amounts.last().text) else null
        val amount = Values.amountMinor(movement.text) ?: return null
        return Draft(
            date = date,
            valueDate = valueDate,
            description = StringBuilder(descriptionCells.joinToString(" ") { it.text }),
            amountMinor = amount,
            amountX = (movement.x + movement.endX) / 2,
            balanceMinor = balance,
            explicitSign = movement.text.trim().let { it.startsWith("-") || it.startsWith("−") || it.endsWith("-") },
            descriptionX = descriptionCells.first().x,
        )
    }

    fun build(): ParsedSection {
        var previous = opening
        val parsed =
            rows.map { draft ->
                val magnitude = abs(draft.amountMinor)
                val signed =
                    when {
                        draft.explicitSign -> draft.amountMinor
                        draft.balanceMinor != null && previous != null && abs(draft.balanceMinor - previous) == magnitude ->
                            if (draft.balanceMinor < previous) -magnitude else magnitude
                        debitX != null && creditX != null ->
                            if (abs(draft.amountX - debitX!!) <= abs(draft.amountX - creditX!!)) -magnitude else magnitude
                        signedColumn -> magnitude
                        else -> -magnitude
                    }
                previous = draft.balanceMinor ?: previous?.plus(signed)
                ParsedRow(
                    date = draft.date,
                    valueDate = draft.valueDate,
                    description = draft.description.toString().replace(SPACES, " ").trim(),
                    amountMinor = signed,
                    balanceMinor = draft.balanceMinor,
                )
            }
        return ParsedSection(
            label = label,
            accountNumber = accountNumber,
            iban = iban,
            suggestedType = type,
            periodStart = periodStart,
            periodEnd = periodEnd,
            openingBalanceMinor = opening,
            closingBalanceMinor = closing ?: parsed.lastOrNull { it.balanceMinor != null }?.balanceMinor,
            rows = parsed,
        )
    }

    companion object {
        private val OPENING = Regex("saldo\\s+(anterior|inicial)|opening balance|previous balance", RegexOption.IGNORE_CASE)
        private val CLOSING =
            Regex("saldo\\s+(contabil[ií]stico|final|a(c)?tual\\s+contabil)|closing balance", RegexOption.IGNORE_CASE)
        private val CURRENCIES = setOf("EUR", "USD", "GBP", "CHF", "BRL")
        private val SIGN_MARKERS = setOf("D", "C", "DB", "CR")
        private val SPACES = Regex("\\s+")
    }
}

/** Guesses an account type from a bank's product name. */
internal fun accountTypeFor(label: String): AccountType {
    val l = label.lowercase()
    return when {
        "habita" in l || "hipot" in l || "mortgage" in l -> AccountType.MORTGAGE
        "crédito" in l || "credito" in l || "empréstimo" in l || "loan" in l -> AccountType.LOAN
        "prazo" in l || "deposito a medida" in l || "depósito a medida" in l || "dp " in "$l " || "term" in l -> AccountType.TERM_DEPOSIT
        "fundo" in l || "invest" in l || "carteira" in l -> AccountType.INVESTMENT
        "poupan" in l || "saving" in l -> AccountType.SAVINGS
        else -> AccountType.CURRENT
    }
}

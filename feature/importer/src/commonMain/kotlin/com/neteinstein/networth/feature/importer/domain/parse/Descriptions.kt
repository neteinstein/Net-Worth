package com.neteinstein.networth.feature.importer.domain.parse

/** What kind of bank movement a description is, as far as the text tells. */
enum class MovementKind {
    CARD_PURCHASE,

    /** "Pagamento Baixo Valor": contactless payments the bank batches without a merchant name. */
    LOW_VALUE_CARD,
    MB_WAY_PERSON,
    TRANSFER_IN,
    TRANSFER_OUT,
    DIRECT_DEBIT,
    SERVICE_PAYMENT,
    CASH_WITHDRAWAL,
    REFUND,
    MORTGAGE,
    FEE,
    TAX,
    INTEREST,
    OTHER,
}

data class DescriptionInfo(
    val kind: MovementKind,
    /** The shop, creditor or payee, cleaned up ("Pingo Doce Lisboa"), or null when the bank doesn't say. */
    val merchant: String?,
    /** The other person or company in a transfer. */
    val counterparty: String? = null,
    /** A masked phone number ("9** *** 412") for MB WAY transfers that show one. */
    val phone: String? = null,
) {
    /**
     * Uppercase key used to learn rules and find past choices; null when there's nothing to learn
     * from. Merchants are keyed by their distinctive first word(s); people by phone or full name.
     */
    val key: String?
        get() = phone ?: merchant?.let { Descriptions.keyOf(it.uppercase()) } ?: counterparty?.uppercase()
}

/**
 * Normalises Portuguese bank descriptions (novobanco, Best, and the conventions most PT banks
 * share): strips card numbers, references and postcodes, and recognises the common movement kinds.
 */
object Descriptions {
    private val ignore = RegexOption.IGNORE_CASE
    private val card = Regex("cart[aã]o\\s*[*\\d\\s]*\\d\\*?|cart[aã]o\\s*(?:\\*+\\s*)+\\d*\\*?", ignore)
    private val amountInText = Regex("\\b\\d+[.,]\\d{2}\\s*eur\\b", ignore)
    private val postcode = Regex("\\b\\d{4}-\\d{3}\\b")
    private val reference = Regex("\\b(?=[A-Za-z]*\\d)[A-Za-z0-9.]{6,}\\b")
    private val invoice = Regex("invoicenr\\.?\\s*\\d+", ignore)
    private val phonePattern = Regex("(9[\\d*]{2}\\s?[\\d*]{3}\\s?[\\d*]{3})")
    private val spaces = Regex("\\s+")

    private val lowValue = Regex("^pagamento baixo valor", ignore)
    private val cash = Regex("^levantamento", ignore)
    private val refund = Regex("^devolu[cç][aã]o\\s+(?:compra\\s+)?(.*)$", ignore)
    private val purchase = Regex("^(?:compra|transa[cç][aã]o visa)\\s*(?:mb\\s*way|mbway|mb|internet|visa)?\\s*(.*)$", ignore)
    private val mbWay =
        Regex(
            "(?:trf|transf\\w*)\\s+imediata\\s+c/\\s*cart[aã]o\\s*\\(mbway\\)-?\\s*app\\s+terceiros\\s+p/\\s*(.*)$|^mb\\s*way\\s+p/\\s*(.*)$",
            ignore,
        )
    private val transferIn =
        Regex(
            "^(?:trf|transf\\.?\\w*)\\s+(?:cred\\s+|imediata\\s+)?(?:sepa\\+?\\s+)?(?:interbancaria\\s+sepa\\s+)?de\\s+(.*)$|^transf\\.interbancaria sepa de\\s+(.*)$",
            ignore,
        )
    private val transferOut = Regex("^(?:trf|transf\\w*|transfer[eê]ncia)\\b.*?\\bp/\\s*(.*)$|^p/\\s*(.*?)(?:\\s+app\\s+\\d+)?$", ignore)
    private val directDebit = Regex("^cobran[cç]a\\s+sdd\\s+(.*?)(?:\\s+adc\\b.*)?$", ignore)
    private val service = Regex("^pag(?:amento)?\\s+serv(?:i[cç]os?)?\\s+(.*?)(?:\\s+app\\b.*)?$", ignore)
    private val mortgage = Regex("presta[cç][aã]o\\s+ch\\b|cr[eé]dito habita", ignore)
    private val tax = Regex("^(?:imp\\.?\\s+selo|imposto)", ignore)
    private val fee = Regex("^(?:com\\.?\\s|comiss[aã]o|manuten[cç][aã]o)", ignore)
    private val interest = Regex("^juros", ignore)

    fun describe(raw: String): DescriptionInfo {
        val text = raw.replace(spaces, " ").trim()
        return when {
            lowValue.containsMatchIn(text) -> DescriptionInfo(MovementKind.LOW_VALUE_CARD, null)
            cash.containsMatchIn(text) -> DescriptionInfo(MovementKind.CASH_WITHDRAWAL, null)
            mortgage.containsMatchIn(text) -> DescriptionInfo(MovementKind.MORTGAGE, null)
            tax.containsMatchIn(text) -> DescriptionInfo(MovementKind.TAX, null)
            fee.containsMatchIn(text) -> DescriptionInfo(MovementKind.FEE, null)
            interest.containsMatchIn(text) -> DescriptionInfo(MovementKind.INTEREST, null)
            else -> parties(text)
        }
    }

    private fun parties(text: String): DescriptionInfo {
        refund.find(text)?.let { return DescriptionInfo(MovementKind.REFUND, clean(it.groupValues[1])) }
        mbWay.find(text)?.let { m ->
            val who = (m.groupValues[1].ifBlank { m.groupValues[2] }).trim()
            val phone = phonePattern.find(who)?.groupValues?.get(1)?.replace(spaces, " ")
            return DescriptionInfo(
                MovementKind.MB_WAY_PERSON,
                null,
                counterparty = if (phone !=
                    null
                ) {
                    null
                } else {
                    cleanName(who)
                },
                phone = phone,
            )
        }
        directDebit.find(text)?.let { return DescriptionInfo(MovementKind.DIRECT_DEBIT, clean(it.groupValues[1].trimEnd(','))) }
        service.find(text)?.let { return DescriptionInfo(MovementKind.SERVICE_PAYMENT, clean(it.groupValues[1])) }
        transferIn.find(text)?.let { m ->
            return DescriptionInfo(MovementKind.TRANSFER_IN, null, counterparty = cleanName(m.groupValues[1].ifBlank { m.groupValues[2] }))
        }
        transferOut.find(text)?.let { m ->
            return DescriptionInfo(MovementKind.TRANSFER_OUT, null, counterparty = cleanName(m.groupValues[1].ifBlank { m.groupValues[2] }))
        }
        purchase.find(text)?.let { return DescriptionInfo(MovementKind.CARD_PURCHASE, clean(it.groupValues[1])) }
        return DescriptionInfo(MovementKind.OTHER, clean(text))
    }

    /** Removes card numbers, amounts, references and postcodes; title-cases what's left. */
    fun clean(text: String): String? {
        val stripped =
            text.replace(card, " ").replace(amountInText, " ").replace(invoice, " ").replace(postcode, " ")
                .replace(reference, " ").replace(Regex("\\b\\d+\\b"), " ").replace(Regex("[*]+"), " ")
                .replace(spaces, " ").trim().trim('-', ',', '.', ' ')
        return stripped.takeIf { it.length >= 2 }?.let(::titleCase)
    }

    private fun cleanName(text: String): String? =
        text.replace(Regex("\\b(app|online)\\b.*$", ignore), " ").replace(Regex("\\b\\d+\\b"), " ")
            .replace(spaces, " ").trim().takeIf { it.length >= 2 }?.let(::titleCase)

    private fun titleCase(text: String): String =
        text.lowercase().split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    /**
     * The words a learned rule matches on: the first word when it's distinctive (5+ letters,
     * e.g. "DECATHLON"), otherwise the first two ("PINGO DOCE", "H3 NORTE").
     */
    fun keyOf(upper: String): String {
        val words = upper.split(' ').map { it.trim(',', '.', ';', ':', '-') }.filter { it.isNotBlank() }
        if (words.isEmpty()) return upper
        return if (words[0].length >= 5 || words.size == 1) words[0] else words.take(2).joinToString(" ")
    }
}

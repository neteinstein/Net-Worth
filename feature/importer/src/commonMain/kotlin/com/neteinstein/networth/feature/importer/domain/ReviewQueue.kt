package com.neteinstein.networth.feature.importer.domain

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.text.DescriptionInfo
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import com.neteinstein.networth.core.domain.text.Rules
import com.neteinstein.networth.feature.importer.domain.model.ReviewChoice
import com.neteinstein.networth.feature.importer.domain.model.ReviewItem

/** Builds the Review & learn queue and applies the family's choices. */
object ReviewQueue {
    /** How many category chips each item offers (the design shows three or four). */
    private const val CHOICES = 3

    /** Uncategorised outflows, newest first, each with a suggestion from past choices. */
    fun build(database: FamilyDatabase): List<ReviewItem> {
        val categories = database.categories.sortedBy { it.position }
        val filed = database.transactions.filter { it.categoryId != null && !it.isInternalTransfer }
        // Past choices per merchant key, and overall popularity as a fallback for the chips.
        val byKey = filed.groupBy { Descriptions.describe(it.description).key }
        val popular = filed.groupingBy { it.categoryId!! }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        val defaults = (popular + categories.map { it.id }).distinct()
        return database.transactions.filter {
            it.needsReview
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenBy { it.description })
            .map { tx ->
                val info = Descriptions.describe(tx.description)
                val past = info.key?.let { byKey[it] }.orEmpty()
                val counts = past.groupingBy { it.categoryId!! }.eachCount()
                val best = counts.maxByOrNull { it.value }
                val kindDefault = if (best == null) defaultFor(database, info.kind) ?: knownBrand(database, info) else null
                val suggestion = best?.key ?: kindDefault
                ReviewItem(
                    transaction = tx,
                    info = info,
                    suggestedCategoryId = suggestion,
                    history = best?.let { it.value to past.size },
                    choiceIds = (listOfNotNull(suggestion) + counts.keys + defaults).distinct().filter { id ->
                        categories.any { it.id == id }
                    }.take(
                        CHOICES + 1,
                    ),
                    needsNote = info.kind == MovementKind.MB_WAY_PERSON || info.kind == MovementKind.LOW_VALUE_CARD,
                    ruleKey = Rules.learn(info)?.first,
                )
            }
    }

    /** Bank fees and mortgage instalments map onto the seeded subcategories when they still exist. */
    private fun defaultFor(
        database: FamilyDatabase,
        kind: MovementKind,
    ): String? {
        val subcategory =
            when (kind) {
                MovementKind.FEE, MovementKind.TAX -> "Bank fees"
                MovementKind.MORTGAGE -> "Rent / mortgage"
                else -> return null
            }
        return database.categories.firstOrNull { c -> c.subcategories.any { it.name.equals(subcategory, ignoreCase = true) } }?.id
    }

    /** The category of a well-known Portuguese brand ("Continente" → Groceries), found by its default subcategory. */
    private fun knownBrand(
        database: FamilyDatabase,
        info: DescriptionInfo,
    ): String? {
        val names = KnownMerchants.subcategoryFor(info)?.let(KnownMerchants::namesOf) ?: return null
        return database.categories.firstOrNull { c ->
            c.subcategories.any { sub -> names.any { it.equals(sub.name.trim(), ignoreCase = true) } }
        }?.id
    }

    /** Number of merchants Net Worth can already file on its own (learned rules + merchants with history). */
    fun knownMerchants(database: FamilyDatabase): Int {
        val fromHistory = database.transactions.filter { it.categoryId != null }.mapNotNull { Descriptions.describe(it.description).key }
        return (fromHistory + database.merchantRules.map { it.pattern.uppercase() }).toSet().size
    }

    /**
     * Files the chosen transactions; remembered choices become merchant rules, which also file any
     * other waiting transaction from the same merchant. Returns how many transactions were filed.
     */
    fun apply(
        database: FamilyDatabase,
        choices: List<ReviewChoice>,
        ids: IdGenerator,
        now: Long,
    ): Pair<FamilyDatabase, Int> {
        var rules = database.merchantRules
        val byId = choices.associateBy { it.transactionId }
        var filed = 0
        var transactions =
            database.transactions.map { tx ->
                val choice = byId[tx.id] ?: return@map tx
                filed++
                if (choice.remember) {
                    Rules.learn(Descriptions.describe(tx.description))?.let { (pattern, match) ->
                        val existing = rules.firstOrNull { it.pattern.equals(pattern, ignoreCase = true) && it.match == match }
                        rules =
                            rules.upsert(
                                MerchantRule(
                                    existing?.id ?: ids.newId(),
                                    pattern,
                                    match,
                                    choice.categoryId,
                                    choice.subcategoryId,
                                    (existing?.hits ?: 0) + 1,
                                    now,
                                ),
                            )
                    }
                }
                tx.copy(
                    categoryId = choice.categoryId,
                    subcategoryId = choice.subcategoryId,
                    note = choice.note?.trim()?.takeIf { it.isNotEmpty() } ?: tx.note,
                    updatedAt = now,
                )
            }
        val learned = rules.filter { it.updatedAt == now && it !in database.merchantRules }
        if (learned.isNotEmpty()) {
            transactions =
                transactions.map { tx ->
                    if (!tx.needsReview) return@map tx
                    val rule = Rules.find(learned, tx.description) ?: return@map tx
                    filed++
                    tx.copy(categoryId = rule.categoryId, subcategoryId = rule.subcategoryId, updatedAt = now)
                }
        }
        return database.copy(transactions = transactions, merchantRules = rules) to filed
    }
}

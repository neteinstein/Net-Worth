package com.neteinstein.networth.feature.manage.domain

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One rule as a rules file writes it: the category and subcategory by name, since ids differ in every file. */
data class RuleFileEntry(
    val pattern: String,
    val match: RuleMatch,
    val category: String,
    val subcategory: String? = null,
)

/** Why a picked file couldn't be read as merchant rules. */
enum class RuleFileError {
    /** Not a JSON object or list. */
    NOT_JSON,

    /** JSON, but without a single usable rule. */
    NO_RULES,
}

/** A rules file as read: its usable rules, or why there are none. [invalid] counts entries left out. */
data class RuleFileContent(
    val entries: List<RuleFileEntry> = emptyList(),
    val invalid: Int = 0,
    val error: RuleFileError? = null,
)

/** A rule from the file with its category found: what it would add or change. */
data class ResolvedRule(
    val pattern: String,
    val match: RuleMatch,
    val categoryId: String,
    val subcategoryId: String?,
)

/** What importing a rules file would do to the household's rules. */
data class RuleImportPlan(
    val toAdd: List<ResolvedRule>,
    /** Existing rules (same text and match) whose category or subcategory the file changes, already changed. */
    val toUpdate: List<MerchantRule>,
    val unchanged: Int,
    /** "Category" or "Category › Subcategory" the household doesn't have; their rules are left out. */
    val unknownCategories: List<String>,
    /** Every rule of the file whose category was found: what re-filing the past uses. */
    val resolved: List<ResolvedRule> = toAdd,
) {
    val hasChanges: Boolean get() = toAdd.isNotEmpty() || toUpdate.isNotEmpty()

    /** [resolved] as merchant rules, for matching transactions. */
    val rules: List<MerchantRule>
        get() = resolved.map { MerchantRule("", it.pattern, it.match, it.categoryId, it.subcategoryId) }
}

/**
 * Merchant rules files: `{"rules": [{"pattern": "LIDL", "match": "PREFIX", "category": "Groceries",
 * "subcategory": "Supermarket"}, …]}` (or the bare list). `match` is PREFIX (the default), CONTAINS or PHONE.
 */
object RuleFile {
    /** The name an exported rules file is shared under. */
    const val EXPORT_FILE_NAME = "merchant-rules.json"

    private val pretty = Json { prettyPrint = true }

    /**
     * Writes [rules] as a rules file that [read] takes back: categories by name, in the household's category order,
     * then by text. Rules whose category no longer exists are left out.
     */
    fun write(rules: List<MerchantRule>, categories: List<Category>): String {
        val byId = categories.associateBy { it.id }
        val lines = rules.mapNotNull { rule ->
            val category = byId[rule.categoryId] ?: return@mapNotNull null
            Triple(rule, category, category.subcategories.firstOrNull { it.id == rule.subcategoryId })
        }.sortedWith(compareBy({ it.second.position }, { it.second.name }, { it.first.pattern }))
        val root = buildJsonObject {
            put(
                "rules",
                buildJsonArray {
                    lines.forEach { (rule, category, subcategory) ->
                        add(
                            buildJsonObject {
                                put("pattern", rule.pattern.trim())
                                put("match", rule.match.name)
                                put("category", category.name)
                                subcategory?.let { put("subcategory", it.name) }
                            },
                        )
                    }
                },
            )
        }
        return pretty.encodeToString(JsonElement.serializer(), root) + "\n"
    }

    fun read(text: String): RuleFileContent {
        val root = runCatching { Json.parseToJsonElement(text.removePrefix("")) }.getOrNull()
            ?: return RuleFileContent(error = RuleFileError.NOT_JSON)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root["rules"] as? JsonArray ?: return RuleFileContent(error = RuleFileError.NO_RULES)
            // A bare word or number: the parser reads plain text that way.
            else -> return RuleFileContent(error = RuleFileError.NOT_JSON)
        }
        val entries = items.mapNotNull(::entry)
        // A pattern written twice keeps its last line, as if the rules were typed one after the other.
        val unique = entries.associateBy { it.pattern to it.match }.values.toList()
        return RuleFileContent(
            entries = unique,
            invalid = items.size - entries.size,
            error = RuleFileError.NO_RULES.takeIf { unique.isEmpty() },
        )
    }

    private fun entry(item: JsonElement): RuleFileEntry? {
        val fields = item as? JsonObject ?: return null
        val pattern = fields.text("pattern")?.uppercase() ?: return null
        val category = fields.text("category") ?: return null
        val match = fields.text("match")?.let { name ->
            RuleMatch.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return null
        }
        return RuleFileEntry(pattern, match ?: RuleMatch.PREFIX, category, fields.text("subcategory"))
    }

    private fun JsonObject.text(key: String): String? =
        (get(key)?.takeUnless { it is JsonNull } as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

    /** Finds each entry's category by name (ignoring case) and compares it with the [existing] rules. */
    fun plan(entries: List<RuleFileEntry>, categories: List<Category>, existing: List<MerchantRule>): RuleImportPlan {
        val toAdd = mutableListOf<ResolvedRule>()
        val toUpdate = mutableListOf<MerchantRule>()
        val resolvedRules = mutableListOf<ResolvedRule>()
        val unknown = linkedSetOf<String>()
        var unchanged = 0
        entries.forEach { entry ->
            val category = categories.firstOrNull { sameName(it.name, entry.category) }
            val subcategory = entry.subcategory?.let { name -> category?.subcategories?.firstOrNull { sameName(it.name, name) } }
            if (category == null || (entry.subcategory != null && subcategory == null)) {
                unknown += listOfNotNull(category?.name ?: entry.category, entry.subcategory).joinToString(" › ")
                return@forEach
            }
            val resolved = ResolvedRule(entry.pattern, entry.match, category.id, subcategory?.id)
            resolvedRules += resolved
            val same = existing.firstOrNull { it.pattern.trim().uppercase() == entry.pattern && it.match == entry.match }
            when {
                same == null -> toAdd += resolved
                same.categoryId == resolved.categoryId && same.subcategoryId == resolved.subcategoryId -> unchanged++
                else -> toUpdate += same.copy(categoryId = resolved.categoryId, subcategoryId = resolved.subcategoryId)
            }
        }
        return RuleImportPlan(toAdd, toUpdate, unchanged, unknown.toList(), resolvedRules)
    }

    private fun sameName(a: String, b: String): Boolean = a.normalized() == b.normalized()

    private fun String.normalized(): String = trim().replace('’', '\'').lowercase()
}

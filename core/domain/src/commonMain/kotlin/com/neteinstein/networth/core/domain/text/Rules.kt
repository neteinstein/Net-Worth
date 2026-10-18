package com.neteinstein.networth.core.domain.text

import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch

/**
 * Merchant rules: what "Always file DECATHLON this way" turns into.
 *
 * - [RuleMatch.PREFIX] matches the start of the cleaned merchant name ("DECATHLON MATOSINHOS").
 * - [RuleMatch.CONTAINS] matches anywhere in the raw bank description.
 * - [RuleMatch.PHONE] matches an MB WAY transfer's phone number or person exactly.
 */
object Rules {
    fun matches(
        rule: MerchantRule,
        description: String,
        info: DescriptionInfo = Descriptions.describe(description),
    ): Boolean {
        val pattern = rule.pattern.uppercase().trim()
        if (pattern.isEmpty()) return false
        return when (rule.match) {
            RuleMatch.PREFIX -> info.merchant?.uppercase()?.startsWith(pattern) == true
            RuleMatch.CONTAINS -> description.uppercase().contains(pattern)
            RuleMatch.PHONE -> info.phone?.uppercase() == pattern || info.counterparty?.uppercase() == pattern
        }
    }

    /** The most specific matching rule (longest pattern), if any. */
    fun find(
        rules: List<MerchantRule>,
        description: String,
        info: DescriptionInfo = Descriptions.describe(description),
    ): MerchantRule? = rules.filter { matches(it, description, info) }.maxByOrNull { it.pattern.length }

    /** The rule to learn from a choice on [info], or null when there's nothing to key it on. */
    fun learn(info: DescriptionInfo): Pair<String, RuleMatch>? {
        info.phone?.let { return it to RuleMatch.PHONE }
        info.merchant?.let { return Descriptions.keyOf(it.uppercase()) to RuleMatch.PREFIX }
        info.counterparty?.let { return it.uppercase() to RuleMatch.PHONE }
        return null
    }
}

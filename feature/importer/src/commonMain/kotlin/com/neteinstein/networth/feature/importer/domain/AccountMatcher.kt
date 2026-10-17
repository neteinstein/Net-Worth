package com.neteinstein.networth.feature.importer.domain

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.parse.Values

/** Finds the family account a statement section belongs to, by IBAN or account number. */
object AccountMatcher {
    fun match(
        database: FamilyDatabase,
        section: ParsedSection,
    ): Account? {
        val iban = section.iban?.uppercase()
        val number = section.accountNumber?.let(Values::digits)?.takeIf { it.length >= 6 }
        return database.accounts.firstOrNull { iban != null && it.iban?.uppercase() == iban }
            ?: database.accounts.firstOrNull { account ->
                val own = account.accountNumber?.let(Values::digits)?.takeIf { it.length >= 6 }
                number != null && own != null && (own.endsWith(number) || number.endsWith(own))
            }
            ?: database.accounts.firstOrNull { account ->
                // An account saved with only its IBAN still matches a statement that only shows the number.
                number != null && account.iban?.let(Values::digits)?.contains(number) == true
            }
    }
}

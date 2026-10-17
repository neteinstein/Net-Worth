package com.neteinstein.networth.feature.onboarding.domain.model

import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.seed.HouseholdSetup

/** What the "Start fresh" form holds while it is being filled in. */
data class HouseholdForm(
    val householdName: String = "",
    val adults: List<String> = listOf(""),
    val children: List<String> = emptyList(),
    val currencyCode: String = CurrencyCode.DEFAULT.code,
    val createAccounts: Boolean = true,
) {
    val canSubmit: Boolean get() = householdName.isNotBlank() && adults.any { it.isNotBlank() }

    fun toSetup(jointSavingsName: String) = HouseholdSetup(
        householdName = householdName,
        adults = adults,
        children = children,
        currencyCode = currencyCode,
        createAccounts = createAccounts,
        jointSavingsName = jointSavingsName,
    )
}

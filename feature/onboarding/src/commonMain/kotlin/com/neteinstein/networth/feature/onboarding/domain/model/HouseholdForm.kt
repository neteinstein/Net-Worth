package com.neteinstein.networth.feature.onboarding.domain.model

import com.neteinstein.networth.core.domain.model.CurrencyCode
import com.neteinstein.networth.core.domain.repository.DatabaseFileName
import com.neteinstein.networth.core.domain.repository.DatabaseFolder
import com.neteinstein.networth.core.domain.repository.NewDatabaseFile
import com.neteinstein.networth.core.domain.seed.HouseholdSetup

/** What the "Start fresh" form holds while it is being filled in. */
data class HouseholdForm(
    val householdName: String = "",
    val adults: List<String> = listOf(""),
    val children: List<String> = emptyList(),
    val currencyCode: String = CurrencyCode.DEFAULT.code,
    val createAccounts: Boolean = true,
    /** False: the app names the file and puts it in My Drive. True: the user picks [fileName] and [folder]. */
    val chooseFile: Boolean = false,
    val fileName: String = "",
    /** Null is the root of My Drive. */
    val folder: DatabaseFolder? = null,
) {
    val canSubmit: Boolean get() = householdName.isNotBlank() && adults.any { it.isNotBlank() }

    /** The name the file will get: the typed one when the user chooses, otherwise one from the family name. */
    val resolvedFileName: String
        get() = (if (chooseFile) DatabaseFileName.custom(fileName) else null) ?: DatabaseFileName.forHousehold(householdName)

    fun toNewFile(): NewDatabaseFile =
        if (chooseFile) NewDatabaseFile(name = fileName.takeIf(String::isNotBlank), folderId = folder?.id) else NewDatabaseFile()

    fun toSetup(jointSavingsName: String) = HouseholdSetup(
        householdName = householdName,
        adults = adults,
        children = children,
        currencyCode = currencyCode,
        createAccounts = createAccounts,
        jointSavingsName = jointSavingsName,
    )
}

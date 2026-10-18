package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.repository.DatabaseFileName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseFileNameTest {
    @Test
    fun householdNamesAreSanitised() {
        assertEquals("Net Worth – A B.networth.json", DatabaseFileName.forHousehold("A/B"))
        assertEquals("Net Worth – Family.networth.json", DatabaseFileName.forHousehold(" ? "))
    }

    @Test
    fun customNamesGetTheDatabaseEnding() {
        assertEquals("Our money.networth.json", DatabaseFileName.custom(" Our money "))
        assertEquals("Our money.networth.json", DatabaseFileName.custom("Our money.networth.json"))
        assertEquals("Our money.networth.json", DatabaseFileName.custom("Our money.JSON"))
        assertEquals("2026 budget.networth.json", DatabaseFileName.custom("2026: budget"))
    }

    @Test
    fun blankCustomNamesAreNotNames() {
        assertNull(DatabaseFileName.custom("  "))
        assertNull(DatabaseFileName.custom("/?.networth.json"))
    }
}

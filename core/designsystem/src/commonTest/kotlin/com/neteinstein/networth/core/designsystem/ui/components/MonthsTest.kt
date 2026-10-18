package com.neteinstein.networth.core.designsystem.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthsTest {
    @Test
    fun loadedNamesAreKept() {
        val names = (1..12).map { "m$it" }
        assertEquals(names, names.orBlankMonths())
    }

    @Test
    fun notYetLoadedNamesAreTwelveBlanks() {
        // The web's first frame: the array resource is still loading. Indexing October must not throw.
        val names = emptyList<String>().orBlankMonths()
        assertEquals(12, names.size)
        assertEquals("", names[9])
    }
}

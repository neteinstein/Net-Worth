package com.neteinstein.networth.core.designsystem.ui.components

import com.neteinstein.networth.core.domain.seed.DefaultCategories
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryNamesTest {
    @Test
    fun everySeededNameCanBeTranslated() {
        assertEquals(DefaultCategories.names, defaultCategoryNames.keys)
    }
}

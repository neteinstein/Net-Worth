package com.neteinstein.networth.feature.manage.domain

import com.neteinstein.networth.core.domain.seed.DefaultCategories
import com.neteinstein.networth.core.testing.SequentialIds
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The rules file shipped in docs/ imports cleanly into a household with the default categories. */
class BrandRulesFileTest {
    @Test
    fun everyRuleFindsItsDefaultCategory() {
        val content = RuleFile.read(File("../../docs/merchant-rules/pt-brands.json").readText())
        val plan = RuleFile.plan(content.entries, DefaultCategories.create(SequentialIds("c"), 0), emptyList())

        assertNull(content.error)
        assertEquals(0, content.invalid)
        assertEquals(emptyList(), plan.unknownCategories)
        assertEquals(content.entries.size, plan.toAdd.size)
    }
}

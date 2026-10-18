package com.neteinstein.networth.feature.manage.ui.screens

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.core.testing.screenshot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every screen of this module in light and dark, compared with the committed goldens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenshotTest {
    private fun manageState() = ManageUiState(
        loading = false,
        accounts = listOf(
            ManagedAccount(Account("a", "Pedro", AccountType.CURRENT, listOf("p"), bank = "novobanco"), 2_140_00, false, true),
            ManagedAccount(Account("j", "Joint savings", AccountType.SAVINGS, listOf("p", "m")), 12_600_00, false, false),
            ManagedAccount(Account("i", "Inês", AccountType.SAVINGS, listOf("i")), 2_300_00, true, false),
            ManagedAccount(Account("h", "Mortgage", AccountType.MORTGAGE, listOf("p", "m")), 147_268_16, false, false),
        ),
        members = listOf(
            Member("p", "Pedro", MemberRole.ADULT),
            Member("m", "Maria", MemberRole.ADULT),
            Member("i", "Inês", MemberRole.CHILD),
        ),
        categoryCount = 10,
        subcategoryCount = 39,
        categoryColors = listOf("#141B2D", "#2D55D6", "#D9652B", "#7D9BF2"),
        rules = listOf(
            RuleLine("r", "CONTINENTE*", "Groceries", "#2D55D6"),
            RuleLine("s", "DECATHLON*", "Kids’ activities", "#F0A964"),
        ),
    )

    @Test
    fun manage() = screenshot("manage") { ManageContent(manageState(), ManageActions()) }

    @Test
    fun manageAssumptions() = screenshot("manage_assumptions") {
        ManageContent(manageState(), ManageActions(), showAssumptionsAtStart = true)
    }

    @Test
    fun categories() = screenshot("categories") {
        val school = Category(
            "s",
            "School",
            "#D9652B",
            listOf(Subcategory("a", "Books & materials"), Subcategory("b", "Canteen"), Subcategory("c", "School trips")),
            monthlyBudgetMinor = 300_00,
            seasonalPeakMonth = 9,
        )
        CategoriesContent(
            CategoriesUiState(
                loading = false,
                categories = listOf(
                    CategoryItem(Category("h", "Housing", "#141B2D", monthlyBudgetMinor = 1_200_00), 18, 40),
                    CategoryItem(school, 12, 31),
                    CategoryItem(Category("g", "Groceries", "#2D55D6"), 22, 90),
                ),
                expandedId = "s",
            ),
            CategoryActions(),
        )
    }
}

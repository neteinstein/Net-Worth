package com.neteinstein.networth.core.domain.seed

import com.neteinstein.networth.core.domain.model.Category
import com.neteinstein.networth.core.domain.model.Subcategory
import com.neteinstein.networth.core.domain.repository.IdGenerator

/**
 * The starting categories, copied from the "Categories" artboard of the design
 * (docs/design/Categories.dc.html): names, colours and subcategories, in that order.
 * Budgets and seasonal peaks shown there are example values and are not seeded.
 */
object DefaultCategories {
    internal val definitions: List<Triple<String, String, List<String>>> = listOf(
        Triple(
            "Housing",
            "#141B2D",
            listOf("Rent / mortgage", "Condo fees", "Electricity", "Water", "Gas", "Internet & phone", "Insurance"),
        ),
        Triple("Groceries", "#2D55D6", listOf("Supermarket", "Bakery & market", "Household supplies")),
        Triple(
            "School",
            "#D9652B",
            listOf("Books & materials", "Canteen", "Parents’ association", "School trips", "After-school (ATL)"),
        ),
        Triple("Transport", "#7D9BF2", listOf("Fuel", "Tolls", "Public transport", "Parking", "Car maintenance")),
        Triple("Clothes", "#A8487A", listOf("Adults", "Kids", "Shoes")),
        Triple("Kids’ activities", "#F0A964", listOf("Sports", "Music", "Camps")),
        Triple("Dining & leisure", "#6A4FC9", listOf("Restaurants", "Takeaway", "Outings", "Subscriptions")),
        Triple("Health", "#3E8C7E", listOf("Pharmacy", "Doctor & dentist", "Optician")),
        Triple("Vacations", "#C9CFDA", listOf("Travel", "Accommodation", "Activities")),
        Triple("Other", "#A3ABBA", listOf("Gifts", "Bank fees", "Misc")),
    )

    fun create(ids: IdGenerator, now: Long): List<Category> = definitions.mapIndexed { index, (name, color, subs) ->
        Category(
            id = ids.newId(),
            name = name,
            colorHex = color,
            subcategories = subs.map { Subcategory(ids.newId(), it) },
            position = index,
            updatedAt = now,
        )
    }

    /** Swatches offered when recolouring a category (Categories artboard). */
    val palette: List<String> = listOf("#D9652B", "#2D55D6", "#3E8C7E", "#A8487A", "#6A4FC9", "#141B2D")
}

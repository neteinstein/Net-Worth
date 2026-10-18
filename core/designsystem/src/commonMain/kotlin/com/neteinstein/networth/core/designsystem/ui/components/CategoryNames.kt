package com.neteinstein.networth.core.designsystem.ui.components

import androidx.compose.runtime.Composable
import com.neteinstein.networth.core.designsystem.generated.resources.Res
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_accommodation
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_activities
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_adults
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_after_school_atl
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_bakery_market
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_bank_costs
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_bank_fees
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_books_materials
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_camps
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_canteen
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_car_maintenance
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_card_fees
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_clothes
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_condo_fees
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_dining_leisure
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_doctor_dentist
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_electricity
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_fuel
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_gas
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_gifts
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_groceries
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_health
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_household_supplies
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_housing
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_insurance
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_interest
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_internet_phone
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_kids
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_kids_activities
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_misc
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_music
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_optician
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_other
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_outings
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_parents_association
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_parking
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_pharmacy
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_public_transport
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_rent_mortgage
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_restaurants
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_school
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_school_trips
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_shoes
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_sports
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_subscriptions
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_supermarket
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_takeaway
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_tolls
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_transport
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_travel
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_vacations
import com.neteinstein.networth.core.designsystem.generated.resources.designsystem_category_water
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A category or subcategory name as the family sees it. The defaults are seeded into the shared
 * database in English (`DefaultCategories`), so a name that is still exactly one of them is shown in
 * the app's language; anything the family typed or renamed is shown as stored.
 */
@Composable
fun categoryName(name: String): String = defaultCategoryNames[name]?.let { stringResource(it) } ?: name

/** The seeded English names (categories and subcategories) and their translatable strings. */
internal val defaultCategoryNames: Map<String, StringResource> = mapOf(
    "Housing" to Res.string.designsystem_category_housing,
    "Rent / mortgage" to Res.string.designsystem_category_rent_mortgage,
    "Condo fees" to Res.string.designsystem_category_condo_fees,
    "Electricity" to Res.string.designsystem_category_electricity,
    "Water" to Res.string.designsystem_category_water,
    "Gas" to Res.string.designsystem_category_gas,
    "Internet & phone" to Res.string.designsystem_category_internet_phone,
    "Insurance" to Res.string.designsystem_category_insurance,
    "Groceries" to Res.string.designsystem_category_groceries,
    "Supermarket" to Res.string.designsystem_category_supermarket,
    "Bakery & market" to Res.string.designsystem_category_bakery_market,
    "Household supplies" to Res.string.designsystem_category_household_supplies,
    "School" to Res.string.designsystem_category_school,
    "Books & materials" to Res.string.designsystem_category_books_materials,
    "Canteen" to Res.string.designsystem_category_canteen,
    "Parents’ association" to Res.string.designsystem_category_parents_association,
    "School trips" to Res.string.designsystem_category_school_trips,
    "After-school (ATL)" to Res.string.designsystem_category_after_school_atl,
    "Transport" to Res.string.designsystem_category_transport,
    "Fuel" to Res.string.designsystem_category_fuel,
    "Tolls" to Res.string.designsystem_category_tolls,
    "Public transport" to Res.string.designsystem_category_public_transport,
    "Parking" to Res.string.designsystem_category_parking,
    "Car maintenance" to Res.string.designsystem_category_car_maintenance,
    "Clothes" to Res.string.designsystem_category_clothes,
    "Adults" to Res.string.designsystem_category_adults,
    "Kids" to Res.string.designsystem_category_kids,
    "Shoes" to Res.string.designsystem_category_shoes,
    "Kids’ activities" to Res.string.designsystem_category_kids_activities,
    "Sports" to Res.string.designsystem_category_sports,
    "Music" to Res.string.designsystem_category_music,
    "Camps" to Res.string.designsystem_category_camps,
    "Dining & leisure" to Res.string.designsystem_category_dining_leisure,
    "Restaurants" to Res.string.designsystem_category_restaurants,
    "Takeaway" to Res.string.designsystem_category_takeaway,
    "Outings" to Res.string.designsystem_category_outings,
    "Subscriptions" to Res.string.designsystem_category_subscriptions,
    "Health" to Res.string.designsystem_category_health,
    "Pharmacy" to Res.string.designsystem_category_pharmacy,
    "Doctor & dentist" to Res.string.designsystem_category_doctor_dentist,
    "Optician" to Res.string.designsystem_category_optician,
    "Bank costs" to Res.string.designsystem_category_bank_costs,
    "Bank fees" to Res.string.designsystem_category_bank_fees,
    "Card fees" to Res.string.designsystem_category_card_fees,
    "Interest" to Res.string.designsystem_category_interest,
    "Vacations" to Res.string.designsystem_category_vacations,
    "Travel" to Res.string.designsystem_category_travel,
    "Accommodation" to Res.string.designsystem_category_accommodation,
    "Activities" to Res.string.designsystem_category_activities,
    "Other" to Res.string.designsystem_category_other,
    "Gifts" to Res.string.designsystem_category_gifts,
    "Misc" to Res.string.designsystem_category_misc,
)

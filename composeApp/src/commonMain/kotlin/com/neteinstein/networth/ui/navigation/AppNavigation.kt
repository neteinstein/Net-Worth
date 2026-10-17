package com.neteinstein.networth.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.neteinstein.networth.feature.manage.ui.screens.CategoriesScreen
import com.neteinstein.networth.feature.manage.ui.screens.ManageScreen
import com.neteinstein.networth.feature.overview.ui.screens.OverviewScreen
import com.neteinstein.networth.feature.settings.ui.screens.SettingsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Non-JVM targets (iOS, web) have no reflection, so the back stack can only be saved if every
 * [Route] is registered for polymorphic serialization here.
 */
internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Overview::class, Route.Overview.serializer())
            subclass(Route.Manage::class, Route.Manage.serializer())
            subclass(Route.Categories::class, Route.Categories.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
        }
    }
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, Route.Overview)
    val back: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        transitionSpec = { forward() },
        popTransitionSpec = { backward() },
        predictivePopTransitionSpec = { backward() },
        entryProvider = entryProvider {
            entry<Route.Overview> {
                OverviewScreen(onOpenManage = { backStack.add(Route.Manage) })
            }
            entry<Route.Manage> {
                ManageScreen(
                    onBack = back,
                    onOpenCategories = { backStack.add(Route.Categories) },
                    onOpenSettings = { backStack.add(Route.Settings) },
                )
            }
            entry<Route.Categories> {
                CategoriesScreen(onBack = back)
            }
            entry<Route.Settings> {
                SettingsScreen(onBack = back)
            }
        },
    )
}

private const val DURATION = 380

private fun forward(): ContentTransform =
    (slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION))) togetherWith
        (slideOutHorizontally(tween(DURATION)) { -it / 4 } + fadeOut(tween(DURATION)))

private fun backward(): ContentTransform =
    (slideInHorizontally(tween(DURATION)) { -it / 4 } + fadeIn(tween(DURATION))) togetherWith
        (slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION)) + scaleOut(targetScale = 0.95f))

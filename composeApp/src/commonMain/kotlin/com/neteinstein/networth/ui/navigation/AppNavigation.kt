package com.neteinstein.networth.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.neteinstein.networth.composeapp.generated.resources.Res
import com.neteinstein.networth.composeapp.generated.resources.nav_alerts
import com.neteinstein.networth.composeapp.generated.resources.nav_future
import com.neteinstein.networth.composeapp.generated.resources.nav_import
import com.neteinstein.networth.composeapp.generated.resources.nav_overview
import com.neteinstein.networth.composeapp.generated.resources.nav_settings
import com.neteinstein.networth.composeapp.generated.resources.nav_spending
import com.neteinstein.networth.core.designsystem.ui.icons.NetWorthIcons
import com.neteinstein.networth.feature.alerts.ui.screens.AlertsScreen
import com.neteinstein.networth.feature.future.ui.screens.FutureScreen
import com.neteinstein.networth.feature.future.ui.screens.YearAheadScreen
import com.neteinstein.networth.feature.importer.ui.screens.ImportScreen
import com.neteinstein.networth.feature.importer.ui.screens.ReviewScreen
import com.neteinstein.networth.feature.manage.ui.screens.CategoriesScreen
import com.neteinstein.networth.feature.manage.ui.screens.ManageScreen
import com.neteinstein.networth.feature.overview.ui.screens.OverviewScreen
import com.neteinstein.networth.feature.settings.ui.screens.SettingsScreen
import com.neteinstein.networth.feature.spending.ui.screens.FiltersScreen
import com.neteinstein.networth.feature.spending.ui.screens.SpendingScreen
import com.neteinstein.networth.feature.spending.ui.screens.TransactionsRequest
import com.neteinstein.networth.feature.spending.ui.screens.TransactionsScreen
import kotlinx.datetime.LocalDate
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource

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
            subclass(Route.Import::class, Route.Import.serializer())
            subclass(Route.Review::class, Route.Review.serializer())
            subclass(Route.Spending::class, Route.Spending.serializer())
            subclass(Route.Filters::class, Route.Filters.serializer())
            subclass(Route.Transactions::class, Route.Transactions.serializer())
            subclass(Route.Alerts::class, Route.Alerts.serializer())
            subclass(Route.Future::class, Route.Future.serializer())
            subclass(Route.YearAhead::class, Route.YearAhead.serializer())
        }
    }
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, Route.Overview)
    val back: () -> Unit = { backStack.removeLastOrNull() }
    val current = backStack.lastOrNull() as? Route
    val onTab = current != null && current in TABS

    // Tabs sit on top of Overview, so back from a tab returns to Overview.
    fun selectTab(tab: Route) {
        while (backStack.size > 1) backStack.removeLastOrNull()
        if (tab != Route.Overview) backStack.add(tab)
    }

    Scaffold(
        topBar = {
            if (onTab) TopBar(onOpenSettings = { backStack.add(Route.Settings) })
        },
        bottomBar = {
            if (current != null && onTab) {
                BottomBar(
                    current = current,
                    onSelect = ::selectTab,
                    onImport = { backStack.add(Route.Import) },
                )
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = back,
            modifier = Modifier
                .padding(top = if (onTab) padding.calculateTopPadding() else 0.dp, bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding),
            transitionSpec = { forward() },
            popTransitionSpec = { backward() },
            predictivePopTransitionSpec = { backward() },
            entryProvider = entryProvider {
                entry<Route.Overview> {
                    OverviewScreen(
                        onOpenManage = { backStack.add(Route.Manage) },
                        onOpenImport = { backStack.add(Route.Import) },
                        onOpenReview = { backStack.add(Route.Review) },
                        onOpenFuture = { selectTab(Route.Future) },
                    )
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
                entry<Route.Import> {
                    ImportScreen(onBack = back, onOpenReview = { backStack.add(Route.Review) })
                }
                entry<Route.Review> {
                    ReviewScreen(onBack = back)
                }
                entry<Route.Spending> {
                    SpendingScreen(
                        onOpenFilters = { backStack.add(Route.Filters) },
                        onOpenCategory = { categoryId, period, accountIds ->
                            backStack.add(Route.Transactions(categoryId, period.start.toString(), period.end.toString(), accountIds))
                        },
                    )
                }
                entry<Route.Filters> {
                    FiltersScreen(onBack = back, onShowTransactions = { backStack.add(Route.Transactions(fromFilters = true)) })
                }
                entry<Route.Transactions> { route ->
                    TransactionsScreen(request = route.toRequest(), onBack = back)
                }
                entry<Route.Alerts> {
                    AlertsScreen(
                        onOpenAlertRules = { backStack.add(Route.Manage) },
                        onSeeTransactions = { categoryId, month ->
                            backStack.add(Route.Transactions(categoryId, month.firstDay.toString(), month.lastDay.toString()))
                        },
                    )
                }
                entry<Route.Future> {
                    FutureScreen(onOpenYearAhead = { backStack.add(Route.YearAhead) })
                }
                entry<Route.YearAhead> {
                    YearAheadScreen(onBack = back)
                }
            },
        )
    }
}

private val TABS = setOf(Route.Overview, Route.Spending, Route.Alerts, Route.Future)

private fun Route.Transactions.toRequest() =
    TransactionsRequest(
        categoryId = categoryId,
        start = start?.let(LocalDate::parse),
        end = end?.let(LocalDate::parse),
        accountIds = accountIds,
        fromFilters = fromFilters,
    )

/** Sits above the tab screens: the Settings button in the top-right corner. */
@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedIconButton(onClick = onOpenSettings, modifier = Modifier.size(44.dp)) {
            Icon(NetWorthIcons.Settings, contentDescription = stringResource(Res.string.nav_settings), modifier = Modifier.size(20.dp))
        }
    }
}

/** The artboards' bottom bar: Overview, Spending, the Import action in the middle, Alerts, Future. */
@Composable
private fun BottomBar(
    current: Route,
    onSelect: (Route) -> Unit,
    onImport: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == Route.Overview,
            onClick = { onSelect(Route.Overview) },
            icon = { Icon(NetWorthIcons.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_overview)) },
        )
        NavigationBarItem(
            selected = current == Route.Spending,
            onClick = { onSelect(Route.Spending) },
            icon = { Icon(NetWorthIcons.Bars, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_spending)) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onImport,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        NetWorthIcons.Download,
                        contentDescription = stringResource(Res.string.nav_import),
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
            },
        )
        NavigationBarItem(
            selected = current == Route.Alerts,
            onClick = { onSelect(Route.Alerts) },
            icon = { Icon(NetWorthIcons.Bell, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_alerts)) },
        )
        NavigationBarItem(
            selected = current == Route.Future,
            onClick = { onSelect(Route.Future) },
            icon = { Icon(NetWorthIcons.TrendUp, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_future)) },
        )
    }
}

private const val DURATION = 380

private fun forward(): ContentTransform =
    (slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION))) togetherWith
        (slideOutHorizontally(tween(DURATION)) { -it / 4 } + fadeOut(tween(DURATION)))

private fun backward(): ContentTransform =
    (slideInHorizontally(tween(DURATION)) { -it / 4 } + fadeIn(tween(DURATION))) togetherWith
        (slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION)) + scaleOut(targetScale = 0.95f))

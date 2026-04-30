/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.authenticatednavbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import cmp.navigation.generated.resources.Res
import cmp.navigation.generated.resources.not_connected
import cmp.navigation.ui.KptRootScaffold
import cmp.navigation.ui.ScaffoldNavigationData
import cmp.navigation.ui.logDestinationChanged
import cmp.navigation.ui.rememberKptNavController
import com.letapay.app.core.ui.NavigationItem
import com.letapay.app.feature.chat.chatDestination
import com.letapay.app.feature.chat.navigateToChat
import com.letapay.app.feature.trade.navigateToTrade
import com.letapay.app.feature.trade.tradeDestination
import com.letapay.app.feature.wallet.WalletRoute
import com.letapay.app.feature.wallet.navigateToWallet
import com.letapay.app.feature.wallet.walletDestination
import com.letapay.app.feature.yield.navigateToYield
import com.letapay.app.feature.yield.yieldDestination
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.analytics.rememberAnalyticsHelper
import template.core.base.ui.EventsEffect

@Composable
internal fun AuthenticatedNavbarNavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberKptNavController(
        name = "AuthenticatedNavbarScreen",
    ),
    viewModel: AuthenticatedNavbarNavigationViewModel = koinViewModel(),
) {
    val analyticsHelper = rememberAnalyticsHelper()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    EventsEffect(eventFlow = viewModel.eventFlow) { event ->
        navController.apply {
            when (event) {
                AuthenticatedNavBarEvent.NavigateToChatScreen -> {
                    analyticsHelper.logDestinationChanged(event.tab.startDestinationRoute)
                    navigateToTabOrRoot(tabToNavigateTo = event.tab) { navigateToChat(navOptions = it) }
                }
                AuthenticatedNavBarEvent.NavigateToWalletScreen -> {
                    analyticsHelper.logDestinationChanged(event.tab.startDestinationRoute)
                    navigateToTabOrRoot(tabToNavigateTo = event.tab) { navigateToWallet(navOptions = it) }
                }
                AuthenticatedNavBarEvent.NavigateToTradeScreen -> {
                    analyticsHelper.logDestinationChanged(event.tab.startDestinationRoute)
                    navigateToTabOrRoot(tabToNavigateTo = event.tab) { navigateToTrade(navOptions = it) }
                }
                AuthenticatedNavBarEvent.NavigateToYieldScreen -> {
                    analyticsHelper.logDestinationChanged(event.tab.startDestinationRoute)
                    navigateToTabOrRoot(tabToNavigateTo = event.tab) { navigateToYield(navOptions = it) }
                }
            }
        }
    }

    val message = stringResource(Res.string.not_connected)
    LaunchedEffect(isOffline) {
        if (isOffline) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = Indefinite,
                )
            }
        }
    }

    AuthenticatedNavbarNavigationScreenContent(
        navController = navController,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onAction = remember(viewModel) {
            { viewModel.trySendAction(it) }
        },
    )
}

@Composable
internal fun AuthenticatedNavbarNavigationScreenContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAction: (AuthenticatedNavBarAction) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val navigationItems = persistentListOf<NavigationItem>(
        AuthenticatedNavBarTabItem.ChatTab,
        AuthenticatedNavBarTabItem.WalletTab,
        AuthenticatedNavBarTabItem.TradeTab,
        AuthenticatedNavBarTabItem.YieldTab,
    )

    KptRootScaffold(
        contentWindowInsets = WindowInsets(0.dp),
        navigationData = ScaffoldNavigationData(
            navigationItems = navigationItems,
            selectedNavigationItem = navigationItems.find {
                navBackStackEntry.isCurrentRoute(route = it.graphRoute)
            },
            onNavigationClick = { navigationItem ->
                when (navigationItem) {
                    is AuthenticatedNavBarTabItem.ChatTab -> onAction(AuthenticatedNavBarAction.ChatTabClick)
                    is AuthenticatedNavBarTabItem.WalletTab -> onAction(AuthenticatedNavBarAction.WalletTabClick)
                    is AuthenticatedNavBarTabItem.TradeTab -> onAction(AuthenticatedNavBarAction.TradeTabClick)
                    is AuthenticatedNavBarTabItem.YieldTab -> onAction(AuthenticatedNavBarAction.YieldTabClick)
                }
            },
            shouldShowNavigation = navigationItems.any {
                navBackStackEntry.isCurrentRoute(route = it.startDestinationRoute)
            },
        ),
        containerColor = androidx.compose.ui.graphics.Color(0xFF0A0A0F),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = WalletRoute,
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = androidx.compose.animation.core.tween(300),
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300),
                )
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300),
                )
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300),
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = androidx.compose.animation.core.tween(300),
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300),
                )
            },
        ) {
            chatDestination()
            walletDestination()
            tradeDestination()
            yieldDestination()
        }
    }
}

private fun NavController.navigateToTabOrRoot(
    tabToNavigateTo: AuthenticatedNavBarTabItem,
    navigate: (NavOptions) -> Unit,
) {
    if (tabToNavigateTo.startDestinationRoute == currentDestination?.route) {
        return
    } else if (currentDestination?.parent?.route == tabToNavigateTo.graphRoute) {
        popBackStack(route = tabToNavigateTo.startDestinationRoute, inclusive = false)
    } else {
        navigate(
            navOptions {
                popUpTo(graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            },
        )
    }
}

private fun NavBackStackEntry?.isCurrentRoute(route: String): Boolean =
    this
        ?.destination
        ?.hierarchy
        ?.any { it.route == route } == true

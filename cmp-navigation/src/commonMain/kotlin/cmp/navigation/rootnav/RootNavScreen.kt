/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.rootnav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import cmp.navigation.authenticated.AuthenticatedGraphRoute
import cmp.navigation.authenticated.authenticatedGraph
import cmp.navigation.authenticated.navigateToAuthenticatedGraph
import cmp.navigation.splash.SplashRoute
import cmp.navigation.splash.navigateToSplash
import cmp.navigation.splash.splashDestination
import cmp.navigation.ui.rememberKptNavController
import cmp.navigation.utils.toObjectNavigationRoute
import com.letapay.app.feature.auth.AuthRoute
import com.letapay.app.feature.auth.authDestination
import com.letapay.app.feature.auth.navigateToAuth
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.ui.NonNullEnterTransitionProvider
import template.core.base.ui.NonNullExitTransitionProvider
import template.core.base.ui.RootTransitionProviders
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun RootNavScreen(
    modifier: Modifier = Modifier,
    viewModel: RootNavViewModel = koinViewModel(),
    navController: NavHostController = rememberKptNavController(name = "RootNavScreen"),
    onSplashScreenRemoved: () -> Unit = {},
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val previousStateReference = remember { AtomicReference(state) }

    val isNotSplashScreen = state != RootNavState.Splash
    LaunchedEffect(isNotSplashScreen) {
        if (isNotSplashScreen) onSplashScreenRemoved()
    }

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
        enterTransition = { toEnterTransition()(this) },
        exitTransition = { toExitTransition()(this) },
        popEnterTransition = { toEnterTransition()(this) },
        popExitTransition = { toExitTransition()(this) },
    ) {
        splashDestination()
        authDestination()
        authenticatedGraph()
    }

    val targetRoute = when (state) {
        RootNavState.ShowOnboarding -> ""
        RootNavState.Auth -> AuthRoute.toObjectNavigationRoute()
        RootNavState.Splash -> SplashRoute.toObjectNavigationRoute()
        RootNavState.UserLocked -> ""
        is RootNavState.UserUnlocked -> AuthenticatedGraphRoute.toObjectNavigationRoute()
    }
    val currentRoute = navController.currentDestination?.rootLevelRoute()

    if (currentRoute == targetRoute &&
        previousStateReference.load() == state
    ) {
        previousStateReference.store(state)
        return
    }
    previousStateReference.store(state)

    ClearFocus()

    val rootNavOptions = navOptions {
        popUpTo(navController.graph.id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

    LaunchedEffect(state) {
        when (state) {
            RootNavState.Splash -> navController.navigateToSplash(rootNavOptions)
            RootNavState.Auth -> navController.navigateToAuth(rootNavOptions)
            RootNavState.ShowOnboarding -> {}
            RootNavState.UserLocked -> {}
            is RootNavState.UserUnlocked -> navController.navigateToAuthenticatedGraph(
                navOptions = rootNavOptions,
            )
        }
    }
}

private fun NavDestination?.rootLevelRoute(): String? = when {
    this == null -> null
    parent?.route == null -> route
    else -> parent.rootLevelRoute()
}

@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toEnterTransition(): NonNullEnterTransitionProvider =
    when (targetState.destination.rootLevelRoute()) {
        SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Enter.none
        else -> RootTransitionProviders.Enter.fadeIn
    }

@Suppress("MaxLineLength")
private fun AnimatedContentTransitionScope<NavBackStackEntry>.toExitTransition(): NonNullExitTransitionProvider {
    return when (initialState.destination.rootLevelRoute()) {
        SplashRoute.toObjectNavigationRoute() -> RootTransitionProviders.Exit.none
        else -> RootTransitionProviders.Exit.fadeOut
    }
}

@Composable
expect fun ClearFocus()

/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import template.core.base.ui.composableWithStayTransitions

@Serializable
data object ProfileRoute

fun NavController.navigateToProfile(navOptions: NavOptions? = null) = navigate(ProfileRoute, navOptions)

fun NavGraphBuilder.profileDestination() {
    composableWithStayTransitions<ProfileRoute> {
        val viewModel: ProfileViewModel = koinViewModel()
        val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
        ProfileScreen(uiState = uiState)
    }
}

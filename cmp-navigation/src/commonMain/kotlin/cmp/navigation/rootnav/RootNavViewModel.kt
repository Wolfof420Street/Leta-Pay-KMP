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

import androidx.lifecycle.viewModelScope
import cmp.navigation.rootnav.RootNavAction.Internal.UserStateUpdateReceive
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.data.repository.UserDataRepository
import com.letapay.app.core.model.UserData
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import template.core.base.ui.BaseViewModel

class RootNavViewModel(
    sessionRepository: SessionRepository,
    userDataRepository: UserDataRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {

    init {
        combine(
            sessionRepository.sessionState,
            userDataRepository.userData,
        ) { sessionState, userData ->
            UserStateUpdateReceive(
                isBootstrapped = sessionState.isBootstrapped,
                isAuthenticated = sessionState.session != null && userData.isAuthenticated,
                userData = userData,
            )
        }.onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
        }
    }

    private fun handleUserStateUpdateReceive(
        action: UserStateUpdateReceive,
    ) {
        if (!action.isBootstrapped) {
            mutableStateFlow.update { RootNavState.Splash }
            return
        }

        val userData = action.userData

        val updatedRootNavState = when {
            userData.firstTimeUser -> RootNavState.ShowOnboarding

            !action.isAuthenticated -> RootNavState.Auth

            else -> RootNavState.UserUnlocked(
                activeUserId = userData.activeUserId.ifBlank { "wallet-user" },
            )
        }

        mutableStateFlow.update { updatedRootNavState }
    }
}

sealed class RootNavState {
    data object Auth : RootNavState()

    data object ShowOnboarding : RootNavState()

    data object Splash : RootNavState()

    data object UserLocked : RootNavState()

    data class UserUnlocked(
        val activeUserId: String,
    ) : RootNavState()
}

sealed class RootNavAction {

    sealed class Internal {

        data class UserStateUpdateReceive(
            val isBootstrapped: Boolean,
            val isAuthenticated: Boolean,
            val userData: UserData,
        ) : RootNavAction()
    }
}

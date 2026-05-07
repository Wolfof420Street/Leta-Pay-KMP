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

import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.NetworkMonitor
import com.letapay.app.core.model.UserData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import template.core.base.ui.BaseViewModel

internal class AuthenticatedNavbarNavigationViewModel(
    networkMonitor: NetworkMonitor,
) : BaseViewModel<Unit, AuthenticatedNavBarEvent, AuthenticatedNavBarAction>(
    initialState = Unit,
) {

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    override fun handleAction(action: AuthenticatedNavBarAction) {
        when (action) {
            AuthenticatedNavBarAction.ChatTabClick -> sendEvent(AuthenticatedNavBarEvent.NavigateToChatScreen)
            AuthenticatedNavBarAction.WalletTabClick -> sendEvent(AuthenticatedNavBarEvent.NavigateToWalletScreen)
            AuthenticatedNavBarAction.TradeTabClick -> sendEvent(AuthenticatedNavBarEvent.NavigateToTradeScreen)
            AuthenticatedNavBarAction.YieldTabClick -> sendEvent(AuthenticatedNavBarEvent.NavigateToYieldScreen)
            is AuthenticatedNavBarAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: AuthenticatedNavBarAction.Internal) {
        when (action) {
            is AuthenticatedNavBarAction.Internal.UserStateUpdateReceive -> {
            }
        }
    }
}

internal sealed class AuthenticatedNavBarAction {
    data object ChatTabClick : AuthenticatedNavBarAction()
    data object WalletTabClick : AuthenticatedNavBarAction()
    data object TradeTabClick : AuthenticatedNavBarAction()
    data object YieldTabClick : AuthenticatedNavBarAction()

    sealed class Internal : AuthenticatedNavBarAction() {
        data class UserStateUpdateReceive(val userState: UserData?) : Internal()
    }
}

internal sealed class AuthenticatedNavBarEvent {

    abstract val tab: AuthenticatedNavBarTabItem

    data object NavigateToChatScreen : AuthenticatedNavBarEvent() {
        override val tab: AuthenticatedNavBarTabItem = AuthenticatedNavBarTabItem.ChatTab
    }

    data object NavigateToWalletScreen : AuthenticatedNavBarEvent() {
        override val tab: AuthenticatedNavBarTabItem = AuthenticatedNavBarTabItem.WalletTab
    }

    data object NavigateToTradeScreen : AuthenticatedNavBarEvent() {
        override val tab: AuthenticatedNavBarTabItem = AuthenticatedNavBarTabItem.TradeTab
    }

    data object NavigateToYieldScreen : AuthenticatedNavBarEvent() {
        override val tab: AuthenticatedNavBarTabItem = AuthenticatedNavBarTabItem.YieldTab
    }
}

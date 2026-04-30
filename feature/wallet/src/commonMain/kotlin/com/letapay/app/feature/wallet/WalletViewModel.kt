/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.PortfolioRepository
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.model.wallet.AssetBalance
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletUiState(
    val walletAddress: String? = null,
    val totalUsdValue: String? = null,
    val balances: List<AssetBalance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class WalletViewModel(
    private val sessionRepository: SessionRepository,
    private val portfolioRepository: PortfolioRepository,
) : ViewModel() {

    val stateFlow: StateFlow<WalletUiState> = combine(
        sessionRepository.sessionState,
        portfolioRepository.portfolioState,
    ) { sessionState, portfolioState ->
        WalletUiState(
            walletAddress = sessionState.session?.walletAddress?.value,
            totalUsdValue = portfolioState.balance.totalUsdValue,
            balances = portfolioState.balance.balances,
            isLoading = sessionState.isLoading || portfolioState.isLoading,
            errorMessage = portfolioState.errorMessage ?: sessionState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState(),
    )

    fun refreshBalance() {
        viewModelScope.launch {
            portfolioRepository.refreshBalance()
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionRepository.logout()
        }
    }
}

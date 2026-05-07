/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.trade

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TradeUiState(
    val fromAmount: String = "",
    val toAmount: String = "",
    val fromToken: String = "ETH",
    val toToken: String = "USDC",
    val estimatedFeeUsd: String = "$2.50",
    val isLoading: Boolean = false,
)

class TradeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TradeUiState())
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    fun updateFromAmount(amount: String) {
        _uiState.update { it.copy(fromAmount = amount) }
    }

    fun toggleLoading() {
        _uiState.update { it.copy(isLoading = !it.isLoading) }
    }
}

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
import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.SwapRepository
import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.swap.SwapQuoteRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeUiState(
    val fromAmount: String = "",
    val toAmount: String = "",
    val fromToken: String = "ETH",
    val toToken: String = "USDC",
    val estimatedFeeUsd: String = "",
    val quoteId: String? = null,
    val executionMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

class TradeViewModel(
    private val swapRepository: SwapRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TradeUiState())
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    fun updateFromAmount(amount: String) {
        _uiState.update { it.copy(fromAmount = amount) }
    }

    fun requestQuote() {
        val state = _uiState.value
        val amount = state.fromAmount.trim()
        if (amount.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter an amount before requesting a quote.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, executionMessage = null) }
            when (
                val result = swapRepository.quote(
                    SwapQuoteRequest(
                        fromAsset = state.fromToken,
                        toAsset = state.toToken,
                        amount = amount,
                        chain = 1,
                    ),
                )
            ) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toAmount = result.data.toAmount,
                            estimatedFeeUsd = result.data.estimatedFeeUsd,
                            quoteId = result.data.quoteId,
                            errorMessage = null,
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message,
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun executeQuote() {
        val quoteId = _uiState.value.quoteId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = swapRepository.execute(quoteId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            executionMessage = (
                                "Unsigned swap tx ready for signing on chain " +
                                    "${result.data.unsignedTx.chainId}."
                                ),
                            errorMessage = null,
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message,
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }
}

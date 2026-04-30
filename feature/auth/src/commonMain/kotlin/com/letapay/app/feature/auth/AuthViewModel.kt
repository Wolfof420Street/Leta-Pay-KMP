/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.ConnectWalletRequest
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.model.auth.AuthChallenge
import com.letapay.app.core.model.blockchain.WalletAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val walletAddress: String = "",
    val signature: String = "",
    val deviceToken: String = "",
    val challenge: AuthChallenge? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(AuthUiState())

    val stateFlow: StateFlow<AuthUiState> = combine(
        formState,
        sessionRepository.sessionState,
    ) { form, session ->
        form.copy(
            challenge = session.challenge,
            isLoading = session.isLoading,
            errorMessage = form.errorMessage ?: session.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(),
    )

    fun onWalletAddressChanged(value: String) {
        formState.update {
            it.copy(
                walletAddress = value,
                errorMessage = null,
            )
        }
        clearRepositoryError()
    }

    fun onSignatureChanged(value: String) {
        formState.update {
            it.copy(
                signature = value,
                errorMessage = null,
            )
        }
        clearRepositoryError()
    }

    fun onDeviceTokenChanged(value: String) {
        formState.update { it.copy(deviceToken = value) }
    }

    fun setLocalError(message: String) {
        formState.update { it.copy(errorMessage = message) }
    }

    fun requestNonce(walletAddress: WalletAddress) {
        formState.update {
            it.copy(
                walletAddress = walletAddress.value,
                errorMessage = null,
            )
        }
        clearRepositoryError()
        viewModelScope.launch {
            sessionRepository.requestNonce(walletAddress)
        }
    }

    fun connectWallet() {
        val currentState = stateFlow.value
        val challenge = currentState.challenge
        val walletAddress = runCatching { WalletAddress(currentState.walletAddress.trim()) }.getOrNull()
        val validationError = when {
            challenge == null -> "Request a nonce before verifying the wallet signature."
            walletAddress == null -> "Enter a valid wallet address."
            currentState.signature.isBlank() -> "Paste the signed WalletConnect message before connecting."
            else -> null
        }

        if (validationError != null) {
            setLocalError(validationError)
        } else {
            clearRepositoryError()
            formState.update { it.copy(errorMessage = null) }
            viewModelScope.launch {
                sessionRepository.authenticate(
                    ConnectWalletRequest(
                        walletAddress = walletAddress!!,
                        signature = currentState.signature.trim(),
                        nonce = challenge!!.nonce,
                        message = challenge.message,
                        deviceToken = currentState.deviceToken.ifBlank { null },
                    ),
                )
            }
        }
    }

    private fun clearRepositoryError() {
        viewModelScope.launch {
            sessionRepository.clearError()
        }
    }
}

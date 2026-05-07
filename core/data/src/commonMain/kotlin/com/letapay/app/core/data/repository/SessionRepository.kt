/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repository

import com.letapay.app.core.model.auth.AuthChallenge
import com.letapay.app.core.model.auth.WalletSession
import com.letapay.app.core.model.blockchain.WalletAddress
import kotlinx.coroutines.flow.StateFlow

data class SessionState(
    val isBootstrapped: Boolean = false,
    val isLoading: Boolean = false,
    val session: WalletSession? = null,
    val challenge: AuthChallenge? = null,
    val errorMessage: String? = null,
)

data class ConnectWalletRequest(
    val walletAddress: WalletAddress,
    val signature: String,
    val nonce: String,
    val message: String? = null,
    val deviceToken: String? = null,
    val walletProvider: String = "walletconnect",
)

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun bootstrap()

    suspend fun requestNonce(walletAddress: WalletAddress)

    suspend fun authenticate(request: ConnectWalletRequest)

    suspend fun refreshSession()

    suspend fun clearError()

    suspend fun logout()
}

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repositoryImpl

import com.letapay.app.core.data.repository.FirebaseAuthRepository
import com.letapay.app.core.data.repository.FirebaseAuthState
import com.letapay.app.core.network.firebase.FirebaseAuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

class FirebaseAuthRepositoryImpl(
    private val firebaseAuthApi: FirebaseAuthApi,
) : FirebaseAuthRepository {
    private val mutableAuthState = MutableStateFlow(FirebaseAuthState())

    override val authState: StateFlow<FirebaseAuthState> = mutableAuthState.asStateFlow()

    override suspend fun signIn(
        customToken: String,
        walletAddress: String,
        sessionId: String?,
    ) {
        runCatching {
            firebaseAuthApi.signInWithCustomToken(customToken)
        }.onSuccess { session ->
            val now = Clock.System.now().toEpochMilliseconds()
            mutableAuthState.update {
                it.copy(
                    isAuthenticated = true,
                    walletAddress = walletAddress,
                    sessionId = sessionId,
                    idToken = session.idToken,
                    refreshToken = session.refreshToken,
                    expiresAtEpochMillis = now + (session.expiresInSeconds * 1000L),
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            mutableAuthState.update {
                FirebaseAuthState(
                    walletAddress = walletAddress,
                    sessionId = sessionId,
                    errorMessage = throwable.message ?: "Unable to establish Firebase custom auth.",
                )
            }
        }
    }

    override suspend fun signOut() {
        mutableAuthState.value = FirebaseAuthState()
    }
}

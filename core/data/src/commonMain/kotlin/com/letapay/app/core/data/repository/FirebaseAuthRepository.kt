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

import kotlinx.coroutines.flow.StateFlow

data class FirebaseAuthState(
    val isAuthenticated: Boolean = false,
    val walletAddress: String? = null,
    val sessionId: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val errorMessage: String? = null,
)

interface FirebaseAuthRepository {
    val authState: StateFlow<FirebaseAuthState>

    suspend fun signIn(
        customToken: String,
        walletAddress: String,
        sessionId: String? = null,
    )

    suspend fun signOut()
}

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class NonceRequest(
    val walletAddress: String,
)

@Serializable
data class NonceResponse(
    val nonce: String,
    val expiresAt: Long,
)

data class GeneratedNonce(
    val nonce: String,
    val expiresAt: Long,
)

@Serializable
data class VerifyRequest(
    val walletAddress: String,
    val message: String,
    val signature: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class FirebaseTokenResponse(
    val firebaseToken: String,
)

@Serializable
data class AuthTokens(
    val sessionToken: String,
    val refreshToken: String,
    val firebaseToken: String,
    val expiresAt: Long,
)

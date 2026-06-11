/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.auth

import com.letapay.app.core.common.buildSiweMessage
import com.letapay.app.core.model.auth.AuthChallenge
import com.letapay.app.core.model.auth.WalletSession
import com.letapay.app.core.model.blockchain.WalletAddress
import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class AuthApi(
    private val client: HttpClient,
) {
    suspend fun requestNonce(walletAddress: WalletAddress): AuthChallenge {
        val response = client.post("/auth/request-nonce") {
            setBody(AuthNonceRequest(walletAddress = walletAddress.value))
        }.bodyOrThrow<AuthNonceResponse>()

        return AuthChallenge(
            walletAddress = walletAddress,
            nonce = response.nonce,
            message = buildSignInMessage(
                walletAddress = walletAddress.value,
                nonce = response.nonce,
            ),
            expiresAtEpochMillis = response.expiresAtEpochMillis,
        )
    }

    suspend fun verifySignature(request: VerifySignatureRequest): WalletSession {
        val response = client.post("/auth/verify-signature") {
            setBody(request.toPayload())
        }.bodyOrThrow<AuthTokensResponse>()
        return response.toModel(request.walletAddress)
    }

    suspend fun refreshSession(
        refreshToken: String,
        walletAddress: WalletAddress,
        deviceFingerprint: String? = null,
    ): WalletSession {
        val response = client.post("/auth/refresh-token") {
            if (!deviceFingerprint.isNullOrBlank()) {
                header("X-Device-Fingerprint", deviceFingerprint)
            }
            setBody(RefreshTokenRequest(refreshToken = refreshToken))
        }.bodyOrThrow<AuthTokensResponse>()
        return response.toModel(walletAddress)
    }

    suspend fun fetchFirebaseToken(sessionToken: String): String {
        val response = client.post("/auth/firebase-token") {
            bearerAuth(sessionToken)
        }.bodyOrThrow<FirebaseTokenPayload>()
        return response.firebaseToken
    }

    suspend fun revokeSession(sessionToken: String) {
        client.post("/auth/revoke-session") {
            bearerAuth(sessionToken)
        }
    }
}

data class VerifySignatureRequest(
    val walletAddress: WalletAddress,
    val signature: String,
    val nonce: String,
    val message: String? = null,
)

@Serializable
private data class AuthNonceRequest(
    val walletAddress: String,
)

@Serializable
private data class AuthNonceResponse(
    val nonce: String,
    @SerialName("expiresAt")
    val expiresAtEpochMillis: Long? = null,
)

@Serializable
private data class VerifySignaturePayload(
    val walletAddress: String,
    val signature: String,
    val message: String? = null,
)

@Serializable
private data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
private data class AuthTokensResponse(
    val sessionToken: String,
    val refreshToken: String,
    val firebaseToken: String,
    @SerialName("expiresAt")
    val expiresAtEpochMillis: Long,
)

@Serializable
private data class FirebaseTokenPayload(
    val firebaseToken: String,
)

private fun VerifySignatureRequest.toPayload(): VerifySignaturePayload = VerifySignaturePayload(
    walletAddress = walletAddress.value,
    signature = signature,
    message = message,
)

private fun AuthTokensResponse.toModel(walletAddress: WalletAddress): WalletSession = WalletSession(
    walletAddress = walletAddress,
    sessionToken = sessionToken,
    refreshToken = refreshToken,
    firebaseToken = firebaseToken,
    createdAtEpochMillis = expiresAtEpochMillis,
)

private fun buildSignInMessage(
    walletAddress: String,
    nonce: String,
): String = buildSiweMessage(
    domain = DEFAULT_SIWE_DOMAIN,
    address = walletAddress,
    uri = DEFAULT_SIWE_URI,
    nonce = nonce,
    chainId = DEFAULT_SIWE_CHAIN_ID,
    issuedAt = Clock.System.now(),
    statement = DEFAULT_SIWE_STATEMENT,
)

private const val DEFAULT_SIWE_DOMAIN = "letapay.app"
private const val DEFAULT_SIWE_URI = "https://letapay.app"
private const val DEFAULT_SIWE_CHAIN_ID = 1
private const val DEFAULT_SIWE_STATEMENT = "Sign in to Leta Pay"

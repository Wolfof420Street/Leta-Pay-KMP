/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.firebase

import com.letapay.app.core.network.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

data class FirebaseSession(
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

class FirebaseAuthApi(
    private val client: HttpClient,
    private val appConfig: AppConfig,
) {
    suspend fun signInWithCustomToken(customToken: String): FirebaseSession {
        val response = client.post(
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${appConfig.firebaseApiKey}",
        ) {
            setBody(
                FirebaseCustomTokenRequest(
                    token = customToken,
                    returnSecureToken = true,
                ),
            )
        }.body<FirebaseCustomTokenResponse>()

        return FirebaseSession(
            idToken = response.idToken,
            refreshToken = response.refreshToken,
            expiresInSeconds = response.expiresIn.toLongOrNull() ?: 0L,
        )
    }
}

@Serializable
private data class FirebaseCustomTokenRequest(
    val token: String,
    val returnSecureToken: Boolean,
)

@Serializable
private data class FirebaseCustomTokenResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
)

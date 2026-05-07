/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.chat

import com.letapay.app.core.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class RemoteContact(
    val walletAddress: String,
    val alias: String? = null,
    val isFavorite: Boolean = false,
    val lastInteractionAt: Long? = null,
    val source: String = "server",
)

class ContactsApi(
    private val client: HttpClient,
) {
    suspend fun getContacts(sessionToken: String): List<RemoteContact> {
        return client.get("/contacts") {
            bearerAuth(sessionToken)
        }.bodyOrThrow<List<RemoteContact>>()
    }
}

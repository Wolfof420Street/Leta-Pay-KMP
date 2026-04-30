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

import com.letapay.app.core.network.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
data class RemoteChatMessage(
    val id: String,
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: Long,
)

class FirebaseChatApi(
    private val client: HttpClient,
    private val appConfig: AppConfig,
) {
    suspend fun upsertThreadMember(
        idToken: String,
        threadId: String,
        walletAddress: String,
    ) {
        client.put("${appConfig.realtimeDatabaseUrl}/threadMembers/$threadId/$walletAddress.json") {
            parameter("auth", idToken)
            setBody(true)
        }
    }

    suspend fun sendMessage(
        idToken: String,
        threadId: String,
        message: RemoteChatMessage,
    ) {
        client.put("${appConfig.realtimeDatabaseUrl}/threads/$threadId/messages/${message.id}.json") {
            parameter("auth", idToken)
            setBody(message)
        }
    }

    suspend fun fetchMessages(
        idToken: String,
        threadId: String,
    ): List<RemoteChatMessage> {
        val response = client.get("${appConfig.realtimeDatabaseUrl}/threads/$threadId/messages.json") {
            parameter("auth", idToken)
        }.body<Map<String, RemoteChatMessage>?>()

        return response
            .orEmpty()
            .values
            .sortedBy(RemoteChatMessage::timestamp)
    }
}

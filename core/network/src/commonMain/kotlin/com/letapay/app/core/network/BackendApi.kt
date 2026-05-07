/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@PublishedApi
internal val backendJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
data class BackendErrorEnvelope(
    val code: String,
    val message: String,
    @SerialName("retryAfter")
    val retryAfterSeconds: Int? = null,
)

class BackendApiException(
    val statusCode: Int,
    val code: String,
    override val message: String,
    val retryAfterSeconds: Int? = null,
) : Exception(message)

suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (status.isSuccess()) {
        return body()
    }

    val errorBody = runCatching { body<String>() }.getOrNull()
    val envelope = errorBody
        ?.takeIf(String::isNotBlank)
        ?.let { payload -> runCatching { backendJson.decodeFromString<BackendErrorEnvelope>(payload) }.getOrNull() }

    throw BackendApiException(
        statusCode = status.value,
        code = envelope?.code ?: "HTTP_${status.value}",
        message = envelope?.message ?: "Request failed with status ${status.value}.",
        retryAfterSeconds = envelope?.retryAfterSeconds,
    )
}

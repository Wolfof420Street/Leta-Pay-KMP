/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.error.AddressRejectedError
import com.letapay.backend.error.CoinbaseApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ScreeningService {
    suspend fun check(address: String): Boolean
}

class CoinbaseScreeningService(
    private val httpClient: HttpClient,
    private val circuitBreaker: CircuitBreaker,
    private val baseUrl: String = "https://api.coinbase.com",
) : ScreeningService {
    private val riskKey: String = System.getenv("COINBASE_RISK_KEY").orEmpty()

    override suspend fun check(address: String): Boolean {
        if (riskKey.isBlank()) {
            return true
        }
        val response = runCatching {
            circuitBreaker.execute {
                httpClient.post("$baseUrl/api/v1/risk/address") {
                    header("Authorization", "Bearer $riskKey")
                    contentType(ContentType.Application.Json)
                    setBody(CoinbaseRiskRequest(address = address))
                }.body<CoinbaseRiskResponse>()
            }
        }.getOrElse {
            throw CoinbaseApiError(message = "Risk screening request failed.")
        }

        val flagged = response.flagged || response.riskLevel.equals("high", ignoreCase = true)
        if (flagged) {
            throw AddressRejectedError()
        }
        return true
    }
}

@Serializable
private data class CoinbaseRiskRequest(
    val address: String,
)

@Serializable
private data class CoinbaseRiskResponse(
    val flagged: Boolean = false,
    @SerialName("risk_level") val riskLevel: String = "unknown",
)

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.client

import com.letapay.backend.error.CoinbaseApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val CDP_BASE_URL = "https://api.coinbase.com"
private const val FALLBACK_SWAP_BASE_URL = "https://api.0x.org"

class CoinbaseCdpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = CDP_BASE_URL,
    private val fallbackSwapBaseUrl: String = FALLBACK_SWAP_BASE_URL,
) {
    private val apiKey: String = System.getenv("COINBASE_API_KEY").orEmpty()

    suspend fun fetchOnchainTransactionHistory(walletAddress: String, chainId: Long): List<OnchainTxDto> {
        requireApiKey("onchain history")
        val response = httpClient.get("$baseUrl/api/v2/accounts/$walletAddress/transactions") {
            header("Authorization", "Bearer $apiKey")
            header("X-Chain-Id", chainId.toString())
            accept(ContentType.Application.Json)
        }
        return response.body<CdpHistoryResponse>().data
    }

    suspend fun fetchOnchainTxStatus(txHash: String, chainId: Long): CdpTxStatusDto {
        requireApiKey("transaction status")
        val response = httpClient.get("$baseUrl/api/v1/onchain/transactions/$txHash") {
            header("Authorization", "Bearer $apiKey")
            header("X-Chain-Id", chainId.toString())
            accept(ContentType.Application.Json)
        }
        return response.body<CdpTxStatusDto>()
    }

    suspend fun requestSwapRoute(request: CdpSwapQuoteRequest): CdpSwapQuoteResponse {
        requireApiKey("swap quote")
        val primary = runCatching {
            httpClient.post("$baseUrl/api/v1/swap/quote") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CdpSwapQuoteResponse>()
        }

        primary.getOrNull()?.let { return it }

        val fallbackResponse = runCatching {
            httpClient.get("$fallbackSwapBaseUrl/swap/v1/quote") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                url {
                    parameters.append("buyToken", request.toToken)
                    parameters.append("sellToken", request.fromToken)
                    parameters.append("sellAmount", request.fromAmount)
                    parameters.append("slippagePercentage", (request.slippageBps.toDouble() / 10_000.0).toString())
                }
            }.body<ZeroXSwapQuoteResponse>()
        }.getOrElse { cause ->
            throw CoinbaseApiError(
                message = "Swap quote unavailable from Coinbase and fallback router: ${cause.message}",
            )
        }

        return CdpSwapQuoteResponse(
            quoteId = fallbackResponse.price,
            toAmount = fallbackResponse.buyAmount,
            rate = fallbackResponse.price,
            priceImpactBps = 0,
            estimatedFeeUsd = fallbackResponse.estimatedGas,
            expiresAt = System.currentTimeMillis() + 60_000L,
            calldata = fallbackResponse.data,
        )
    }

    private fun requireApiKey(operation: String) {
        if (apiKey.isBlank()) {
            throw CoinbaseApiError(message = "COINBASE_API_KEY is required for $operation.")
        }
    }
}

@Serializable
data class OnchainTxDto(
    val hash: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CdpHistoryResponse(
    val data: List<OnchainTxDto> = emptyList(),
)

@Serializable
data class CdpTxStatusDto(
    val hash: String,
    val status: String,
)

@Serializable
data class CdpSwapQuoteRequest(
    @SerialName("from_token") val fromToken: String,
    @SerialName("to_token") val toToken: String,
    @SerialName("from_amount") val fromAmount: String,
    @SerialName("chain_id") val chainId: Long,
    @SerialName("slippage_bps") val slippageBps: Int,
)

@Serializable
data class CdpSwapQuoteResponse(
    @SerialName("quote_id") val quoteId: String,
    @SerialName("to_amount") val toAmount: String,
    val rate: String,
    @SerialName("price_impact_bps") val priceImpactBps: Int,
    @SerialName("estimated_fee_usd") val estimatedFeeUsd: String? = null,
    @SerialName("expires_at") val expiresAt: Long,
    val calldata: String,
)

@Serializable
private data class ZeroXSwapQuoteResponse(
    val price: String,
    @SerialName("buyAmount") val buyAmount: String,
    @SerialName("estimatedGas") val estimatedGas: String,
    val data: String,
)

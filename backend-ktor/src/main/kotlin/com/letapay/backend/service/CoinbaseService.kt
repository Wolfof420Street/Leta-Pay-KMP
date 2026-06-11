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

import com.letapay.backend.client.CdpSwapQuoteRequest
import com.letapay.backend.client.CoinbaseCdpClient
import com.letapay.backend.error.CoinbaseApiError
import com.letapay.backend.model.swap.SpotPrice
import com.letapay.backend.model.swap.SwapQuote
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.swap.UnsignedSwapTx
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class TxStatus(
    val confirmed: Boolean,
    val failed: Boolean,
    val networkName: String,
)

interface CoinbaseService {
    suspend fun getSpotPrice(fromAsset: String, toAsset: String, chain: Long): SpotPrice

    suspend fun getSwapQuote(request: SwapQuoteRequest): SwapQuote

    suspend fun getSwapUnsignedTx(quoteId: String): UnsignedSwapTx

    suspend fun getTxStatus(txHash: String, chain: Long): TxStatus
}

class DefaultCoinbaseService(
    private val httpClient: HttpClient,
    private val priceCache: PriceCache,
    private val circuitBreaker: CircuitBreaker,
) : CoinbaseService {
    private val cdpClient = CoinbaseCdpClient(httpClient)
    private val json = Json { ignoreUnknownKeys = true }
    var spotPriceFetchCount = 0
    var failSpotPrice = false

    override suspend fun getSpotPrice(fromAsset: String, toAsset: String, chain: Long): SpotPrice {
        val cacheKey = "$chain:${fromAsset.uppercase()}:${toAsset.uppercase()}"
        priceCache.get(cacheKey)?.let { cachedPrice ->
            return SpotPrice(
                fromAsset = fromAsset,
                toAsset = toAsset,
                chain = chain,
                price = cachedPrice,
            )
        }

        return try {
            circuitBreaker.execute {
                if (failSpotPrice) {
                    throw CoinbaseApiError(message = "Spot price unavailable.")
                }
                spotPriceFetchCount += 1
                val product = "${fromAsset.uppercase()}-${toAsset.uppercase()}"
                val response = httpClient.get("https://api.coinbase.com/v2/prices/$product/spot") {
                    accept(ContentType.Application.Json)
                }.bodyAsText().let { payload ->
                    json.decodeFromString<CoinbaseSpotPriceResponse>(payload)
                }
                SpotPrice(fromAsset = fromAsset, toAsset = toAsset, chain = chain, price = response.data.amount)
                    .also { priceCache.put(cacheKey, it.price) }
            }
        } catch (exception: Exception) {
            val stale = priceCache.getEntry(cacheKey)?.price
                ?: throw exception
            SpotPrice(
                fromAsset = fromAsset,
                toAsset = toAsset,
                chain = chain,
                price = stale,
            )
        }
    }

    override suspend fun getSwapQuote(request: SwapQuoteRequest): SwapQuote {
        return try {
            circuitBreaker.execute {
                val response = cdpClient.requestSwapRoute(
                    CdpSwapQuoteRequest(
                        fromToken = request.fromAsset,
                        toToken = request.toAsset,
                        fromAmount = request.amount,
                        chainId = request.chain,
                        slippageBps = request.slippageBps,
                    ),
                )
                val toAmount = response.toAmount.takeIf { it.isNotBlank() }
                    ?: throw CoinbaseApiError(message = "Swap quote response did not contain to_amount.")
                SwapQuote(
                    quoteId = response.quoteId.ifBlank { throw CoinbaseApiError(message = "Swap quote id missing.") },
                    fromAsset = request.fromAsset,
                    toAsset = request.toAsset,
                    fromAmount = request.amount,
                    toAmount = toAmount,
                    rate = response.rate,
                    priceImpactBps = response.priceImpactBps,
                    estimatedFeeUsd = response.estimatedFeeUsd ?: "0.00",
                    expiresAt = response.expiresAt,
                    calldata = response.calldata,
                    chainId = request.chain,
                    slippageBps = request.slippageBps,
                )
            }
        } catch (exception: NumberFormatException) {
            throw CoinbaseApiError(message = "Invalid swap amount.")
        }
    }

    override suspend fun getSwapUnsignedTx(quoteId: String): UnsignedSwapTx {
        if (quoteId.isBlank()) {
            throw CoinbaseApiError(message = "Quote id is required.")
        }
        throw CoinbaseApiError(message = "Direct Coinbase unsigned swap fetch is not supported by this backend path.")
    }

    override suspend fun getTxStatus(txHash: String, chain: Long): TxStatus =
        circuitBreaker.execute {
            val response = cdpClient.fetchOnchainTxStatus(txHash, chain)
            val normalizedStatus = response.status.lowercase()
            TxStatus(
                confirmed = normalizedStatus in setOf("confirmed", "complete", "succeeded", "success"),
                failed = normalizedStatus in setOf("failed", "reverted", "error"),
                networkName = when (chain) {
                    137L -> "Polygon"
                    8453L -> "Base"
                    else -> "Ethereum"
                },
            )
        }
}

@Serializable
private data class CoinbaseSpotPriceResponse(
    val data: CoinbaseSpotPricePayload,
)

@Serializable
private data class CoinbaseSpotPricePayload(
    @SerialName("amount") val amount: String,
)

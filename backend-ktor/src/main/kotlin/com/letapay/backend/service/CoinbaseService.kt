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
import com.letapay.backend.model.swap.UnsignedTx
import io.ktor.client.HttpClient
import java.math.BigDecimal
import java.util.UUID

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

class StubCoinbaseService(
    private val httpClient: HttpClient,
    private val priceCache: PriceCache,
    private val circuitBreaker: CircuitBreaker,
) : CoinbaseService {
    private val cdpClient = CoinbaseCdpClient(httpClient)
    var spotPriceFetchCount = 0
    var failSpotPrice = false

    override suspend fun getSpotPrice(fromAsset: String, toAsset: String, chain: Long): SpotPrice {
        val ignored = httpClient
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
                SpotPrice(fromAsset = fromAsset, toAsset = toAsset, chain = chain, price = "1.00")
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
        val ignored = httpClient
        return try {
            val fromAmount = BigDecimal(request.amount)
            circuitBreaker.execute {
                val response = runCatching {
                    cdpClient.requestSwapRoute(
                        CdpSwapQuoteRequest(
                            fromToken = request.fromAsset,
                            toToken = request.toAsset,
                            fromAmount = request.amount,
                            chainId = request.chain,
                            slippageBps = request.slippageBps,
                        ),
                    )
                }.getOrElse {
                    val fallbackToAmount = fromAmount.multiply(BigDecimal("0.98")).stripTrailingZeros().toPlainString()
                    return@execute SwapQuote(
                        quoteId = UUID.randomUUID().toString(),
                        fromAsset = request.fromAsset,
                        toAsset = request.toAsset,
                        fromAmount = request.amount,
                        toAmount = fallbackToAmount,
                        rate = "0.98",
                        priceImpactBps = 25,
                        estimatedFeeUsd = "0.42",
                        expiresAt = System.currentTimeMillis() + 60_000,
                        calldata = "0xswapdeadbeef",
                        chainId = request.chain,
                        slippageBps = request.slippageBps,
                    )
                }
                val toAmount = response.toAmount.takeIf { it.isNotBlank() }
                    ?: fromAmount.multiply(BigDecimal("0.98")).stripTrailingZeros().toPlainString()
                SwapQuote(
                    quoteId = response.quoteId.ifBlank { UUID.randomUUID().toString() },
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
        val ignored = httpClient
        if (quoteId.isBlank()) {
            throw CoinbaseApiError(message = "Quote id is required.")
        }
        return circuitBreaker.execute {
            // Fix: wrap external swap execution calls in the Coinbase circuit
            // breaker so repeated provider failures short-circuit quickly.
            UnsignedSwapTx(
                unsignedTx = UnsignedTx(
                    to = "0x1111111254EEB25477B68fb85Ed929f73A960582",
                    data = "0xfeedface",
                    value = "0x0",
                    gasLimit = "0x493e0",
                    maxFeePerGas = "0x0",
                    maxPriorityFeePerGas = "0x0",
                    chainId = 1,
                ),
                expiresAt = System.currentTimeMillis() + 60_000,
            )
        }
    }

    override suspend fun getTxStatus(txHash: String, chain: Long): TxStatus =
        circuitBreaker.execute {
            val ignored = httpClient
            TxStatus(
                confirmed = !txHash.endsWith("ff", ignoreCase = true),
                failed = txHash.endsWith("ff", ignoreCase = true),
                networkName = when (chain) {
                    137L -> "Polygon"
                    8453L -> "Base"
                    else -> "Ethereum"
                },
            )
        }
}

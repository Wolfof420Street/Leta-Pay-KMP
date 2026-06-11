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

import com.letapay.backend.model.swap.SwapQuote
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.swap.UnsignedTx
import com.letapay.backend.model.yield.StakeRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.MDC
import java.util.UUID

private const val DEV_SWAP_ROUTER = "0x1111111254EEB25477B68fb85Ed929f73A960582"

interface AgentKitClient {
    suspend fun buildTransfer(
        fromAddress: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): Result<UnsignedTx>

    suspend fun getSwapQuote(fromAddress: String, request: SwapQuoteRequest): Result<SwapQuote>

    suspend fun buildSwap(fromAddress: String, quote: SwapQuote): Result<UnsignedTx>

    suspend fun buildStake(fromAddress: String, request: StakeRequest, chainId: Long): Result<UnsignedTx>
}

class StaticAgentKitFallbackClient : AgentKitClient {
    override suspend fun buildTransfer(
        fromAddress: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): Result<UnsignedTx> = Result.success(
        UnsignedTx(
            to = toAddress,
            data = run {
                val assetUp = asset.uppercase()
                val digitsOnly = amount.filter(Char::isDigit).ifBlank { "0" }
                val transferData = "0xtransfer${assetUp}$digitsOnly"
                transferData
            },
            value = if (asset.equals("ETH", ignoreCase = true)) amount else "0x0",
            gasLimit = "0x5208",
            maxFeePerGas = "0x3b9aca00",
            maxPriorityFeePerGas = "0x3b9aca00",
            chainId = chainId,
        ),
    )

    override suspend fun getSwapQuote(
        fromAddress: String,
        request: SwapQuoteRequest,
    ): Result<SwapQuote> = Result.success(
        SwapQuote(
            quoteId = UUID.randomUUID().toString(),
            fromAsset = request.fromAsset,
            toAsset = request.toAsset,
            fromAmount = request.amount,
            toAmount = request.amount,
            rate = "1.0",
            priceImpactBps = minOf(request.slippageBps, 25),
            estimatedFeeUsd = "0.25",
            expiresAt = System.currentTimeMillis() + 60_000L,
            calldata = "0xswapdeadbeef",
            chainId = request.chain,
            slippageBps = request.slippageBps,
        ),
    )

    override suspend fun buildSwap(fromAddress: String, quote: SwapQuote): Result<UnsignedTx> = Result.success(
        UnsignedTx(
            to = DEV_SWAP_ROUTER,
            data = quote.calldata,
            value = "0x0",
            gasLimit = "0x493e0",
            maxFeePerGas = "0x3b9aca00",
            maxPriorityFeePerGas = "0x3b9aca00",
            chainId = quote.chainId,
        ),
    )

    override suspend fun buildStake(
        fromAddress: String,
        request: StakeRequest,
        chainId: Long,
    ): Result<UnsignedTx> = Result.success(
        UnsignedTx(
            to = "0x000000000000000000000000000000000000beef",
            data = "0xstake${request.opportunityId.takeLast(8)}",
            value = request.amount,
            gasLimit = "0x7a120",
            maxFeePerGas = "0x3b9aca00",
            maxPriorityFeePerGas = "0x3b9aca00",
            chainId = chainId,
        ),
    )
}

class SidecarAgentKitClient(
    private val httpClient: HttpClient,
    private val sidecarUrl: String,
    private val sidecarSecret: String,
    private val circuitBreaker: CircuitBreaker,
) : AgentKitClient {

    override suspend fun buildTransfer(
        fromAddress: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): Result<UnsignedTx> = runCatching {
        circuitBreaker.execute {
            httpClient.post("$sidecarUrl/agentkit/transfer/build") {
                withSidecarAuth()
                setBody(
                    mapOf(
                        "fromAddress" to fromAddress,
                        "toAddress" to toAddress,
                        "asset" to asset.uppercase(),
                        "amount" to amount,
                        "networkId" to chainId.toNetworkId(),
                    ),
                )
            }.body<SidecarCalldataResponse>().calldata.toUnsignedTx(defaultChainId = chainId)
        }
    }

    override suspend fun getSwapQuote(fromAddress: String, request: SwapQuoteRequest): Result<SwapQuote> = runCatching {
        circuitBreaker.execute {
            val response = httpClient.post("$sidecarUrl/agentkit/swap/quote") {
                withSidecarAuth()
                setBody(
                    mapOf(
                        "fromAddress" to fromAddress,
                        "fromAsset" to request.fromAsset,
                        "toAsset" to request.toAsset,
                        "amount" to request.amount,
                        "networkId" to request.chain.toNetworkId(),
                        "slippageBps" to request.slippageBps,
                    ),
                )
            }.body<SidecarQuoteResponse>()

            val quote = response.quote
            val quoteId = requireNotNull(quote.string("quoteId")) { "Missing quoteId in sidecar quote response" }
            val expiresAt = requireNotNull(quote.long("expiresAt")) { "Missing expiresAt in sidecar quote response" }
            val calldata = requireNotNull(quote.string("calldata")) { "Missing calldata in sidecar quote response" }
            SwapQuote(
                quoteId = quoteId,
                fromAsset = request.fromAsset,
                toAsset = request.toAsset,
                fromAmount = quote.string("fromAmount") ?: request.amount,
                toAmount = quote.string("toAmount") ?: request.amount,
                rate = quote.string("rate") ?: "1.0",
                priceImpactBps = quote.int("priceImpactBps") ?: 25,
                estimatedFeeUsd = quote.string("estimatedFeeUsd") ?: "0.12",
                expiresAt = expiresAt,
                calldata = calldata,
                chainId = request.chain,
                slippageBps = request.slippageBps,
            )
        }
    }

    override suspend fun buildSwap(fromAddress: String, quote: SwapQuote): Result<UnsignedTx> = runCatching {
        circuitBreaker.execute {
            httpClient.post("$sidecarUrl/agentkit/swap/build") {
                withSidecarAuth()
                setBody(
                    mapOf(
                        "fromAddress" to fromAddress,
                        "fromAsset" to quote.fromAsset,
                        "toAsset" to quote.toAsset,
                        "amount" to quote.fromAmount,
                        "networkId" to quote.chainId.toNetworkId(),
                        "slippageBps" to quote.slippageBps,
                    ),
                )
            }.body<SidecarCalldataResponse>().calldata.toUnsignedTx(
                toFallback = DEV_SWAP_ROUTER,
                defaultChainId = quote.chainId,
            )
        }
    }

    override suspend fun buildStake(fromAddress: String, request: StakeRequest, chainId: Long): Result<UnsignedTx> =
        runCatching {
            circuitBreaker.execute {
                httpClient.post("$sidecarUrl/agentkit/stake/build") {
                    withSidecarAuth()
                    setBody(
                        mapOf(
                            "fromAddress" to fromAddress,
                            "opportunityId" to request.opportunityId,
                            "amount" to request.amount,
                            "networkId" to chainId.toNetworkId(),
                        ),
                    )
                }.body<SidecarCalldataResponse>().calldata.toUnsignedTx(defaultChainId = chainId)
            }
        }

    private fun io.ktor.client.request.HttpRequestBuilder.withSidecarAuth() {
        header("X-Internal-Token", sidecarSecret)
        val requestId = MDC.get("requestId") ?: ""
        if (requestId.isNotBlank()) {
            header("X-Request-ID", requestId)
        }
        contentType(ContentType.Application.Json)
    }

    private fun Long.toNetworkId(): String =
        when (this) {
            137L -> "polygon-mainnet"
            8453L -> "base-mainnet"
            else -> "ethereum-mainnet"
        }

    private fun JsonObject.toUnsignedTx(toFallback: String? = null, defaultChainId: Long): UnsignedTx =
        UnsignedTx(
            to = string("to") ?: toFallback ?: error("Missing destination 'to' in sidecar calldata response"),
            data = string("data") ?: "0x",
            value = string("value") ?: "0x0",
            gasLimit = string("gasLimit") ?: "0x493e0",
            maxFeePerGas = string("maxFeePerGas") ?: "0x0",
            maxPriorityFeePerGas = string("maxPriorityFeePerGas") ?: "0x0",
            chainId = long("chainId") ?: defaultChainId,
            nonce = int("nonce"),
        )
}

@Serializable
private data class SidecarCalldataResponse(
    val success: Boolean = false,
    val calldata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
private data class SidecarQuoteResponse(
    val success: Boolean = false,
    val quote: JsonObject = JsonObject(emptyMap()),
)

private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.long(key: String): Long? =
    this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: this[key]?.jsonPrimitive?.doubleOrNull?.toLong()

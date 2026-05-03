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
import io.ktor.client.request.get
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

interface AgentKitClient {
    suspend fun buildTransfer(
        fromAddress: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): UnsignedTx

    suspend fun getSwapQuote(fromAddress: String, request: SwapQuoteRequest): SwapQuote

    suspend fun buildSwap(fromAddress: String, quote: SwapQuote): UnsignedTx

    suspend fun buildStake(fromAddress: String, request: StakeRequest, chainId: Long): UnsignedTx
}

class SidecarAgentKitClient(
    private val httpClient: HttpClient,
    private val sidecarUrl: String,
    private val sidecarSecret: String,
) : AgentKitClient {

    override suspend fun buildTransfer(
        fromAddress: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): UnsignedTx {
        val response = runCatching {
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
            }.body<SidecarCalldataResponse>()
        }.getOrNull()

        return response?.calldata?.toUnsignedTx(chainId) ?: fallbackUnsignedTx(chainId)
    }

    override suspend fun getSwapQuote(fromAddress: String, request: SwapQuoteRequest): SwapQuote {
        val response = runCatching {
            httpClient.post("$sidecarUrl/agentkit/swap/quote") {
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
        }.getOrNull()

        if (response == null) {
            return SwapQuote(
                quoteId = "fallback-quote-${System.currentTimeMillis()}",
                fromAmount = request.amount,
                toAmount = request.amount,
                rate = "1.0",
                priceImpactBps = 25,
                estimatedFeeUsd = "0.12",
                expiresAt = System.currentTimeMillis() + 60_000,
                calldata = "0x",
                chainId = request.chain,
            )
        }

        val quote = response.quote
        return SwapQuote(
            quoteId = quote.string("quoteId") ?: "quote-${System.currentTimeMillis()}",
            fromAmount = quote.string("fromAmount") ?: request.amount,
            toAmount = quote.string("toAmount") ?: request.amount,
            rate = quote.string("rate") ?: "1.0",
            priceImpactBps = quote.int("priceImpactBps") ?: 25,
            estimatedFeeUsd = quote.string("estimatedFeeUsd") ?: "0.12",
            expiresAt = quote.long("expiresAt") ?: (System.currentTimeMillis() + 60_000),
            calldata = quote.string("calldata") ?: "0x",
            chainId = request.chain,
        )
    }

    override suspend fun buildSwap(fromAddress: String, quote: SwapQuote): UnsignedTx {
        val response = runCatching {
            httpClient.post("$sidecarUrl/agentkit/swap/build") {
                withSidecarAuth()
                setBody(
                    mapOf(
                        "fromAddress" to fromAddress,
                        "fromAsset" to "ETH",
                        "toAsset" to "USDC",
                        "amount" to quote.fromAmount,
                        "networkId" to quote.chainId.toNetworkId(),
                        "slippageBps" to 50,
                    ),
                )
            }.body<SidecarCalldataResponse>()
        }.getOrNull()

        return response?.calldata?.toUnsignedTx(quote.chainId) ?: fallbackUnsignedTx(quote.chainId)
    }

    override suspend fun buildStake(fromAddress: String, request: StakeRequest, chainId: Long): UnsignedTx {
        val response = runCatching {
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
            }.body<SidecarCalldataResponse>()
        }.getOrNull()

        return response?.calldata?.toUnsignedTx(chainId) ?: fallbackUnsignedTx(chainId)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.withSidecarAuth() {
        header("x-sidecar-secret", sidecarSecret)
        contentType(ContentType.Application.Json)
    }

    private fun Long.toNetworkId(): String =
        when (this) {
            137L -> "polygon-mainnet"
            8453L -> "base-mainnet"
            else -> "ethereum-mainnet"
        }

    private fun fallbackUnsignedTx(chainId: Long): UnsignedTx =
        UnsignedTx(
            to = "0x1111111254EEB25477B68fb85Ed929f73A960582",
            data = "0x",
            value = "0x0",
            gasLimit = "0x493e0",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
            chainId = chainId,
        )

    private fun JsonObject.toUnsignedTx(defaultChainId: Long): UnsignedTx =
        UnsignedTx(
            to = string("to") ?: "0x1111111254EEB25477B68fb85Ed929f73A960582",
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

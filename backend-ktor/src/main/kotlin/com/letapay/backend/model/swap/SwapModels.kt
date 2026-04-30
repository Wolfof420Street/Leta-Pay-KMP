/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.model.swap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SwapQuoteRequest(
    val fromAsset: String,
    val toAsset: String,
    val amount: String,
    val chain: Long,
    val slippageBps: Int = 50,
)

@Serializable
data class SwapQuoteResponse(
    val quoteId: String,
    val fromAmount: String,
    val toAmount: String,
    val rate: String,
    val priceImpactBps: Int,
    val estimatedFeeUsd: String,
    val expiresAt: Long,
)

@Serializable
data class SwapExecuteRequest(
    val quoteId: String,
    val idempotencyKey: String,
)

@Serializable
data class SwapExecuteResponse(
    val unsignedTx: UnsignedTx,
    val expiresAt: Long,
)

@Serializable
data class UnsignedTx(
    val to: String,
    val data: String,
    val value: String,
    val gasLimit: String,
    val maxFeePerGas: String,
    val maxPriorityFeePerGas: String,
    val chainId: Long,
    val nonce: Int? = null,
)

@Serializable
data class SwapQuote(
    val quoteId: String,
    val fromAmount: String,
    val toAmount: String,
    val rate: String,
    val priceImpactBps: Int,
    val estimatedFeeUsd: String,
    val expiresAt: Long,
    val calldata: String,
    val chainId: Long = 1,
)

@Serializable
data class UnsignedSwapTx(
    val unsignedTx: UnsignedTx,
    val expiresAt: Long,
)

@Serializable
data class SpotPrice(
    val fromAsset: String,
    val toAsset: String,
    val chain: Long,
    val price: String,
)

@Serializable
internal data class CoinbaseSwapQuoteApiRequest(
    @SerialName("from_token") val fromToken: String,
    @SerialName("to_token") val toToken: String,
    @SerialName("from_amount") val fromAmount: String,
    @SerialName("chain_id") val chainId: Long,
    @SerialName("slippage_bps") val slippageBps: Int,
)

@Serializable
internal data class CoinbaseSwapQuoteApiResponse(
    @SerialName("quote_id") val quoteId: String,
    @SerialName("to_amount") val toAmount: String,
    val rate: String,
    @SerialName("price_impact_bps") val priceImpactBps: Int,
    @SerialName("estimated_fee_usd") val estimatedFeeUsd: String? = null,
    @SerialName("expires_at") val expiresAt: Long,
    val calldata: String,
)

@Serializable
internal data class CoinbaseSwapExecuteApiRequest(
    @SerialName("quote_id") val quoteId: String,
)

@Serializable
internal data class CoinbaseUnsignedTxPayload(
    val to: String,
    val data: String,
    val value: String,
    @SerialName("gas_limit") val gasLimit: String,
    @SerialName("gas_price") val gasPrice: String? = null,
    @SerialName("max_fee_per_gas") val maxFeePerGas: String? = null,
    @SerialName("max_priority_fee_per_gas") val maxPriorityFeePerGas: String? = null,
    @SerialName("chain_id") val chainId: Long? = null,
)

@Serializable
internal data class CoinbaseSwapExecuteApiResponse(
    @SerialName("unsigned_tx") val unsignedTx: CoinbaseUnsignedTxPayload,
    @SerialName("expires_at") val expiresAt: Long,
)

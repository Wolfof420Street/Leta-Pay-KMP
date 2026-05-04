/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.swap

import com.letapay.app.core.model.blockchain.UnsignedTx
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
data class SwapQuote(
    val quoteId: String,
    val fromAsset: String,
    val toAsset: String,
    val fromAmount: String,
    val toAmount: String,
    val rate: String,
    val priceImpactBps: Int,
    val estimatedFeeUsd: String,
    val expiresAt: Long,
    val calldata: String,
    val chainId: Long = 1,
    val slippageBps: Int = 50,
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

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

import com.letapay.backend.model.swap.SwapQuoteRequest
import io.ktor.server.plugins.BadRequestException

interface SwapService {
    fun validateQuoteRequest(request: SwapQuoteRequest)
}

class DefaultSwapService : SwapService {
    private val supportedChains = setOf(1L, 137L, 8453L)
    private val supportedAssets = setOf("ETH", "USDC", "USDT", "DAI", "WETH", "WBTC", "MATIC")

    override fun validateQuoteRequest(request: SwapQuoteRequest) {
        val violation = when {
            request.slippageBps !in 10..1000 -> "slippageBps must be between 10 and 1000."
            request.chain !in supportedChains -> "Unsupported swap chain: ${request.chain}."
            request.fromAsset.uppercase() !in supportedAssets ||
                request.toAsset.uppercase() !in supportedAssets -> "Swap asset is not in the MVP whitelist."
            request.amount.isBlank() -> "amount is required."
            else -> null
        }
        if (violation != null) throw BadRequestException(violation)
    }
}

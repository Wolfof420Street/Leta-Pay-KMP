/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend

import com.letapay.backend.model.price.PriceQuote
import com.letapay.backend.model.swap.SpotPrice
import com.letapay.backend.model.swap.SwapQuote
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.swap.UnsignedSwapTx
import com.letapay.backend.model.swap.UnsignedTx
import com.letapay.backend.service.AgentKitClient
import com.letapay.backend.service.CoinbaseService
import com.letapay.backend.service.FirebaseTokenService
import com.letapay.backend.service.HealthService
import com.letapay.backend.service.HealthSnapshot
import com.letapay.backend.service.PricingService
import com.letapay.backend.service.StaticAgentKitFallbackClient
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    io.ktor.server.testing.testApplication {
        environment {
            config = MapApplicationConfig("ktor.development" to "true")
        }
        block()
    }
}

fun testBackendApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication(block)
}

fun backendTestOverrides(
    healthSnapshot: HealthSnapshot = HealthSnapshot(
        status = "ok",
        db = "ok",
        redis = "ok",
        sidecar = "ok",
        firebase = "ok",
    ),
): Module = module {
    single<HealthService> {
        object : HealthService {
            override suspend fun status(): HealthSnapshot = healthSnapshot
        }
    }
    single<FirebaseTokenService> {
        object : FirebaseTokenService {
            override fun createCustomToken(walletAddress: String, sessionId: String): String =
                "firebase-token-$sessionId"
        }
    }
    single<AgentKitClient> { StaticAgentKitFallbackClient() }
    single<CoinbaseService> {
        object : CoinbaseService {
            override suspend fun getSpotPrice(fromAsset: String, toAsset: String, chain: Long): SpotPrice =
                SpotPrice(fromAsset = fromAsset, toAsset = toAsset, chain = chain, price = "2500.00")

            override suspend fun getSwapQuote(request: SwapQuoteRequest): SwapQuote =
                SwapQuote(
                    quoteId = "quote-${request.chain}",
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
                )

            override suspend fun getSwapUnsignedTx(quoteId: String): UnsignedSwapTx =
                UnsignedSwapTx(
                    unsignedTx = UnsignedTx(
                        to = "0x1111111254EEB25477B68fb85Ed929f73A960582",
                        data = "0xfeedface",
                        value = "0x0",
                        gasLimit = "0x493e0",
                        maxFeePerGas = "0x0",
                        maxPriorityFeePerGas = "0x0",
                        chainId = 1L,
                    ),
                    expiresAt = System.currentTimeMillis() + 60_000L,
                )

            override suspend fun getTxStatus(txHash: String, chain: Long) =
                com.letapay.backend.service.TxStatus(
                    confirmed = false,
                    failed = false,
                    networkName = when (chain) {
                        137L -> "Polygon"
                        8453L -> "Base"
                        else -> "Ethereum"
                    },
                )
        }
    }
    single<PricingService> {
        object : PricingService {
            override suspend fun quote(chain: String, asset: String): PriceQuote =
                PriceQuote(
                    asset = asset.uppercase(),
                    chain = chain,
                    fiatCurrency = "USD",
                    price = "2500.00",
                    stale = false,
                )
        }
    }
    single<CoroutineScope>(named("applicationScope")) {
        TestScope(UnconfinedTestDispatcher())
    }
}

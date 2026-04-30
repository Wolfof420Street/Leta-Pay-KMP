/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.routes

import com.letapay.backend.error.QuoteMismatchError
import com.letapay.backend.error.SlippageExceededError
import com.letapay.backend.error.UpstreamTimeoutError
import com.letapay.backend.middleware.enforceGlobalAndWalletRateLimit
import com.letapay.backend.middleware.idempotencyGuard
import com.letapay.backend.middleware.killSwitchGuard
import com.letapay.backend.model.swap.SwapExecuteRequest
import com.letapay.backend.model.swap.SwapExecuteResponse
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.swap.SwapQuoteResponse
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.CoinbaseService
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.SwapQuoteCacheService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

private val supportedChains = setOf(1L, 137L, 8453L)
private val supportedAssets = setOf("ETH", "USDC", "USDT", "DAI", "WETH", "WBTC", "MATIC")

fun Route.configureSwapRoutes() {
    val coinbaseSwapService by inject<CoinbaseService>()
    val idempotencyService by inject<IdempotencyService>()
    val json by inject<Json>()
    val swapQuoteCacheService by inject<SwapQuoteCacheService>()
    val rateLimiter by inject<RateLimiterService>()

    authenticate("session-auth") {
        route("/swap") {
            post("/quote") {
                call.killSwitchGuard()
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val request = call.receive<SwapQuoteRequest>()
                validateSwapQuoteRequest(request)

                val quote = withUpstreamTimeout {
                    coinbaseSwapService.getSwapQuote(request)
                }
                quote.ensureSlippageWithin(request.slippageBps)

                swapQuoteCacheService.putQuote(quote)
                call.respond(
                    SwapQuoteResponse(
                        quoteId = quote.quoteId,
                        fromAmount = quote.fromAmount,
                        toAmount = quote.toAmount,
                        rate = quote.rate,
                        priceImpactBps = quote.priceImpactBps,
                        estimatedFeeUsd = quote.estimatedFeeUsd,
                        expiresAt = quote.expiresAt,
                    ),
                )
            }

            post("/execute") {
                call.killSwitchGuard()
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val replay = call.idempotencyGuard()
                if (replay != null) {
                    // Fix: preserve the first successful execute response
                    // for idempotent retries via the shared replay store.
                    call.respondText(
                        text = replay.payload,
                        status = HttpStatusCode.fromValue(replay.statusCode),
                        contentType = ContentType.Application.Json,
                    )
                    return@post
                }

                val idempotencyKey = requireNotNull(call.request.headers["Idempotency-Key"])
                val request = call.receive<SwapExecuteRequest>()
                request.requireMatchingIdempotencyKey(idempotencyKey)

                val cachedQuote = swapQuoteCacheService.requireActiveQuote(request.quoteId)
                val unsignedTx = withUpstreamTimeout {
                    coinbaseSwapService.getSwapUnsignedTx(request.quoteId)
                }
                if (unsignedTx.unsignedTx.chainId != cachedQuote.chainId) {
                    throw QuoteMismatchError()
                }
                val response = SwapExecuteResponse(
                    unsignedTx = unsignedTx.unsignedTx,
                    expiresAt = minOf(cachedQuote.expiresAt, unsignedTx.expiresAt),
                )

                idempotencyService.complete(
                    key = idempotencyKey,
                    walletAddress = principal.walletAddress,
                    endpoint = call.request.path(),
                    statusCode = HttpStatusCode.OK.value,
                    payload = json.encodeToString(response),
                )
                call.respond(response)
            }
        }
    }
}

private fun validateSwapQuoteRequest(request: SwapQuoteRequest) {
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

private suspend fun <T> withUpstreamTimeout(block: suspend () -> T): T =
    try {
        withTimeout(8_000L) { block() }
    } catch (_: TimeoutCancellationException) {
        throw UpstreamTimeoutError()
    }

private fun com.letapay.backend.model.swap.SwapQuote.ensureSlippageWithin(slippageBps: Int) {
    if (priceImpactBps > slippageBps) {
        throw SlippageExceededError()
    }
}

private fun SwapExecuteRequest.requireMatchingIdempotencyKey(headerKey: String) {
    if (idempotencyKey != headerKey) {
        throw BadRequestException(
            "Body idempotencyKey must match the Idempotency-Key header.",
        )
    }
}

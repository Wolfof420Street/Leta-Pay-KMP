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

import com.letapay.backend.config.AppConfig
import com.letapay.backend.error.InvalidRequestError
import com.letapay.backend.middleware.enforceGlobalAndWalletRateLimit
import com.letapay.backend.middleware.idempotencyGuard
import com.letapay.backend.middleware.killSwitchGuard
import com.letapay.backend.model.yield.StakeRequest
import com.letapay.backend.model.yield.UnstakeRequest
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.AgentKitClient
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.YieldService
import com.letapay.backend.service.enforceTransactionLimit
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.configureYieldRoutes() {
    val yieldService by inject<YieldService>()
    val idempotencyService by inject<IdempotencyService>()
    val agentKitClient by inject<AgentKitClient>()
    val json by inject<Json>()
    val rateLimiter by inject<RateLimiterService>()
    val appConfig by inject<AppConfig>()

    authenticate("session-auth") {
        route("/yield") {
            get("/opportunities") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val chainParam = call.request.queryParameters["chain"]
                val chain = chainParam?.toLongOrNull()
                    ?: chainParam?.let { throw InvalidRequestError("chain must be a valid numeric chain id.") }
                call.respond(yieldService.getOpportunities(chain))
            }

            post("/stake") {
                call.killSwitchGuard()
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val replay = call.idempotencyGuard()
                if (replay != null) {
                    call.respondText(
                        text = replay.payload,
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.fromValue(replay.statusCode),
                    )
                    return@post
                }
                val request = call.receive<StakeRequest>()
                enforceTransactionLimit(request.amount, appConfig)
                call.requireMatchingIdempotencyKey(request.idempotencyKey)
                val opportunity = yieldService.requireOpportunity(request.opportunityId)
                val unsignedTx = agentKitClient.buildStake(
                    fromAddress = principal.walletAddress,
                    request = request,
                    chainId = opportunity.chain,
                ).getOrThrow()
                val position = yieldService.createStakePosition(
                    opportunityId = request.opportunityId,
                    amount = request.amount,
                    walletAddress = principal.walletAddress,
                )
                val response = position.copy(unsignedTx = unsignedTx)
                idempotencyService.complete(
                    key = request.idempotencyKey,
                    walletAddress = principal.walletAddress,
                    endpoint = call.request.path(),
                    statusCode = HttpStatusCode.OK.value,
                    payload = json.encodeToString(response),
                )
                call.respond(response)
            }

            post("/unstake") {
                call.killSwitchGuard()
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val replay = call.idempotencyGuard()
                if (replay != null) {
                    call.respondText(
                        text = replay.payload,
                        contentType = ContentType.Application.Json,
                        status = HttpStatusCode.fromValue(replay.statusCode),
                    )
                    return@post
                }
                val request = call.receive<UnstakeRequest>()
                enforceTransactionLimit(request.amount, appConfig)
                call.requireMatchingIdempotencyKey(request.idempotencyKey)
                val response = yieldService.prepareUnstake(
                    positionId = request.positionId,
                    amount = request.amount,
                    walletAddress = principal.walletAddress,
                )
                idempotencyService.complete(
                    key = request.idempotencyKey,
                    walletAddress = principal.walletAddress,
                    endpoint = call.request.path(),
                    statusCode = HttpStatusCode.OK.value,
                    payload = json.encodeToString(response),
                )
                call.respond(response)
            }

            get("/positions") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val request = com.letapay.app.core.model.PaginatedRequest(limit, offset)
                call.respond(yieldService.getPositions(principal.walletAddress, request))
            }
        }
    }
}

private fun ApplicationCall.requireMatchingIdempotencyKey(expectedKey: String) {
    if (request.headers["Idempotency-Key"] != expectedKey) {
        throw InvalidRequestError("Idempotency key header must match request body.")
    }
}

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

import com.letapay.backend.error.UpstreamTimeoutError
import com.letapay.backend.middleware.enforceGlobalAndWalletRateLimit
import com.letapay.backend.middleware.killSwitchGuard
import com.letapay.backend.model.ai.ParseRequest
import com.letapay.backend.model.ai.PlanRequest
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.AiCommandService
import com.letapay.backend.service.RateLimiterService
import io.ktor.http.ContentType
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout
import org.koin.ktor.ext.inject

fun Route.configureAiRoutes() {
    val aiCommandService by inject<AiCommandService>()
    val rateLimiter by inject<RateLimiterService>()

    route("/ai") {
        sse("/chat-stream") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                ?: call.parameters["token"]
                ?: return@sse close()
            if (token.isBlank()) {
                return@sse close()
            }

            val event = call.request.queryParameters["event"].orEmpty()
            val txHash = call.request.queryParameters["txHash"]
            aiCommandService.streamSummary(event = event, txHash = txHash).collect { chunk ->
                send(ServerSentEvent(data = chunk))
            }
        }
    }
    route("/api/ai") {
        post("/chat-stream") {
            val request = if (call.request.contentType().match(ContentType.Application.Json)) {
                call.receive<PlanRequest>()
            } else {
                PlanRequest(message = "")
            }
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                aiCommandService
                    .streamSummary(event = request.message.ifBlank { "chat" }, txHash = null)
                    .collect { chunk ->
                        write("data: $chunk\n\n")
                        flush()
                    }
            }
        }
    }

    authenticate("session-auth") {
        route("/ai") {
            post("/parse") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val request = call.receive<ParseRequest>()
                try {
                    call.respond(withTimeout(10_000L) { aiCommandService.parse(request.message) })
                } catch (_: TimeoutCancellationException) {
                    throw UpstreamTimeoutError()
                }
            }

            post("/plan") {
                // Fix: the kill switch is the first statement so planning never
                // reaches parsing or AI when value moves are frozen.
                call.killSwitchGuard()
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.enforceGlobalAndWalletRateLimit(rateLimiter, principal.walletAddress)
                val request = call.receive<PlanRequest>()
                try {
                    call.respond(withTimeout(20_000L) { aiCommandService.plan(request.message) })
                } catch (_: TimeoutCancellationException) {
                    throw UpstreamTimeoutError()
                }
            }
        }
    }
}

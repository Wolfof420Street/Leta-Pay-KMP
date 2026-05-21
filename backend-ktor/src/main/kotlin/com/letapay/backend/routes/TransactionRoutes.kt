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

import com.letapay.backend.error.InvalidTxHashError
import com.letapay.backend.middleware.enforceGlobalAndWalletRateLimit
import com.letapay.backend.middleware.idempotencyGuard
import com.letapay.backend.middleware.killSwitchGuard
import com.letapay.backend.model.transaction.BuildRequest
import com.letapay.backend.model.transaction.SendRequest
import com.letapay.backend.model.transaction.SendResponse
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.PendingNotificationService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.TransactionCommandService
import com.letapay.backend.service.TransactionService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
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

fun Route.configureTransactionRoutes() {
    val transactionCommandService by inject<TransactionCommandService>()
    val transactionService by inject<TransactionService>()
    val idempotencyService by inject<IdempotencyService>()
    val pendingNotificationService by inject<PendingNotificationService>()
    val rateLimiter by inject<RateLimiterService>()
    val json by inject<Json>()

    authenticate("session-auth") {
        route("/transactions") {
            configureBuildRoute(
                transactionCommandService = transactionCommandService,
                rateLimiter = rateLimiter,
                idempotencyService = idempotencyService,
                json = json,
            )
            configureSendRoute(
                transactionService = transactionService,
                pendingNotificationService = pendingNotificationService,
                rateLimiter = rateLimiter,
                idempotencyService = idempotencyService,
                json = json,
            )

            get("/history") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val request = com.letapay.app.core.model.PaginatedRequest(limit, offset)
                call.respond(transactionService.history(principal.walletAddress, request))
            }

            get("/status/{txHash}") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                val txHash = requireNotNull(call.parameters["txHash"])
                if (!TX_HASH_REGEX.matches(txHash)) {
                    // Fix: invalid hashes now return the required machine code
                    // instead of a generic bad-request payload.
                    throw InvalidTxHashError()
                }
                call.respond(transactionService.status(principal.walletAddress, txHash))
            }
        }
    }
}

private fun Route.configureBuildRoute(
    transactionCommandService: TransactionCommandService,
    rateLimiter: RateLimiterService,
    idempotencyService: IdempotencyService,
    json: Json,
) {
    post("/build") {
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

        val request = call.receive<BuildRequest>()
        val chainId = call.request.headers["X-Chain-Id"]?.toLongOrNull() ?: 1L
        val response = transactionCommandService.build(
            walletAddress = principal.walletAddress,
            request = request,
            chainId = chainId,
        )
        idempotencyService.complete(
            key = call.request.headers["Idempotency-Key"].orEmpty(),
            walletAddress = principal.walletAddress,
            endpoint = call.request.path(),
            statusCode = HttpStatusCode.OK.value,
            payload = json.encodeToString(response),
        )
        call.respond(response)
    }
}

private fun Route.configureSendRoute(
    transactionService: TransactionService,
    pendingNotificationService: PendingNotificationService,
    rateLimiter: RateLimiterService,
    idempotencyService: IdempotencyService,
    json: Json,
) {
    post("/send") {
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

        val request = call.receive<SendRequest>()
        val headers = call.request
        val submitted = transactionService.recordSubmitted(
            walletAddress = principal.walletAddress,
            toAddress = headers.requiredOrDefault("X-To-Address", ZERO_ADDRESS),
            signedTx = request.signedTx,
            asset = headers.requiredOrDefault("X-Asset", "ETH"),
            amount = headers.requiredOrDefault("X-Amount", "0"),
            chainId = headers.chainIdOrDefault(),
        )
        pendingNotificationService.register(
            walletAddress = principal.walletAddress,
            txHash = submitted.txHash,
            chain = headers.chainIdOrDefault(),
            asset = headers.requiredOrDefault("X-Asset", "ETH"),
            amount = headers.requiredOrDefault("X-Amount", "0"),
            notificationType = "TX_CONFIRMED",
        )
        val response = SendResponse(
            txHash = submitted.txHash,
            status = submitted.status,
        )
        idempotencyService.complete(
            key = call.request.headers["Idempotency-Key"].orEmpty(),
            walletAddress = principal.walletAddress,
            endpoint = call.request.path(),
            statusCode = HttpStatusCode.OK.value,
            payload = json.encodeToString(response),
        )
        call.respond(response)
    }
}

private fun ApplicationRequest.requiredOrDefault(header: String, default: String): String =
    headers[header] ?: default

private fun ApplicationRequest.chainIdOrDefault(): Long =
    headers["X-Chain-Id"]?.toLongOrNull() ?: 1L

private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

private val TX_HASH_REGEX = Regex("^0x[a-fA-F0-9]{64}$")

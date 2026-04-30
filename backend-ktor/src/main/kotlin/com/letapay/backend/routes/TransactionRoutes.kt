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

import com.letapay.backend.error.AddressRejectedError
import com.letapay.backend.error.InvalidTxHashError
import com.letapay.backend.middleware.enforceGlobalAndWalletRateLimit
import com.letapay.backend.middleware.idempotencyGuard
import com.letapay.backend.middleware.killSwitchGuard
import com.letapay.backend.model.transaction.BuildRequest
import com.letapay.backend.model.transaction.BuildResponse
import com.letapay.backend.model.transaction.SendRequest
import com.letapay.backend.model.transaction.SendResponse
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.PendingNotificationService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.ScreeningService
import com.letapay.backend.service.TransactionService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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

fun Route.configureTransactionRoutes() {
    val screeningService by inject<ScreeningService>()
    val transactionService by inject<TransactionService>()
    val idempotencyService by inject<IdempotencyService>()
    val pendingNotificationService by inject<PendingNotificationService>()
    val rateLimiter by inject<RateLimiterService>()
    val json by inject<Json>()

    authenticate("session-auth") {
        route("/transactions") {
            post("/build") {
                // Fix: value-moving build requests now hit the kill switch before any parsing or screening work.
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
                if (!screeningService.check(request.to)) {
                    throw AddressRejectedError()
                }
                val response = BuildResponse(
                    status = "prepared",
                    preview = "Prepared unsigned transaction for ${request.to}.",
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
                val submitted = transactionService.recordSubmitted(
                    walletAddress = principal.walletAddress,
                    toAddress = call.request.headers["X-To-Address"] ?: "0x0000000000000000000000000000000000000000",
                    signedTx = request.signedTx,
                    asset = call.request.headers["X-Asset"] ?: "ETH",
                    amount = call.request.headers["X-Amount"] ?: "0",
                    chainId = call.request.headers["X-Chain-Id"]?.toLongOrNull() ?: 1L,
                )
                // Fix: successful broadcasts now register a watcher row so confirmations can fan out to active devices.
                pendingNotificationService.register(
                    walletAddress = principal.walletAddress,
                    txHash = submitted.txHash,
                    chain = call.request.headers["X-Chain-Id"]?.toLongOrNull() ?: 1L,
                    asset = call.request.headers["X-Asset"] ?: "ETH",
                    amount = call.request.headers["X-Amount"] ?: "0",
                    notificationType = "TX_CONFIRMED",
                )
                val response = SendResponse(
                    txHash = submitted.txHash,
                    status = submitted.status,
                )
                // Fix: cache successful mutation responses so duplicate keys replay the original body.
                idempotencyService.complete(
                    key = call.request.headers["Idempotency-Key"].orEmpty(),
                    walletAddress = principal.walletAddress,
                    endpoint = call.request.path(),
                    statusCode = HttpStatusCode.OK.value,
                    payload = json.encodeToString(response),
                )
                call.respond(response)
            }

            get("/history") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.respond(transactionService.history(principal.walletAddress))
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

private val TX_HASH_REGEX = Regex("^0x[a-fA-F0-9]{64}$")

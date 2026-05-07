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
import com.letapay.backend.middleware.attachPreAuthWallet
import com.letapay.backend.middleware.enforcePreAuthRateLimit
import com.letapay.backend.model.auth.FirebaseTokenResponse
import com.letapay.backend.model.auth.NonceRequest
import com.letapay.backend.model.auth.NonceResponse
import com.letapay.backend.model.auth.RefreshRequest
import com.letapay.backend.model.auth.VerifyRequest
import com.letapay.backend.security.WalletPrincipal
import com.letapay.backend.service.AuthService
import com.letapay.backend.service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.koin.ktor.ext.inject

fun Route.configureAuthRoutes() {
    val authService by inject<AuthService>()
    val rateLimiter by inject<RateLimiterService>()

    route("/auth") {
        post("/request-nonce") {
            val request = call.receive<NonceRequest>()
            call.attachPreAuthWallet(request.walletAddress)
            call.enforcePreAuthRateLimit(rateLimiter)
            val nonce = withUpstreamTimeout { authService.generateNonce(request.walletAddress) }
            call.respond(
                NonceResponse(
                    nonce = nonce.nonce,
                    expiresAt = nonce.expiresAt,
                ),
            )
        }

        post("/verify-signature") {
            val request = call.receive<VerifyRequest>()
            call.attachPreAuthWallet(request.walletAddress)
            call.enforcePreAuthRateLimit(rateLimiter)
            call.respond(withUpstreamTimeout { authService.verify(request) })
        }

        post("/refresh-token") {
            val request = call.receive<RefreshRequest>()
            call.respond(
                withUpstreamTimeout {
                    authService.rotateRefreshToken(
                        rawToken = request.refreshToken,
                        deviceFingerprint = call.request.headers["X-Device-Fingerprint"],
                    )
                },
            )
        }

        authenticate("session-auth") {
            post("/firebase-token") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                call.respond(
                    FirebaseTokenResponse(
                        firebaseToken = withUpstreamTimeout {
                            authService.mintFirebaseToken(
                                walletAddress = principal.walletAddress,
                                sessionId = principal.sessionId,
                            )
                        },
                    ),
                )
            }

            post("/revoke-session") {
                val principal = requireNotNull(call.principal<WalletPrincipal>())
                authService.revokeSession(principal.sessionId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private suspend fun <T> withUpstreamTimeout(block: suspend () -> T): T =
    try {
        withTimeout(10_000L) { block() }
    } catch (_: TimeoutCancellationException) {
        throw UpstreamTimeoutError()
    }

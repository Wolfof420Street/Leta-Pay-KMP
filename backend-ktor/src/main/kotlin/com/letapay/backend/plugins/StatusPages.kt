/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.plugins

import com.letapay.backend.error.BackendException
import com.letapay.backend.model.ErrorResponse
import com.letapay.backend.security.WalletPrincipal
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.slf4j.MDC

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            val requestId = MDC.get("requestId") ?: ""
            this@configureStatusPages.logWarn(call, "BAD_REQUEST")
            call.respond(
                status = io.ktor.http.HttpStatusCode.BadRequest,
                message = ErrorResponse(
                    "BAD_REQUEST",
                    if (cause.cause == null) "Malformed request body." else "Request body could not be parsed.",
                    requestId = requestId,
                ),
            )
        }
        exception<RequestValidationException> { call, cause ->
            val requestId = MDC.get("requestId") ?: ""
            this@configureStatusPages.logWarn(call, "VALIDATION_FAILED")
            call.respond(
                status = io.ktor.http.HttpStatusCode.BadRequest,
                message = ErrorResponse(
                    "VALIDATION_FAILED",
                    cause.reasons.joinToString("; "),
                    requestId = requestId,
                ),
            )
        }
        exception<BackendException> { call, cause ->
            val requestId = MDC.get("requestId") ?: ""
            // Fix: every domain error now resolves through one envelope so new Phase 5/6 codes stay consistent.
            cause.retryAfterSeconds?.let { retryAfter ->
                call.response.headers.append("Retry-After", retryAfter.toString())
            }
            this@configureStatusPages.logWarn(call, cause.code)
            call.respond(
                status = cause.status,
                message = ErrorResponse(
                    code = cause.code,
                    message = cause.message ?: "Request failed.",
                    retryAfter = cause.retryAfterSeconds,
                    requestId = requestId,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            val durationMs = System.currentTimeMillis() - call.attributes[RequestStartedKey]
            val wallet = call.principal<WalletPrincipal>()?.walletAddress?.take(10) ?: "anonymous"
            val requestId = MDC.get("requestId") ?: ""
            this@configureStatusPages.environment.log.error(
                "walletAddress={} route={} errorCode=INTERNAL_ERROR durationMs={} requestId={}",
                wallet,
                "${call.request.httpMethod.value} ${call.request.path()}",
                durationMs,
                requestId,
                cause,
            )
            call.respond(
                status = io.ktor.http.HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    requestId = requestId,
                ),
            )
        }
    }
}

private fun Application.logWarn(call: io.ktor.server.application.ApplicationCall, errorCode: String) {
    val durationMs = System.currentTimeMillis() - call.attributes[RequestStartedKey]
    val wallet = call.principal<WalletPrincipal>()?.walletAddress?.take(10) ?: "anonymous"
    val requestId = MDC.get("requestId") ?: ""
    environment.log.warn(
        "walletAddress={} route={} errorCode={} durationMs={} requestId={}",
        wallet,
        "${call.request.httpMethod.value} ${call.request.path()}",
        errorCode,
        durationMs,
        requestId,
    )
}

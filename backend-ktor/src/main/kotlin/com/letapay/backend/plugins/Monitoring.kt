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

import com.letapay.backend.error.PayloadTooLargeError
import com.letapay.backend.security.WalletPrincipal
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.contentLength
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.MDC
import org.slf4j.event.Level

val RequestStartedKey = io.ktor.util.AttributeKey<Long>("requestStartedAt")
val PrometheusRegistryKey = io.ktor.util.AttributeKey<PrometheusMeterRegistry>("prometheusRegistry")

private val RequestHardeningPlugin = createApplicationPlugin(name = "RequestHardeningPlugin") {
    onCall { call ->
        val requestId = call.request.headers["X-Request-ID"].toSafeRequestId()
        MDC.put("requestId", requestId)
        call.attributes.put(RequestStartedKey, System.currentTimeMillis())
        val principal = call.principal<WalletPrincipal>()
        principal?.walletAddress?.truncatedWallet()?.let { MDC.put("walletAddress", it) }
        principal?.sessionId?.let { MDC.put("sessionId", it) }
        call.extractTxHash()?.let { MDC.put("txHash", it) }
        val contentLength = call.request.contentLength()
        if (contentLength != null && contentLength > 64 * 1024) {
            // Fix: reject oversized payloads before route handlers deserialize
            // them so every endpoint shares the same 64 KB ceiling.
            throw PayloadTooLargeError()
        }
    }
    onCallRespond { _, _ ->
        MDC.remove("walletAddress")
        MDC.remove("sessionId")
        MDC.remove("txHash")
        MDC.remove("requestId")
    }
}

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    attributes.put(PrometheusRegistryKey, registry)
    install(MicrometerMetrics) {
        this.registry = registry
    }
    install(RequestHardeningPlugin)
}

private fun ApplicationCall.extractTxHash(): String? =
    parameters["txHash"]
        ?: request.queryParameters["txHash"]
        ?: request.headers["X-Tx-Hash"]

private val REQUEST_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")

private fun String?.toSafeRequestId(): String {
    val candidate = this?.trim().orEmpty()
    return if (candidate.isValidRequestId()) {
        candidate
    } else {
        java.util.UUID.randomUUID().toString()
    }
}

private fun String.isValidRequestId(): Boolean =
    isNotBlank() &&
        none { it == '\r' || it == '\n' || it.isISOControl() } &&
        REQUEST_ID_REGEX.matches(this)

private fun String.truncatedWallet(): String =
    if (length <= 12) this else "${take(6)}...${takeLast(4)}"

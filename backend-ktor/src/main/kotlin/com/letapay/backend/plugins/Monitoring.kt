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
        call.attributes.put(RequestStartedKey, System.currentTimeMillis())
        val principal = call.principal<WalletPrincipal>()
        principal?.walletAddress?.take(16)?.let { MDC.put("walletAddress", it) }
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

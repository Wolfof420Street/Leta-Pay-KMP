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

import com.letapay.backend.config.AppConfig
import com.letapay.backend.routes.configureAiRoutes
import com.letapay.backend.routes.configureAuthRoutes
import com.letapay.backend.routes.configureContactRoutes
import com.letapay.backend.routes.configureDeviceTokenRoutes
import com.letapay.backend.routes.configureHealthRoutes
import com.letapay.backend.routes.configurePriceRoutes
import com.letapay.backend.routes.configureSwapRoutes
import com.letapay.backend.routes.configureTransactionRoutes
import com.letapay.backend.routes.configureYieldRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    val appConfig = get<AppConfig>()
    install(SSE)
    install(CachingHeaders)
    install(CORS) {
        allowHost("letapay.app", schemes = listOf("https"))
        if (!appConfig.isProduction) {
            allowHost("localhost:3000", schemes = listOf("http"))
            allowHost("localhost:8080", schemes = listOf("http"))
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    routing {
        get("/metrics") {
            val registry = call.application.attributes[PrometheusRegistryKey]
            call.respondText(registry.scrape(), status = HttpStatusCode.OK)
        }
        get("/openapi") {
            call.respondText(buildOpenApiSpec(), status = HttpStatusCode.OK)
        }
        configureHealthRoutes()
        configureAuthRoutes()
        configureDeviceTokenRoutes()
        configureAiRoutes()
        configurePriceRoutes()
        configureContactRoutes()
        configureTransactionRoutes()
        configureSwapRoutes()
        configureYieldRoutes()
    }
}

private fun buildOpenApiSpec(): String = """
{
  "openapi": "3.0.3",
  "info": { "title": "Leta Pay Backend API", "version": "1.0.0" },
  "paths": {
    "/health": {},
    "/auth/request-nonce": {},
    "/auth/verify-signature": {},
    "/auth/refresh-token": {},
    "/auth/firebase-token": {},
    "/auth/revoke-session": {},
    "/device-token": {},
    "/ai/chat-stream": {},
    "/ai/parse": {},
    "/ai/plan": {},
    "/api/ai/chat-stream": {},
    "/prices/{chain}/{asset}": {},
    "/transactions/build": {},
    "/transactions/send": {},
    "/transactions/history": {},
    "/transactions/status/{txHash}": {},
    "/swap/quote": {},
    "/swap/execute": {},
    "/yield/opportunities": {},
    "/yield/stake": {},
    "/yield/unstake": {},
    "/yield/positions": {}
  }
}
""".trimIndent()

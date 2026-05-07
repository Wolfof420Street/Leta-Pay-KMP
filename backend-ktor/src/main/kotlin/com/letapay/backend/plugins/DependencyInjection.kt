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

import com.google.firebase.messaging.FirebaseMessaging
import com.letapay.backend.config.AppConfig
import com.letapay.backend.config.RuntimeState
import com.letapay.backend.db.DatabaseFactory
import com.letapay.backend.security.JwtTokenService
import com.letapay.backend.service.AgentKitClient
import com.letapay.backend.service.AuthService
import com.letapay.backend.service.CircuitBreaker
import com.letapay.backend.service.CoinbaseScreeningService
import com.letapay.backend.service.CoinbaseService
import com.letapay.backend.service.ConfirmationWatcher
import com.letapay.backend.service.ContactService
import com.letapay.backend.service.DatabaseDeviceTokenService
import com.letapay.backend.service.DatabaseIdempotencyService
import com.letapay.backend.service.DatabasePendingNotificationService
import com.letapay.backend.service.DatabaseTransactionService
import com.letapay.backend.service.DefaultAiCommandService
import com.letapay.backend.service.DefaultAuthService
import com.letapay.backend.service.DefaultHealthService
import com.letapay.backend.service.DefaultPricingService
import com.letapay.backend.service.DefaultYieldService
import com.letapay.backend.service.DeviceTokenService
import com.letapay.backend.service.FakeFirebaseTokenService
import com.letapay.backend.service.FirebaseAdminTokenService
import com.letapay.backend.service.FirebaseMessagingClient
import com.letapay.backend.service.FirebaseTokenService
import com.letapay.backend.service.HealthService
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.ParseResultCache
import com.letapay.backend.service.PendingNotificationService
import com.letapay.backend.service.PriceCache
import com.letapay.backend.service.PricingService
import com.letapay.backend.service.PushMessagingClient
import com.letapay.backend.service.PushNotificationService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.ScreeningService
import com.letapay.backend.service.SidecarAgentKitClient
import com.letapay.backend.service.StubCoinbaseService
import com.letapay.backend.service.StubContactService
import com.letapay.backend.service.SwapQuoteCacheService
import com.letapay.backend.service.TransactionCommandService
import com.letapay.backend.service.TransactionService
import com.letapay.backend.service.YieldService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import javax.sql.DataSource

fun Application.configureDependencyInjection(overrides: Module? = null) {
    val appModules = mutableListOf(
        module {
            single { AppConfig.from(environment.config) }
            single<DataSource> { DatabaseFactory.init(environment.config) }
            single { PriceCache() }
            single { ParseResultCache() }
            single(named("coinbaseCircuitBreaker")) {
                CircuitBreaker(
                    name = "coinbase",
                    failureThreshold = 5,
                    resetTimeoutMs = 30_000L,
                )
            }
            single(named("openAiCircuitBreaker")) {
                CircuitBreaker(
                    name = "openai",
                    failureThreshold = 3,
                    resetTimeoutMs = 60_000L,
                )
            }
            single(named("firebaseCircuitBreaker")) {
                CircuitBreaker(
                    name = "firebase",
                    failureThreshold = 5,
                    resetTimeoutMs = 30_000L,
                )
            }
            single {
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            }
            single {
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(get())
                    }
                    install(HttpRequestRetry) {
                        maxRetries = 2
                        retryIf { _, response -> response.status.value >= HttpStatusCode.InternalServerError.value }
                        exponentialDelay(base = 200.0, maxDelayMs = 2_000)
                    }
                    install(Logging) {
                        level = LogLevel.NONE
                    }
                }
            }
            single { JwtTokenService(get()) }
            single<FirebaseTokenService> {
                val hasFirebaseConfig = !System.getenv("FIREBASE_SA_JSON").isNullOrBlank() ||
                    !System.getenv("FIREBASE_SA_PATH").isNullOrBlank()
                if (hasFirebaseConfig) FirebaseAdminTokenService() else FakeFirebaseTokenService()
            }
            single<PushMessagingClient> {
                if (RuntimeState.isFirebaseHealthy()) {
                    try {
                        FirebaseMessagingClient(FirebaseMessaging.getInstance())
                    } catch (exception: Exception) {
                        com.letapay.backend.service.FakePushMessagingClient()
                    }
                } else {
                    com.letapay.backend.service.FakePushMessagingClient()
                }
            }
            single<AuthService> { DefaultAuthService(get(), get(), get()) }
            single<com.letapay.backend.service.AiCommandService> { DefaultAiCommandService() }
            single<PricingService> { DefaultPricingService(get()) }
            single<ContactService> { StubContactService() }
            single { RateLimiterService() }
            single<ScreeningService> { CoinbaseScreeningService(get()) }
            single<IdempotencyService> { DatabaseIdempotencyService(get()) }
            single<TransactionService> { DatabaseTransactionService() }
            single<TransactionCommandService> {
                com.letapay.backend.service.DefaultTransactionCommandService(get(), get())
            }
            single<AgentKitClient> {
                val config = get<AppConfig>()
                SidecarAgentKitClient(
                    httpClient = get(),
                    sidecarUrl = config.agentKitSidecarUrl,
                    sidecarSecret = config.sidecarSecret,
                )
            }
            single<CoinbaseService> {
                StubCoinbaseService(get(), get(), get(named("coinbaseCircuitBreaker")))
            }
            single { SwapQuoteCacheService() }
            single<com.letapay.backend.service.SwapService> { com.letapay.backend.service.DefaultSwapService() }
            single<DeviceTokenService> { DatabaseDeviceTokenService() }
            single<PendingNotificationService> { DatabasePendingNotificationService() }
            single<HealthService> { DefaultHealthService(get()) }
            single { PushNotificationService(get(), get()) }
            single<YieldService> { DefaultYieldService(get()) }
            single { ConfirmationWatcher(get(), get(), get()) }
        },
    )
    overrides?.let(appModules::add)

    install(Koin) {
        allowOverride(true)
        modules(appModules)
    }

    // Fix: create the DataSource during startup so Exposed is connected before the first request touches a transaction.
    get<DataSource>()
}

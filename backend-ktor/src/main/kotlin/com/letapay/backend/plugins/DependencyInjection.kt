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
import com.letapay.app.core.domain.JwtKeyProvider
import com.letapay.app.core.domain.KeyValueCache
import com.letapay.app.core.domain.RateLimiter
import com.letapay.backend.config.AppConfig
import com.letapay.backend.config.RuntimeState
import com.letapay.backend.db.DatabaseFactory
import com.letapay.backend.security.DevJwtKeyProvider
import com.letapay.backend.security.JwtTokenService
import com.letapay.backend.security.RsaJwtKeyProvider
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
import com.letapay.backend.service.DefaultCoinbaseService
import com.letapay.backend.service.DefaultHealthService
import com.letapay.backend.service.DefaultPricingService
import com.letapay.backend.service.DefaultYieldService
import com.letapay.backend.service.DeviceTokenService
import com.letapay.backend.service.DisabledFirebaseTokenService
import com.letapay.backend.service.EmptyContactService
import com.letapay.backend.service.FirebaseAdminTokenService
import com.letapay.backend.service.FirebaseMessagingClient
import com.letapay.backend.service.FirebaseTokenService
import com.letapay.backend.service.HealthService
import com.letapay.backend.service.IdempotencyService
import com.letapay.backend.service.InMemoryRateLimiter
import com.letapay.backend.service.ParseResultCache
import com.letapay.backend.service.PendingNotificationService
import com.letapay.backend.service.PriceCache
import com.letapay.backend.service.PricingService
import com.letapay.backend.service.PushMessagingClient
import com.letapay.backend.service.PushNotificationService
import com.letapay.backend.service.RateLimiterService
import com.letapay.backend.service.RedisClient
import com.letapay.backend.service.RedisKeyValueCache
import com.letapay.backend.service.RedisRateLimiter
import com.letapay.backend.service.ScreeningService
import com.letapay.backend.service.SidecarAgentKitClient
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import javax.sql.DataSource

fun Application.configureDependencyInjection(vararg overrides: Module) {
    val appModules = mutableListOf(
        module {
            single { AppConfig.from(environment.config) }
            single<DataSource> { DatabaseFactory.init(environment.config) }
            single {
                val cfg = get<AppConfig>()
                val url = if (cfg.isProduction) cfg.redisUrl else cfg.redisUrl.ifBlank { "" }
                RedisClient(url)
            }

            single {
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            }

            single<KeyValueCache<String>>(named("priceCache")) {
                RedisKeyValueCache(get(), get(), String.serializer(), "price_cache")
            }
            single<KeyValueCache<com.letapay.backend.model.ai.ParseResult>>(named("parseResultCache")) {
                RedisKeyValueCache(
                    get(),
                    get(),
                    com.letapay.backend.model.ai.ParseResult.serializer(),
                    "parse_result_cache",
                )
            }
            single<KeyValueCache<com.letapay.backend.model.swap.SwapQuote>>(named("swapQuoteCache")) {
                RedisKeyValueCache(
                    get(),
                    get(),
                    com.letapay.backend.model.swap.SwapQuote.serializer(),
                    "swap_quote_cache",
                )
            }

            single { PriceCache(get(named("priceCache"))) }
            single { ParseResultCache(get(named("parseResultCache"))) }
            single { SwapQuoteCacheService(get(named("swapQuoteCache"))) }

            single<CoroutineScope>(named("applicationScope")) {
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            }

            single<RateLimiter>(named("rateLimiter")) {
                val config = get<AppConfig>()
                if (config.isProduction) {
                    if (config.redisUrl.isBlank()) {
                        error("REDIS_URL required in production for rate limiting")
                    }
                    RedisRateLimiter(get())
                } else {
                    InMemoryRateLimiter()
                }
            }
            single { RateLimiterService(get(named("rateLimiter"))) }

            single<JwtKeyProvider> {
                val config = get<AppConfig>()
                check(!(config.isProduction && (config.jwtPrivateKey == null || config.jwtPublicKey == null))) {
                    "DevJwtKeyProvider cannot be used when ENV=production."
                }
                if (config.isDev && (config.jwtPrivateKey == null || config.jwtPublicKey == null)) {
                    DevJwtKeyProvider()
                } else {
                    RsaJwtKeyProvider(config)
                }
            }
            single(named("coinbaseCircuitBreaker")) {
                CircuitBreaker(name = "coinbase", failureThreshold = 5, resetTimeoutMs = 30_000L)
            }
            single(named("openAiCircuitBreaker")) {
                CircuitBreaker(name = "openai", failureThreshold = 3, resetTimeoutMs = 60_000L)
            }
            single(named("firebaseCircuitBreaker")) {
                CircuitBreaker(name = "firebase", failureThreshold = 5, resetTimeoutMs = 30_000L)
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
            single { JwtTokenService(get(), get()) }
            single<FirebaseTokenService> {
                val hasFirebaseConfig = !System.getenv("FIREBASE_SA_JSON").isNullOrBlank() ||
                    !System.getenv("FIREBASE_SA_PATH").isNullOrBlank()
                if (hasFirebaseConfig) FirebaseAdminTokenService() else DisabledFirebaseTokenService()
            }
            single<PushMessagingClient> {
                if (RuntimeState.isFirebaseHealthy()) {
                    try {
                        FirebaseMessagingClient(FirebaseMessaging.getInstance())
                    } catch (_: Exception) {
                        com.letapay.backend.service.NoOpPushMessagingClient()
                    }
                } else {
                    com.letapay.backend.service.NoOpPushMessagingClient()
                }
            }
            single<AuthService> { DefaultAuthService(get(), get(), get()) }
            single<com.letapay.backend.service.AiCommandService> { DefaultAiCommandService(get()) }
            single<PricingService> { DefaultPricingService(get()) }
            single<ContactService> { EmptyContactService() }
            single<ScreeningService> { CoinbaseScreeningService(get(), get(named("coinbaseCircuitBreaker"))) }
            single<IdempotencyService> { DatabaseIdempotencyService(get()) }
            single<TransactionService> { DatabaseTransactionService() }
            single<TransactionCommandService> {
                com.letapay.backend.service.DefaultTransactionCommandService(get(), get(), get())
            }
            single<AgentKitClient> {
                val config = get<AppConfig>()
                SidecarAgentKitClient(
                    httpClient = get(),
                    sidecarUrl = config.agentKitSidecarUrl,
                    sidecarSecret = config.sidecarSecret,
                    circuitBreaker = get(named("coinbaseCircuitBreaker")),
                )
            }
            single<CoinbaseService> {
                DefaultCoinbaseService(get(), get(), get(named("coinbaseCircuitBreaker")))
            }
            single<com.letapay.backend.service.SwapService> { com.letapay.backend.service.DefaultSwapService() }
            single<DeviceTokenService> { DatabaseDeviceTokenService() }
            single<PendingNotificationService> { DatabasePendingNotificationService() }
            single<HealthService> { DefaultHealthService(get(), get(), get(), get()) }
            single { PushNotificationService(get(), get()) }
            single<YieldService> { DefaultYieldService(get()) }
            single { ConfirmationWatcher(get(), get(), get()) }
        },
    )
    appModules.addAll(overrides)

    install(Koin) {
        allowOverride(true)
        modules(appModules)
    }

    get<DataSource>()
}

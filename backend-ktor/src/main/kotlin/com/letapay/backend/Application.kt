/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend

import com.letapay.backend.config.RuntimeState
import com.letapay.backend.config.initFirebase
import com.letapay.backend.db.IdempotencyKeys
import com.letapay.backend.db.Sessions
import com.letapay.backend.plugins.configureDependencyInjection
import com.letapay.backend.plugins.configureKoog
import com.letapay.backend.plugins.configureMonitoring
import com.letapay.backend.plugins.configureRouting
import com.letapay.backend.plugins.configureSecurity
import com.letapay.backend.plugins.configureSerialization
import com.letapay.backend.plugins.configureStatusPages
import com.letapay.backend.service.ConfirmationWatcher
import com.letapay.backend.service.RedisClient
import com.letapay.backend.service.YieldService
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.qualifier.named
import org.koin.ktor.ext.get

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.configureApp(vararg overrides: org.koin.core.module.Module) {
    configureMonitoring()
    configureSerialization()
    configureStatusPages()
    configureDependencyInjection(*overrides)
    configureSecurity()
    configureKoog()
    configureRouting()

    val yieldService = get<YieldService>()
    val confirmationWatcher = get<ConfirmationWatcher>()
    val redisClient = get<RedisClient>()
    val backendHttpClient = get<HttpClient>()
    val reconciliationScope = get<kotlinx.coroutines.CoroutineScope>(named("applicationScope"))
    val appConfig = get<com.letapay.backend.config.AppConfig>()

    environment.monitor.subscribe(ApplicationStarted) {
        RuntimeState.markMisconfigured(false)
        try {
            initFirebase(environment.config)
            if (appConfig.isProduction && !RuntimeState.isFirebaseConfigured()) {
                log.warn("Firebase credentials are not configured; push notifications will be disabled.")
            }
        } catch (exception: Exception) {
            if (appConfig.isProduction) {
                log.warn("Firebase credentials unavailable or invalid; push notifications will be disabled.")
            } else {
                log.info("Firebase init skipped in non-production environment.")
            }
            RuntimeState.markFirebaseHealthy(false)
        }
    }

    if (appConfig.isProduction) {
        reconciliationScope.launch {
            while (isActive) {
                try {
                    yieldService.reconcilePositions()
                } catch (exception: Exception) {
                    log.warn("Position reconciliation failed", exception)
                }
                delay(5 * 60 * 1000L)
            }
        }

        reconciliationScope.launch {
            try {
                confirmationWatcher.watch()
            } catch (exception: Exception) {
                // Fix: watcher boot failures are logged but do not block server startup.
                log.warn("Confirmation watcher failed to initialise", exception)
            }
        }

        reconciliationScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    transaction {
                        IdempotencyKeys.deleteWhere { expiresAt less now }
                        Sessions.deleteWhere { expiresAt less now }
                    }
                } catch (exception: Exception) {
                    log.warn("Security state cleanup failed", exception)
                }
                delay(60 * 60 * 1000L)
            }
        }
    }

    environment.monitor.subscribe(ApplicationStopping) {
        reconciliationScope.cancel()
        redisClient.close()
        backendHttpClient.close()
    }
}

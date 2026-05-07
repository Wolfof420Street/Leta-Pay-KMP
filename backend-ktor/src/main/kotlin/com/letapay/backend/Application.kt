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
import com.letapay.backend.service.YieldService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.configureApp(overrides: org.koin.core.module.Module? = null) {
    configureMonitoring()
    configureSerialization()
    configureStatusPages()
    configureDependencyInjection(overrides)
    configureSecurity()
    configureKoog()
    configureRouting()

    val yieldService = get<YieldService>()
    val confirmationWatcher = get<ConfirmationWatcher>()
    val reconciliationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    environment.monitor.subscribe(ApplicationStarted) {
        val required = listOf(
            "OPENAI_API_KEY",
            "SESSION_SECRET",
            "REFRESH_TOKEN_PEPPER",
            "DATABASE_URL",
            "COINBASE_API_KEY",
            "COINBASE_RISK_KEY",
        )
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        if (missing.isNotEmpty()) {
            log.error("Missing required environment variables: {}", missing.joinToString())
            RuntimeState.markMisconfigured(true)
        } else {
            RuntimeState.markMisconfigured(false)
        }
        try {
            initFirebase(environment.config)
        } catch (exception: Exception) {
            log.error("Firebase init failed: {}", exception.message)
            RuntimeState.markFirebaseHealthy(false)
        }
    }

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

    environment.monitor.subscribe(ApplicationStopping) {
        reconciliationScope.cancel()
    }
}

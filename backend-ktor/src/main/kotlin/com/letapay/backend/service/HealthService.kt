/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.config.AppConfig
import com.letapay.backend.config.RuntimeState
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

interface HealthService {
    suspend fun status(): HealthSnapshot
}

data class HealthSnapshot(
    val status: String,
    val db: String,
    val redis: String,
    val sidecar: String,
    val firebase: String,
)

class DefaultHealthService(
    private val dataSource: DataSource,
    private val redisClient: RedisClient,
    private val httpClient: HttpClient,
    private val appConfig: AppConfig,
) : HealthService {
    override suspend fun status(): HealthSnapshot {
        val db = checkDatabase()
        val redis = checkRedis()
        val sidecar = checkSidecar()
        val firebase = when {
            !RuntimeState.isFirebaseConfigured() -> "unconfigured"
            RuntimeState.isFirebaseHealthy() -> "ok"
            else -> "degraded"
        }
        return HealthSnapshot(
            status = if (db == "ok" && redis == "ok" && sidecar == "ok") "ok" else "degraded",
            db = db,
            redis = redis,
            sidecar = sidecar,
            firebase = firebase,
        )
    }

    private suspend fun checkDatabase(): String =
        withContext(Dispatchers.IO) {
            runCatching {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("SELECT 1").use { statement ->
                        statement.execute()
                    }
                }
                "ok"
            }.getOrElse { "degraded" }
        }

    private suspend fun checkRedis(): String =
        runCatching {
            redisClient.ping()
            "ok"
        }.getOrElse { "degraded" }

    private suspend fun checkSidecar(): String =
        runCatching {
            val response = httpClient.get("${appConfig.agentKitSidecarUrl}/health") {
                header("X-Internal-Token", appConfig.sidecarSecret)
            }
            if (response.status == HttpStatusCode.OK) "ok" else "degraded"
        }.getOrElse { "degraded" }
}

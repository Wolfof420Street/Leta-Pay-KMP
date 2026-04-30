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

import com.letapay.backend.config.RuntimeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

interface HealthService {
    suspend fun dbStatus(): String

    suspend fun firebaseStatus(): String
}

class DefaultHealthService(
    private val dataSource: DataSource,
) : HealthService {
    override suspend fun dbStatus(): String =
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

    override suspend fun firebaseStatus(): String =
        if (RuntimeState.isFirebaseHealthy()) "ok" else "degraded"
}

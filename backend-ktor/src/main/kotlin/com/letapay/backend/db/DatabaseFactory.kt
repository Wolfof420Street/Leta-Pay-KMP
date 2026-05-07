/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import javax.sql.DataSource

object DatabaseFactory {
    fun init(config: ApplicationConfig): DataSource {
        val defaultJdbcUrl = buildString {
            append("jdbc:h2:mem:letapay-")
            append(UUID.randomUUID())
            append(";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
        }
        val pool = HikariDataSource(
            HikariConfig().apply {
                // Fix: Ktor tests boot without application.conf, so default to isolated in-memory H2.
                jdbcUrl = config.propertyOrNull("database.url")?.getString()
                    ?: System.getenv("DATABASE_URL")
                    ?: defaultJdbcUrl
                driverClassName = config.propertyOrNull("database.driver")?.getString()
                    ?: if (jdbcUrl.startsWith("jdbc:h2:")) "org.h2.Driver" else null
                username = config.propertyOrNull("database.user")?.getString()
                    ?: if (jdbcUrl.startsWith("jdbc:h2:")) "sa" else null
                password = config.propertyOrNull("database.password")?.getString()
                    ?: if (jdbcUrl.startsWith("jdbc:h2:")) "" else null
                maximumPoolSize = config.propertyOrNull("database.maxPoolSize")
                    ?.getString()
                    ?.toInt()
                    ?: System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull()
                    ?: 10
                minimumIdle = 2
                idleTimeout = 600_000L
                connectionTimeout = 30_000L
                maxLifetime = 1_800_000L
                validationTimeout = 5_000L
                connectionTestQuery = "SELECT 1"
                isAutoCommit = false
                validate()
            },
        )
        Database.connect(pool)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Nonces,
                Sessions,
                IdempotencyKeys,
                Transactions,
                SwapQuotes,
                StakingPositions,
                DeviceTokens,
                PendingNotifications,
            )
        }
        return pool
    }
}

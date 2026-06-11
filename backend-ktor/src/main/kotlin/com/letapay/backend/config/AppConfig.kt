/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.config

import io.ktor.server.config.ApplicationConfig
import java.math.BigDecimal

data class AppConfig(
    val environment: RuntimeEnvironment,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtSecret: String,
    val refreshTokenPepper: String,
    val parseModel: String,
    val planModel: String,
    val chatModel: String,
    val agentKitSidecarUrl: String,
    val hasExplicitAgentKitSidecarUrl: Boolean,
    val sidecarSecret: String,
    val jwtPrivateKey: String?,
    val jwtPublicKey: String?,
    val appDomain: String,
    val appOrigin: String,
    val redisUrl: String,
    val databaseUrl: String?,
    val dbMinPoolSize: Int,
    val dbMaxPoolSize: Int,
    val dbConnectionTimeoutMs: Long,
    val maxTransactionValueEth: BigDecimal?,
) {
    val isDev: Boolean get() = environment == RuntimeEnvironment.DEVELOPMENT
    val isProduction: Boolean get() = environment == RuntimeEnvironment.PRODUCTION

    companion object {
        private val productionDefaults = setOf(
            "dev-jwt-secret-change-me",
            "dev-refresh-pepper-change-me",
            "dev-sidecar-secret",
        )

        fun from(
            config: ApplicationConfig,
            allowMissingJwtKeys: Boolean = false,
        ): AppConfig {
            EnvLoader.load()
            val environment = RuntimeEnvironment.from(
                EnvLoader.get("ENV") ?: EnvLoader.get("ENVIRONMENT"),
                config.propertyOrNull("ktor.development")?.getString()?.toBooleanStrictOrNull() == true,
            )
            val explicitSidecarUrl = config.stringOrNull(
                path = "agentkit.sidecarUrl",
                env = "AGENTKIT_SIDECAR_URL",
            )
            val appConfig = AppConfig(
                environment = environment,
                jwtIssuer = config.string(
                    path = "security.jwt.issuer",
                    env = "JWT_ISSUER",
                    default = "leta-pay-backend",
                ),
                jwtAudience = config.string(
                    path = "security.jwt.audience",
                    env = "JWT_AUDIENCE",
                    default = "leta-pay-clients",
                ),
                jwtSecret = config.string(
                    path = "security.jwt.secret",
                    env = "SESSION_SECRET",
                    default = "dev-jwt-secret-change-me",
                ),
                refreshTokenPepper = config.string(
                    path = "security.refresh.pepper",
                    env = "REFRESH_TOKEN_PEPPER",
                    default = "dev-refresh-pepper-change-me",
                ),
                parseModel = config.string(
                    path = "ai.models.parse",
                    env = "AI_PARSE_MODEL",
                    default = "gpt-4o-mini",
                ),
                planModel = config.string(
                    path = "ai.models.plan",
                    env = "AI_PLAN_MODEL",
                    default = "gpt-4o",
                ),
                chatModel = config.string(
                    path = "ai.models.chat",
                    env = "AI_CHAT_MODEL",
                    default = "gpt-4o-mini",
                ),
                agentKitSidecarUrl = explicitSidecarUrl ?: "http://agentkit-sidecar:3100",
                hasExplicitAgentKitSidecarUrl = explicitSidecarUrl != null,
                sidecarSecret = config.string(
                    path = "agentkit.sidecarSecret",
                    env = "SIDECAR_SECRET",
                    default = "dev-sidecar-secret",
                ),
                jwtPrivateKey = config.stringOrNull(
                    path = "security.jwt.privateKey",
                    env = "JWT_PRIVATE_KEY",
                ),
                jwtPublicKey = config.stringOrNull(
                    path = "security.jwt.publicKey",
                    env = "JWT_PUBLIC_KEY",
                ),
                appDomain = config.string(
                    path = "security.siwe.domain",
                    env = "APP_DOMAIN",
                    default = "letapay.app",
                ),
                appOrigin = config.string(
                    path = "security.siwe.origin",
                    env = "APP_ORIGIN",
                    default = "https://letapay.app",
                ),
                redisUrl = config.string(
                    path = "redis.url",
                    env = "REDIS_URL",
                    default = if (environment == RuntimeEnvironment.PRODUCTION) "" else "redis://localhost:6379",
                ),
                databaseUrl = config.stringOrNull(
                    path = "database.url",
                    env = "DATABASE_URL",
                ),
                dbMinPoolSize = config.int(
                    path = "database.minPoolSize",
                    env = "DB_MIN_POOL_SIZE",
                    default = if (environment == RuntimeEnvironment.PRODUCTION) 4 else 1,
                ),
                dbMaxPoolSize = config.int(
                    path = "database.maxPoolSize",
                    env = "DB_MAX_POOL_SIZE",
                    default = if (environment == RuntimeEnvironment.PRODUCTION) 20 else 10,
                ),
                dbConnectionTimeoutMs = config.long(
                    path = "database.connectionTimeoutMs",
                    env = "DB_CONNECTION_TIMEOUT_MS",
                    default = 30_000L,
                ),
                maxTransactionValueEth = config.decimalOrNull(
                    path = "transactions.maxValueEth",
                    env = "MAX_TRANSACTION_VALUE_ETH",
                ),
            )
            appConfig.validate(allowMissingJwtKeys)
            return appConfig
        }

        private fun ApplicationConfig.stringOrNull(path: String, env: String): String? =
            propertyOrNull(path)?.getString()
                ?.takeIf(String::isNotBlank)
                ?: EnvLoader.get(env)?.takeIf(String::isNotBlank)

        private fun ApplicationConfig.string(path: String, env: String, default: String): String =
            stringOrNull(path, env) ?: default

        private fun ApplicationConfig.int(path: String, env: String, default: Int): Int =
            stringOrNull(path, env)?.toIntOrNull() ?: default

        private fun ApplicationConfig.long(path: String, env: String, default: Long): Long =
            stringOrNull(path, env)?.toLongOrNull() ?: default

        private fun ApplicationConfig.decimalOrNull(path: String, env: String): BigDecimal? =
            stringOrNull(path, env)?.toBigDecimalOrNull()
    }

    private fun validate(allowMissingJwtKeys: Boolean) {
        require(dbMinPoolSize > 0) { "DB_MIN_POOL_SIZE must be greater than 0." }
        require(dbMaxPoolSize >= dbMinPoolSize) { "DB_MAX_POOL_SIZE must be >= DB_MIN_POOL_SIZE." }
        require(dbConnectionTimeoutMs >= 1_000L) { "DB_CONNECTION_TIMEOUT_MS must be at least 1000." }

        if (isProduction) {
            requireProductionValue("SESSION_SECRET", jwtSecret, minLength = 32)
            requireProductionValue("REFRESH_TOKEN_PEPPER", refreshTokenPepper, minLength = 16)
            requireProductionValue("SIDECAR_SECRET", sidecarSecret, minLength = 16)
            requireProductionValue("APP_DOMAIN", appDomain)
            requireProductionValue("APP_ORIGIN", appOrigin)
            requireProductionValue("REDIS_URL", redisUrl)
            requireProductionValue("DATABASE_URL", databaseUrl.orEmpty())
            if (jwtPrivateKey.isNullOrBlank() || jwtPublicKey.isNullOrBlank()) {
                check(allowMissingJwtKeys) {
                    "JWT_PRIVATE_KEY and JWT_PUBLIC_KEY are required when ENV=production. " +
                        "Provide RSA PEM values before startup."
                }
            }
            check(maxTransactionValueEth != null && maxTransactionValueEth > BigDecimal.ZERO) {
                "MAX_TRANSACTION_VALUE_ETH must be set to a value greater than 0 when ENV=production."
            }
        }
    }

    private fun requireProductionValue(name: String, value: String, minLength: Int = 1) {
        check(value.isNotBlank() && value.length >= minLength && value !in productionDefaults) {
            "$name is missing or still set to a development default. " +
                "Set a production value before starting the backend."
        }
    }
}

enum class RuntimeEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
    TEST,
    ;

    companion object {
        fun from(explicit: String?, ktorDev: Boolean): RuntimeEnvironment =
            when (explicit?.trim()?.lowercase()) {
                "production", "prod" -> PRODUCTION
                "staging", "stage" -> STAGING
                "test" -> TEST
                "development", "dev" -> DEVELOPMENT
                else -> if (ktorDev) DEVELOPMENT else DEVELOPMENT
            }
    }
}

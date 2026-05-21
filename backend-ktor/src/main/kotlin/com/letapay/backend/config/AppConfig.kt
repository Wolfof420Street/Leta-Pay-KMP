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

data class AppConfig(
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
    val isDev: Boolean,
    val appDomain: String,
    val appOrigin: String,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            EnvLoader.load()
            val explicitSidecarUrl = config.stringOrNull(
                path = "agentkit.sidecarUrl",
                env = "AGENTKIT_SIDECAR_URL",
            )
            return AppConfig(
                // Fix: testApplication starts with an empty in-memory config, so fall back to env/defaults.
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
                isDev = config.propertyOrNull("ktor.development")?.getString()?.toBoolean() ?: true,
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
            )
        }

        private fun ApplicationConfig.stringOrNull(path: String, env: String): String? =
            propertyOrNull(path)?.getString()
                ?.takeIf(String::isNotBlank)
                ?: EnvLoader.get(env)?.takeIf(String::isNotBlank)

        private fun ApplicationConfig.string(path: String, env: String, default: String): String =
            stringOrNull(path, env) ?: default
    }
}

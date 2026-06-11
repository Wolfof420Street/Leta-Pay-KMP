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

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility to load environment variables from .env files based on the ENV variable.
 */
object EnvLoader {
    private val loadedVars = mutableMapOf<String, String>()
    private val loaded = AtomicBoolean(false)

    fun load() {
        if (!loaded.compareAndSet(false, true)) return

        val env = System.getenv("ENV") ?: System.getenv("ENVIRONMENT") ?: "dev"
        val fileName = when (env.lowercase()) {
            "staging" -> ".env.staging"
            "production", "prod" -> ".env"
            else -> ".env.dev"
        }

        val file = File(fileName)
        if (file.exists()) {
            file.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        val parts = trimmed.split("=", limit = 2)
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                        loadedVars[key] = value
                    }
                }
            }
        }
    }

    fun get(key: String): String? = loadedVars[key] ?: System.getenv(key)

    fun getOrDefault(key: String, default: String): String = get(key) ?: default
}

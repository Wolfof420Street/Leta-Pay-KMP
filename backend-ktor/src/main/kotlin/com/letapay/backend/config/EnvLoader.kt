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

/**
 * Utility to load environment variables from .env files based on the ENV variable.
 */
object EnvLoader {
    private val loadedVars = mutableMapOf<String, String>()

    fun load() {
        val env = System.getenv("ENV") ?: "dev"
        val fileName = when (env.lowercase()) {
            "staging" -> ".env.staging"
            else -> ".env.dev"
        }

        val file = File(fileName)
        if (file.exists()) {
            println("Loading environment from ${file.absolutePath}")
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
        } else {
            println("Warning: Environment file $fileName not found. Falling back to system environment.")
        }
    }

    fun get(key: String): String? = loadedVars[key] ?: System.getenv(key)

    fun getOrDefault(key: String, default: String): String = get(key) ?: default
}

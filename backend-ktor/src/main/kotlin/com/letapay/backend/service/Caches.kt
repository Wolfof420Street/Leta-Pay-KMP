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

import com.letapay.app.core.domain.KeyValueCache
import com.letapay.backend.model.ai.ParseResult

class PriceCache(
    private val cache: KeyValueCache<String>,
) {
    suspend fun get(key: String): String? = cache.get(key)

    suspend fun getEntry(key: String): Entry? = cache.getEntry(key)?.let { Entry(it.value, it.expiresAt) }

    suspend fun put(key: String, price: String, ttlSeconds: Long = 45L) {
        cache.put(key, price, ttlSeconds)
    }

    data class Entry(val price: String, val expiresAt: Long)
}

class ParseResultCache(
    private val cache: KeyValueCache<ParseResult>,
) {
    suspend fun get(key: String): ParseResult? = cache.get(key)

    suspend fun put(key: String, result: ParseResult, ttlSeconds: Long = 60L) {
        cache.put(key, result, ttlSeconds)
    }
}

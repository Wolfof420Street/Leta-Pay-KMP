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

import com.letapay.backend.model.ai.ParseResult
import java.util.concurrent.ConcurrentHashMap

class PriceCache {
    data class Entry(val price: String, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun get(key: String, now: Long = System.currentTimeMillis()): String? {
        val entry = cache[key] ?: return null
        return if (now < entry.expiresAt) entry.price else null
    }

    fun getEntry(key: String): Entry? = cache[key]

    fun put(key: String, price: String, ttlMs: Long = 45_000L) {
        cache[key] = Entry(price = price, expiresAt = System.currentTimeMillis() + ttlMs)
    }
}

class ParseResultCache {
    data class Entry(val result: ParseResult, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun get(key: String, now: Long = System.currentTimeMillis()): ParseResult? {
        val entry = cache[key] ?: return null
        return if (now < entry.expiresAt) entry.result else null
    }

    fun put(key: String, result: ParseResult, ttlMs: Long = 60_000L) {
        cache[key] = Entry(result = result, expiresAt = System.currentTimeMillis() + ttlMs)
    }
}

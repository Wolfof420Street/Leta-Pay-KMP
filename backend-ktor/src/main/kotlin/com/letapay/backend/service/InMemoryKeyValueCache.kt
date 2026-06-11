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
import com.letapay.app.core.domain.KeyValueEntry
import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueCache<V : Any> : KeyValueCache<V> {
    private val store = ConcurrentHashMap<String, CacheEntry<V>>()

    override suspend fun get(key: String): V? {
        val entry = store[key] ?: return null
        return if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key, entry)
            null
        } else {
            entry.value
        }
    }

    override suspend fun getEntry(key: String): KeyValueEntry<V>? {
        val entry = store[key] ?: return null
        return if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key, entry)
            null
        } else {
            KeyValueEntry(value = entry.value, expiresAt = entry.expiresAt)
        }
    }

    override suspend fun put(key: String, value: V, ttlSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000)
        store[key] = CacheEntry(value, expiresAt)
    }

    override suspend fun invalidate(key: String) {
        store.remove(key)
    }

    private data class CacheEntry<V>(val value: V, val expiresAt: Long)
}

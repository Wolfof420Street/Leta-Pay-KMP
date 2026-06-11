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
import com.letapay.app.core.domain.RateLimiter
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import io.lettuce.core.RedisClient as LettuceRedisClient

class RedisClient(redisUrl: String) : AutoCloseable {
    // When `redisUrl` is blank we operate in an in-memory fallback mode useful for tests and local dev.
    private val inMemory: Boolean = redisUrl.isBlank()

    // in-memory fallback state
    private val store = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long?>>()

    // Lettuce client (only if redisUrl provided)
    private val client: LettuceRedisClient? = if (!inMemory) {
        LettuceRedisClient.create(RedisURI.create(redisUrl))
    } else {
        null
    }
    private val connection: StatefulRedisConnection<String, String>? = client?.connect(StringCodec.UTF8)
    private val commands = connection?.coroutines()

    private fun nowMs(): Long = System.currentTimeMillis()

    suspend fun ping(): String =
        if (inMemory) {
            "PONG"
        } else {
            withContext(Dispatchers.IO) { commands!!.ping() ?: "PONG" }
        }

    suspend fun set(key: String, value: String, ttlSeconds: Long) {
        if (inMemory) {
            val expiry = if (ttlSeconds > 0) nowMs() + ttlSeconds * 1000 else null
            store[key] = value to expiry
        } else {
            withContext(Dispatchers.IO) { commands!!.set(key, value, SetArgs().ex(ttlSeconds)) }
        }
    }

    suspend fun get(key: String): String? =
        if (inMemory) {
            val pair = store[key] ?: return null
            val (v, expiry) = pair
            if (expiry != null && expiry < nowMs()) {
                store.remove(key)
                null
            } else {
                v
            }
        } else {
            withContext(Dispatchers.IO) { commands!!.get(key) }
        }

    suspend fun del(key: String) {
        if (inMemory) {
            store.remove(key)
        } else {
            withContext(Dispatchers.IO) { commands!!.del(key) }
        }
    }

    suspend fun pttl(key: String): Long =
        if (inMemory) {
            val pair = store[key] ?: return -1L
            val expiry = pair.second ?: return -1L
            (expiry - nowMs()).coerceAtLeast(-1L)
        } else {
            withContext(Dispatchers.IO) { commands!!.pttl(key) ?: -1L }
        }

    suspend fun incr(key: String): Long =
        if (inMemory) {
            store.compute(key) { _, old ->
                val curr = old?.first?.toLongOrNull() ?: 0L
                val next = (curr + 1L).toString()
                next to old?.second
            }
            store[key]!!.first.toLong()
        } else {
            withContext(Dispatchers.IO) { commands!!.incr(key) ?: 0L }
        }

    suspend fun expire(key: String, ttlSeconds: Long) {
        if (inMemory) {
            store.computeIfPresent(key) { _, old ->
                val expiry = nowMs() + ttlSeconds * 1000
                old.first to expiry
            }
        } else {
            withContext(Dispatchers.IO) { commands!!.expire(key, ttlSeconds) }
        }
    }

    override fun close() {
        connection?.close()
        client?.close()
        store.clear()
    }
}

class RedisKeyValueCache<V : Any>(
    private val redisClient: RedisClient,
    private val json: Json,
    private val serializer: KSerializer<V>,
    private val namespace: String,
) : KeyValueCache<V> {
    override suspend fun get(key: String): V? =
        redisClient.get(namespaced(key))?.let { json.decodeFromString(serializer, it) }

    override suspend fun getEntry(key: String): KeyValueEntry<V>? {
        val redisKey = namespaced(key)
        val raw = redisClient.get(redisKey) ?: return null
        val ttlMs = redisClient.pttl(redisKey)
        return KeyValueEntry(
            value = json.decodeFromString(serializer, raw),
            expiresAt = System.currentTimeMillis() + ttlMs.coerceAtLeast(0L),
        )
    }

    override suspend fun put(key: String, value: V, ttlSeconds: Long) {
        redisClient.set(namespaced(key), json.encodeToString(serializer, value), ttlSeconds)
    }

    override suspend fun invalidate(key: String) {
        redisClient.del(namespaced(key))
    }

    private fun namespaced(key: String): String = "$namespace:$key"
}

class RedisRateLimiter(
    private val redisClient: RedisClient,
    private val namespace: String = "rate_limit",
) : RateLimiter {
    override suspend fun isAllowed(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val windowId = System.currentTimeMillis() / (windowSeconds * 1000)
        val redisKey = "$namespace:$key:$windowId"
        val count = redisClient.incr(redisKey)
        if (count == 1L) {
            redisClient.expire(redisKey, windowSeconds)
        }
        return count <= maxRequests
    }

    override suspend fun reset(key: String) {
        redisClient.del("$namespace:$key")
    }
}

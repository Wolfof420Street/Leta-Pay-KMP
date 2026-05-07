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

import com.letapay.backend.error.RateLimitExceededError
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class RateLimiterService {
    private val windows = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun enforce(key: String, limit: Int, windowMs: Long) {
        val now = System.currentTimeMillis()
        val timestamps = windows.computeIfAbsent(key) { ArrayDeque() }
        synchronized(timestamps) {
            while (timestamps.isNotEmpty() && now - timestamps.first() >= windowMs) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= limit) {
                val retryAfterSeconds = ((windowMs - (now - timestamps.first())) / 1000).toInt()
                    .coerceAtLeast(1)
                throw RateLimitExceededError(retryAfterSeconds)
            }
            timestamps.addLast(now)
        }
    }
}

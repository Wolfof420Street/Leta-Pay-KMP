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

import com.letapay.app.core.domain.RateLimiter
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class InMemoryRateLimiter : RateLimiter {
    private val windows = ConcurrentHashMap<String, ArrayDeque<Long>>()

    override suspend fun isAllowed(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val now = System.currentTimeMillis()
        val windowMs = windowSeconds * 1000
        val timestamps = windows.computeIfAbsent(key) { ArrayDeque() }
        return synchronized(timestamps) {
            while (timestamps.isNotEmpty() && now - timestamps.first() >= windowMs) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= maxRequests) {
                false
            } else {
                timestamps.addLast(now)
                true
            }
        }
    }

    override suspend fun reset(key: String) {
        windows.remove(key)
    }
}

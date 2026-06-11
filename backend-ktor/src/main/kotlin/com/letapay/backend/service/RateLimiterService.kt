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
import com.letapay.backend.error.RateLimitExceededError
import kotlin.math.ceil

class RateLimiterService(
    private val rateLimiter: RateLimiter,
) {
    suspend fun enforce(key: String, limit: Int, windowMs: Long) {
        require(windowMs > 0) { "windowMs must be > 0" }
        val windowSeconds = ceil(windowMs / 1000.0).toLong()
        val allowed = rateLimiter.isAllowed(key, limit, windowSeconds)
        if (!allowed) {
            // Approximation for retry-after because limiter API is second-granularity.
            throw RateLimitExceededError(retryAfterSeconds = ceil(windowMs / 1000.0).toInt())
        }
    }
}

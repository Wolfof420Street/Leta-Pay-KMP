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

class RateLimiterService(
    private val rateLimiter: RateLimiter,
) {
    suspend fun enforce(key: String, limit: Int, windowMs: Long) {
        val allowed = rateLimiter.isAllowed(key, limit, windowMs / 1000)
        if (!allowed) {
            // Approximation for retry after
            throw RateLimitExceededError(retryAfterSeconds = (windowMs / 1000).toInt())
        }
    }
}

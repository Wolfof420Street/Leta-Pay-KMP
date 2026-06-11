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
import com.letapay.backend.error.QuoteExpiredError
import com.letapay.backend.model.swap.SwapQuote

class SwapQuoteCacheService(
    private val cache: KeyValueCache<SwapQuote>,
) {
    suspend fun putQuote(quote: SwapQuote) {
        cache.put(quote.quoteId, quote, (quote.expiresAt - System.currentTimeMillis()) / 1000)
    }

    suspend fun requireActiveQuote(quoteId: String): SwapQuote {
        return cache.get(quoteId) ?: throw QuoteExpiredError()
    }
}

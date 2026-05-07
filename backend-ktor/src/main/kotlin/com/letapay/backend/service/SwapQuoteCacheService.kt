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

import com.letapay.backend.error.QuoteExpiredError
import com.letapay.backend.model.swap.SwapQuote
import java.util.concurrent.ConcurrentHashMap

class SwapQuoteCacheService {
    private val quotes = ConcurrentHashMap<String, SwapQuote>()

    fun putQuote(quote: SwapQuote) {
        pruneExpiredEntries(System.currentTimeMillis())
        quotes[quote.quoteId] = quote
    }

    fun requireActiveQuote(quoteId: String, now: Long = System.currentTimeMillis()): SwapQuote {
        pruneExpiredEntries(now)
        val quote = quotes[quoteId] ?: throw QuoteExpiredError()
        if (quote.expiresAt <= now) {
            quotes.remove(quoteId)
            throw QuoteExpiredError()
        }
        return quote
    }

    private fun pruneExpiredEntries(now: Long) {
        quotes.entries.removeIf { (_, quote) -> quote.expiresAt <= now }
    }
}

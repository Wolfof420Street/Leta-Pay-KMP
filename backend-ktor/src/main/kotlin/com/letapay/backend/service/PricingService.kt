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

import com.letapay.backend.model.price.PriceQuote

interface PricingService {
    suspend fun quote(chain: String, asset: String): PriceQuote
}

class DefaultPricingService(
    private val coinbaseService: CoinbaseService,
) : PricingService {
    override suspend fun quote(chain: String, asset: String): PriceQuote =
        coinbaseService.getSpotPrice(
            fromAsset = asset.uppercase(),
            toAsset = "USD",
            chain = chain.toLongOrNull() ?: 1L,
        ).let { quote ->
            PriceQuote(
                asset = asset.uppercase(),
                chain = chain.lowercase(),
                fiatCurrency = "USD",
                price = quote.price,
                stale = false,
            )
        }
}

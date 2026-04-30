/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.domain

interface PriceQuoteRepository {
    /**
     * Fetches current USD conversion rate for a given asset on a specific chain.
     */
    suspend fun getUsdRate(assetId: String, chainId: Long): Double

    /**
     * Estimates USD impact of slippage or network bridging paths.
     */
    suspend fun estimateBridgeUsdImpact(amount: Double, assetId: String): Double
}

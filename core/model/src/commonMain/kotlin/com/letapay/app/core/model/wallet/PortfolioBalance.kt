/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.model.wallet

import com.letapay.app.core.model.blockchain.AssetId
import com.letapay.app.core.model.blockchain.ChainId
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioBalance(
    val balances: List<AssetBalance>,
    val totalUsdValue: String? = null,
    val updatedAtEpochMillis: Long? = null,
) {
    companion object {
        val EMPTY = PortfolioBalance(balances = emptyList())
    }
}

@Serializable
data class AssetBalance(
    val assetId: AssetId,
    val symbol: String,
    val amount: String,
    val chainId: ChainId,
    val usdValue: String? = null,
)

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repository

import com.letapay.app.core.model.wallet.PortfolioBalance
import kotlinx.coroutines.flow.StateFlow

data class PortfolioState(
    val isLoading: Boolean = false,
    val balance: PortfolioBalance = PortfolioBalance.EMPTY,
    val errorMessage: String? = null,
)

interface PortfolioRepository {
    val portfolioState: StateFlow<PortfolioState>

    suspend fun refreshBalance()
}

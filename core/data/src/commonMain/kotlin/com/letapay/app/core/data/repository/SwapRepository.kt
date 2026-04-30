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

import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.swap.SpotPrice
import com.letapay.app.core.model.swap.SwapExecuteResponse
import com.letapay.app.core.model.swap.SwapQuoteRequest
import com.letapay.app.core.model.swap.SwapQuoteResponse
import kotlinx.coroutines.flow.Flow

interface SwapRepository {
    fun getSpotPrice(fromAsset: String, toAsset: String, chainId: Long): Flow<Resource<SpotPrice>>
    suspend fun quote(request: SwapQuoteRequest): Resource<SwapQuoteResponse>
    suspend fun execute(quoteId: String): Resource<SwapExecuteResponse>
}

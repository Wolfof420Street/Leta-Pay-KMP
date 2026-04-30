/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repositoryImpl

import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.data.repository.SwapRepository
import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.swap.SpotPrice
import com.letapay.app.core.model.swap.SwapExecuteResponse
import com.letapay.app.core.model.swap.SwapQuoteRequest
import com.letapay.app.core.model.swap.SwapQuoteResponse
import com.letapay.app.core.common.UUIDGenerator
import com.letapay.app.core.network.swap.SwapApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SwapRepositoryImpl(
    private val swapApi: SwapApi,
    private val sessionRepository: SessionRepository,
) : SwapRepository {

    override fun getSpotPrice(fromAsset: String, toAsset: String, chainId: Long): Flow<Resource<SpotPrice>> = flow {
        emit(Resource.Loading())
        val sessionToken = sessionRepository.sessionState.value.session?.sessionToken ?: throw Exception("Unauthorized")
        val quote = swapApi.quote(
            sessionToken = sessionToken,
            request = SwapQuoteRequest(
                fromAsset = fromAsset,
                toAsset = toAsset,
                amount = "1",
                chain = chainId,
            ),
        )
        emit(Resource.Success(SpotPrice(fromAsset = fromAsset, toAsset = toAsset, chain = chainId, price = quote.rate)))
    }

    override suspend fun quote(request: SwapQuoteRequest): Resource<SwapQuoteResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw Exception("Unauthorized")
            val result = swapApi.quote(sessionToken, request)
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }

    override suspend fun execute(quoteId: String): Resource<SwapExecuteResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw Exception("Unauthorized")
            val result = swapApi.execute(sessionToken, quoteId, UUIDGenerator.generateUUID())
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }
}

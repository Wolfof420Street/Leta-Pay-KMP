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

import com.letapay.app.core.data.repository.PortfolioRepository
import com.letapay.app.core.data.repository.PortfolioState
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.network.portfolio.PortfolioApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import template.core.base.common.manager.DispatcherManager

class PortfolioRepositoryImpl(
    private val portfolioApi: PortfolioApi,
    private val sessionRepository: SessionRepository,
    dispatcherManager: DispatcherManager,
) : PortfolioRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherManager.main)
    private val mutablePortfolioState = MutableStateFlow(PortfolioState())

    override val portfolioState: StateFlow<PortfolioState> = mutablePortfolioState.asStateFlow()

    init {
        scope.launch {
            sessionRepository.sessionState
                .map { it.session?.sessionToken }
                .distinctUntilChanged()
                .collect { token ->
                    if (token.isNullOrBlank()) {
                        mutablePortfolioState.value = PortfolioState()
                    } else {
                        refreshBalance()
                    }
                }
        }
    }

    override suspend fun refreshBalance() {
        val sessionToken = sessionRepository.sessionState.value.session?.sessionToken ?: run {
            mutablePortfolioState.value = PortfolioState()
            return
        }

        mutablePortfolioState.update { it.copy(isLoading = true, errorMessage = null) }

        runCatching {
            portfolioApi.fetchBalance(sessionToken)
        }.onSuccess { balance ->
            mutablePortfolioState.update {
                it.copy(
                    isLoading = false,
                    balance = balance,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            mutablePortfolioState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to fetch balances.",
                )
            }
        }
    }
}

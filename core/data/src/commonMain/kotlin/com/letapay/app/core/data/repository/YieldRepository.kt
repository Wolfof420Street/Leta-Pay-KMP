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
import com.letapay.app.core.model.yield.StakeResponse
import com.letapay.app.core.model.yield.StakingPosition
import com.letapay.app.core.model.yield.UnstakeResponse
import com.letapay.app.core.model.yield.YieldOpportunity
import kotlinx.coroutines.flow.Flow

interface YieldRepository {
    fun getOpportunities(): Flow<Resource<List<YieldOpportunity>>>
    fun getStakingPositions(): Flow<Resource<List<StakingPosition>>>
    suspend fun stake(opportunityId: String, amount: String): Resource<StakeResponse>
    suspend fun unstake(positionId: String, amount: String): Resource<UnstakeResponse>
}

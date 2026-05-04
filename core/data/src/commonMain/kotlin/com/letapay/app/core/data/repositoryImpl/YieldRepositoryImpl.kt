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

import com.letapay.app.core.common.UUIDGenerator
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.data.repository.YieldRepository
import com.letapay.app.core.database.AppDatabase
import com.letapay.app.core.database.entity.StakingPositionEntity
import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.yield.StakeResponse
import com.letapay.app.core.model.yield.StakingPosition
import com.letapay.app.core.model.yield.StakingStatus
import com.letapay.app.core.model.yield.UnstakeResponse
import com.letapay.app.core.model.yield.YieldOpportunity
import com.letapay.app.core.network.yield.YieldApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class YieldRepositoryImpl(
    private val yieldApi: YieldApi,
    private val sessionRepository: SessionRepository,
    private val appDatabase: AppDatabase,
) : YieldRepository {

    override fun getOpportunities(): Flow<Resource<List<YieldOpportunity>>> = flow {
        emit(Resource.Loading())
        try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw Exception("Unauthorized")
            val result = yieldApi.getOpportunities(sessionToken)
            emit(Resource.Success(result))
        } catch (throwable: Throwable) {
            emit(Resource.Error(throwable.toAppError()))
        }
    }

    override fun getStakingPositions(): Flow<Resource<List<StakingPosition>>> = flow {
        emit(Resource.Loading())
        val walletAddress = sessionRepository.sessionState.value.session?.walletAddress?.value
            ?: throw Exception("Unauthorized")
        val localPositions = appDatabase.stakingPositionDao.observePositions(walletAddress).first().map { it.toModel() }
        emit(Resource.Success(localPositions))
        val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
            ?: throw Exception("Unauthorized")
        val remotePositions = yieldApi.getPositions(sessionToken)
        appDatabase.stakingPositionDao.replacePositions(
            walletAddress = walletAddress,
            positions = remotePositions.map { it.toEntity() },
        )
        emit(
            Resource.Success(
                appDatabase.stakingPositionDao.observePositions(walletAddress).first().map { it.toModel() },
            ),
        )
    }

    override suspend fun stake(
        opportunityId: String,
        amount: String,
    ): Resource<StakeResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw Exception("Unauthorized")
            val result = yieldApi.stake(
                sessionToken,
                opportunityId,
                amount,
                UUIDGenerator.generateUUID(),
            )
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }

    override suspend fun unstake(
        positionId: String,
        amount: String,
    ): Resource<UnstakeResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw Exception("Unauthorized")
            val result = yieldApi.unstake(
                sessionToken,
                positionId,
                amount,
                UUIDGenerator.generateUUID(),
            )
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }
}

private fun StakingPosition.toEntity(): StakingPositionEntity =
    StakingPositionEntity(
        positionId = positionId,
        walletAddress = walletAddress,
        chainId = chain,
        opportunityId = opportunityId,
        amount = stakedAmount,
        status = status.name,
        updatedAt = createdAt,
    )

private fun StakingPositionEntity.toModel(): StakingPosition =
    StakingPosition(
        positionId = positionId,
        opportunityId = opportunityId,
        walletAddress = walletAddress,
        stakedAmount = amount,
        currentValue = amount,
        currentValueUsd = "0.00",
        accruedRewards = "0.00",
        status = runCatching { StakingStatus.valueOf(status) }.getOrDefault(StakingStatus.Active),
        chain = chainId,
        createdAt = updatedAt,
    )

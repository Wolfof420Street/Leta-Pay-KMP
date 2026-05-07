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
import com.letapay.app.core.data.repository.TransactionRepository
import com.letapay.app.core.database.AppDatabase
import com.letapay.app.core.database.entity.TransactionEntity
import com.letapay.app.core.model.error.AppError.SessionExpiredError
import com.letapay.app.core.model.payment.TransactionStatus
import com.letapay.app.core.model.result.Resource
import com.letapay.app.core.model.transaction.BuildRequest
import com.letapay.app.core.model.transaction.BuildResponse
import com.letapay.app.core.model.transaction.SendResponse
import com.letapay.app.core.network.transaction.TransactionApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class TransactionRepositoryImpl(
    private val transactionApi: TransactionApi,
    private val sessionRepository: SessionRepository,
    private val appDatabase: AppDatabase,
) : TransactionRepository {

    private val transactionsMutex = Mutex()

    override fun getTransactions(): Flow<Resource<List<Any>>> = flow {
        emit(Resource.Loading())
        val local = appDatabase.transactionDao.getAllTransactions().first().map { it.toFeedRow() }
        emit(Resource.Success(local))
        val sessionToken = sessionRepository.sessionState.value.session?.sessionToken ?: return@flow
        transactionsMutex.withLock {
            runCatching {
                transactionApi.fetchActivity(sessionToken)
            }.onSuccess { records ->
                records.forEach { record ->
                    appDatabase.transactionDao.upsertTransaction(
                        TransactionEntity(
                            idempotencyKey = record.txHash,
                            txHash = record.txHash,
                            assetId = "UNKNOWN",
                            chainId = 1L,
                            amount = "0",
                            status = record.status.toTransactionStatus(),
                            updatedAt = record.createdAt,
                        ),
                    )
                }
                emit(Resource.Success(appDatabase.transactionDao.getAllTransactions().first().map { it.toFeedRow() }))
            }.onFailure { throwable ->
                emit(Resource.Error(throwable.toAppError()))
            }
        }
    }

    override suspend fun buildTransaction(request: BuildRequest): Resource<BuildResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw SessionExpiredError()
            val result = transactionApi.buildTransaction(sessionToken, request)
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }

    override suspend fun sendTransaction(
        signedTx: String,
        toAddress: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): Resource<SendResponse> {
        return try {
            val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
                ?: throw SessionExpiredError()
            val idempotencyKey = UUIDGenerator.generateUUID()
            val now = Clock.System.now().toEpochMilliseconds()
            appDatabase.transactionDao.upsertTransaction(
                TransactionEntity(
                    idempotencyKey = idempotencyKey,
                    assetId = asset,
                    chainId = chainId,
                    amount = amount,
                    status = TransactionStatus.Broadcasting,
                    updatedAt = now,
                ),
            )
            val result = transactionApi.sendTransaction(
                sessionToken = sessionToken,
                signedTx = signedTx,
                toAddress = toAddress,
                asset = asset,
                amount = amount,
                chainId = chainId,
                idempotencyKey = idempotencyKey,
            )
            val statusResult = runCatching {
                transactionApi.fetchTransactionStatus(sessionToken, result.txHash)
            }.getOrNull()
            appDatabase.transactionDao.upsertTransaction(
                TransactionEntity(
                    idempotencyKey = idempotencyKey,
                    txHash = result.txHash,
                    assetId = asset,
                    chainId = chainId,
                    amount = amount,
                    status = statusResult?.status?.toTransactionStatus() ?: result.status.toTransactionStatus(),
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                ),
            )
            Resource.Success(result)
        } catch (throwable: Throwable) {
            Resource.Error(throwable.toAppError())
        }
    }
}

private fun TransactionEntity.toFeedRow(): Any = mapOf(
    "idempotencyKey" to idempotencyKey,
    "txHash" to txHash,
    "assetId" to assetId,
    "chainId" to chainId,
    "amount" to amount,
    "status" to status.name,
    "updatedAt" to updatedAt,
)

private fun String.toTransactionStatus(): TransactionStatus = when (lowercase()) {
    "draft" -> TransactionStatus.Draft
    "estimatinggas", "estimating_gas" -> TransactionStatus.EstimatingGas
    "awaitingwalletapproval", "awaiting_wallet_approval" -> TransactionStatus.AwaitingWalletApproval
    "signed" -> TransactionStatus.Signed
    "broadcasting" -> TransactionStatus.Broadcasting
    "submitted", "pending" -> TransactionStatus.Submitted
    "confirmed", "success" -> TransactionStatus.Confirmed
    "cancelled", "canceled" -> TransactionStatus.Cancelled
    else -> TransactionStatus.Failed
}

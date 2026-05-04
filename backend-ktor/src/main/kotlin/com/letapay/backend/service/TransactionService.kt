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

import com.letapay.backend.db.Transactions
import com.letapay.backend.model.transaction.TransactionRecord
import com.letapay.backend.model.transaction.TransactionStatusResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

interface TransactionService {
    suspend fun recordSubmitted(
        walletAddress: String,
        toAddress: String,
        signedTx: String,
        asset: String = "ETH",
        amount: String = "0",
        chainId: Long = 1L,
    ): TransactionStatusResponse

    suspend fun history(walletAddress: String): List<TransactionRecord>

    suspend fun status(walletAddress: String, txHash: String): TransactionStatusResponse
}

class DatabaseTransactionService : TransactionService {
    override suspend fun recordSubmitted(
        walletAddress: String,
        toAddress: String,
        signedTx: String,
        asset: String,
        amount: String,
        chainId: Long,
    ): TransactionStatusResponse {
        val txHash = "0x${UUID.randomUUID().toString().replace("-", "").padEnd(64, '0').take(64)}"
        val createdAt = System.currentTimeMillis()
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.insert {
                it[Transactions.txHash] = txHash
                it[Transactions.walletAddress] = walletAddress
                it[Transactions.toAddress] = toAddress
                it[Transactions.status] = "submitted"
                it[Transactions.signedTx] = signedTx
                it[Transactions.notificationType] = "TX_CONFIRMED"
                it[Transactions.asset] = asset
                it[Transactions.amount] = amount
                it[Transactions.chainId] = chainId
                it[Transactions.createdAt] = createdAt
            }
        }
        return TransactionStatusResponse(txHash = txHash, status = "submitted")
    }

    override suspend fun history(walletAddress: String): List<TransactionRecord> =
        newSuspendedTransaction(Dispatchers.IO) {
            Transactions.selectAll()
                .where { Transactions.walletAddress eq walletAddress }
                .map { row ->
                    TransactionRecord(
                        txHash = row[Transactions.txHash],
                        walletAddress = row[Transactions.walletAddress],
                        toAddress = row[Transactions.toAddress],
                        status = row[Transactions.status],
                        createdAt = row[Transactions.createdAt],
                    )
                }
                .sortedByDescending(TransactionRecord::createdAt)
        }

    override suspend fun status(walletAddress: String, txHash: String): TransactionStatusResponse =
        newSuspendedTransaction(Dispatchers.IO) {
            val row = Transactions.selectAll()
                .where {
                    (Transactions.walletAddress eq walletAddress) and
                        (Transactions.txHash eq txHash)
                }
                .singleOrNull()

            TransactionStatusResponse(
                txHash = txHash,
                status = row?.get(Transactions.status) ?: "unknown",
            )
        }
}

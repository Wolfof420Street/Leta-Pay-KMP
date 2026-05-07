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

import com.letapay.backend.db.PendingNotifications
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

data class PendingNotificationRecord(
    val id: String,
    val walletAddress: String,
    val txHash: String,
    val chain: Long,
    val asset: String,
    val amount: String,
    val notificationType: String,
    val status: String,
    val watchUntil: Long,
)

interface PendingNotificationService {
    suspend fun register(
        walletAddress: String,
        txHash: String,
        chain: Long,
        asset: String,
        amount: String,
        notificationType: String,
        watchUntil: Long = System.currentTimeMillis() + 15 * 60_000L,
    )

    suspend fun watching(): List<PendingNotificationRecord>

    suspend fun updateStatus(id: String, status: String)
}

class DatabasePendingNotificationService : PendingNotificationService {
    override suspend fun register(
        walletAddress: String,
        txHash: String,
        chain: Long,
        asset: String,
        amount: String,
        notificationType: String,
        watchUntil: Long,
    ) {
        newSuspendedTransaction(Dispatchers.IO) {
            PendingNotifications.insert {
                it[id] = UUID.randomUUID().toString()
                it[PendingNotifications.walletAddress] = walletAddress
                it[PendingNotifications.txHash] = txHash
                it[PendingNotifications.chain] = chain
                it[PendingNotifications.asset] = asset
                it[PendingNotifications.amount] = amount
                it[PendingNotifications.notificationType] = notificationType
                it[status] = "WATCHING"
                it[PendingNotifications.watchUntil] = watchUntil
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun watching(): List<PendingNotificationRecord> =
        newSuspendedTransaction(Dispatchers.IO) {
            PendingNotifications.selectAll()
                .where { PendingNotifications.status eq "WATCHING" }
                .map(::toRecord)
        }

    override suspend fun updateStatus(id: String, status: String) {
        newSuspendedTransaction(Dispatchers.IO) {
            PendingNotifications.update({ PendingNotifications.id eq id }) {
                it[PendingNotifications.status] = status
            }
        }
    }

    private fun toRecord(row: ResultRow): PendingNotificationRecord =
        PendingNotificationRecord(
            id = row[PendingNotifications.id],
            walletAddress = row[PendingNotifications.walletAddress],
            txHash = row[PendingNotifications.txHash],
            chain = row[PendingNotifications.chain],
            asset = row[PendingNotifications.asset],
            amount = row[PendingNotifications.amount],
            notificationType = row[PendingNotifications.notificationType],
            status = row[PendingNotifications.status],
            watchUntil = row[PendingNotifications.watchUntil],
        )
}

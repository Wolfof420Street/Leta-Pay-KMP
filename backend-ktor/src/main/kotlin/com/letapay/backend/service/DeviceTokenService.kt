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

import com.letapay.backend.db.DeviceTokens
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

data class DeviceTokenRecord(
    val id: String,
    val walletAddress: String,
    val fcmToken: String,
    val platform: String,
    val active: Boolean,
)

interface DeviceTokenService {
    suspend fun upsert(walletAddress: String, fcmToken: String, platform: String)

    suspend fun deactivate(walletAddress: String, fcmToken: String)

    suspend fun deactivateToken(fcmToken: String)

    suspend fun activeTokens(walletAddress: String): List<DeviceTokenRecord>

    suspend fun records(walletAddress: String, platform: String? = null): List<DeviceTokenRecord>
}

class DatabaseDeviceTokenService : DeviceTokenService {
    override suspend fun upsert(walletAddress: String, fcmToken: String, platform: String) {
        val normalizedPlatform = platform.lowercase()
        val now = System.currentTimeMillis()
        newSuspendedTransaction(Dispatchers.IO) {
            // Fix: deactivate older tokens first so only one active token survives per wallet+platform.
            DeviceTokens.update({
                (DeviceTokens.walletAddress eq walletAddress) and (DeviceTokens.platform eq normalizedPlatform)
            }) {
                it[active] = false
                it[updatedAt] = now
            }
            DeviceTokens.insert {
                it[id] = UUID.randomUUID().toString()
                it[DeviceTokens.walletAddress] = walletAddress
                it[DeviceTokens.fcmToken] = fcmToken
                it[DeviceTokens.platform] = normalizedPlatform
                it[updatedAt] = now
                it[active] = true
            }
        }
    }

    override suspend fun deactivate(walletAddress: String, fcmToken: String) {
        newSuspendedTransaction(Dispatchers.IO) {
            DeviceTokens.update({
                (DeviceTokens.walletAddress eq walletAddress) and (DeviceTokens.fcmToken eq fcmToken)
            }) {
                it[active] = false
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun deactivateToken(fcmToken: String) {
        newSuspendedTransaction(Dispatchers.IO) {
            DeviceTokens.update({ DeviceTokens.fcmToken eq fcmToken }) {
                it[active] = false
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun activeTokens(walletAddress: String): List<DeviceTokenRecord> =
        newSuspendedTransaction(Dispatchers.IO) {
            DeviceTokens.selectAll()
                .where { (DeviceTokens.walletAddress eq walletAddress) and (DeviceTokens.active eq true) }
                .map(::toRecord)
        }

    override suspend fun records(walletAddress: String, platform: String?): List<DeviceTokenRecord> =
        newSuspendedTransaction(Dispatchers.IO) {
            DeviceTokens.selectAll()
                .where {
                    if (platform == null) {
                        DeviceTokens.walletAddress eq walletAddress
                    } else {
                        (DeviceTokens.walletAddress eq walletAddress) and
                            (DeviceTokens.platform eq platform.lowercase())
                    }
                }
                .map(::toRecord)
        }

    private fun toRecord(row: org.jetbrains.exposed.sql.ResultRow): DeviceTokenRecord =
        DeviceTokenRecord(
            id = row[DeviceTokens.id],
            walletAddress = row[DeviceTokens.walletAddress],
            fcmToken = row[DeviceTokens.fcmToken],
            platform = row[DeviceTokens.platform],
            active = row[DeviceTokens.active],
        )
}

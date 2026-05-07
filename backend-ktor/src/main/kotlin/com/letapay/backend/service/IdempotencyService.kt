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

import com.letapay.backend.db.IdempotencyKeys
import com.letapay.backend.error.IdempotencyConflictError
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

data class IdempotencyReplay(
    val statusCode: Int,
    val payload: String,
)

interface IdempotencyService {
    suspend fun checkReplay(
        key: String,
        walletAddress: String,
        endpoint: String,
        payloadHash: String,
    ): IdempotencyReplay?

    suspend fun registerKey(key: String, walletAddress: String, endpoint: String, payloadHash: String)

    suspend fun complete(
        key: String,
        walletAddress: String,
        endpoint: String,
        statusCode: Int,
        payload: String,
    )
}

class DatabaseIdempotencyService(
    private val json: Json,
) : IdempotencyService {
    override suspend fun checkReplay(
        key: String,
        walletAddress: String,
        endpoint: String,
        payloadHash: String,
    ): IdempotencyReplay? {
        val now = System.currentTimeMillis()
        return newSuspendedTransaction(Dispatchers.IO) {
            val existing = IdempotencyKeys.selectAll()
                .where {
                    (IdempotencyKeys.key eq key) and
                        (IdempotencyKeys.walletAddress eq walletAddress) and
                        (IdempotencyKeys.endpoint eq endpoint)
                }
                .singleOrNull() ?: return@newSuspendedTransaction null

            if (existing[IdempotencyKeys.expiresAt] <= now) {
                return@newSuspendedTransaction null
            }
            val existingHash = existing[IdempotencyKeys.requestHash]
            if (existingHash != null && existingHash != payloadHash) {
                throw IdempotencyConflictError()
            }

            val payload = existing[IdempotencyKeys.responseSnapshot]
                ?: return@newSuspendedTransaction IdempotencyReplay(
                    statusCode = 202,
                    payload = json.encodeToString(mapOf("status" to "processing")),
                )
            IdempotencyReplay(
                statusCode = existing[IdempotencyKeys.responseStatus] ?: 200,
                payload = payload,
            )
        }
    }

    override suspend fun registerKey(
        key: String,
        walletAddress: String,
        endpoint: String,
        payloadHash: String,
    ) {
        val now = System.currentTimeMillis()
        newSuspendedTransaction(Dispatchers.IO) {
            IdempotencyKeys.insertIgnore {
                it[IdempotencyKeys.key] = key
                it[IdempotencyKeys.walletAddress] = walletAddress
                it[IdempotencyKeys.endpoint] = endpoint
                it[IdempotencyKeys.requestHash] = payloadHash
                it[createdAt] = now
                it[expiresAt] = now + 24 * 60 * 60 * 1000
                it[responseSnapshot] = null
                it[responseStatus] = null
            }
        }
    }

    override suspend fun complete(
        key: String,
        walletAddress: String,
        endpoint: String,
        statusCode: Int,
        payload: String,
    ) {
        newSuspendedTransaction(Dispatchers.IO) {
            IdempotencyKeys.update({
                (IdempotencyKeys.key eq key) and
                    (IdempotencyKeys.walletAddress eq walletAddress) and
                    (IdempotencyKeys.endpoint eq endpoint)
            }) {
                it[responseSnapshot] = payload
                it[responseStatus] = statusCode
            }
        }
    }
}

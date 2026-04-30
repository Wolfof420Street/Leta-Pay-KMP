/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.letapay.app.core.common.DevicePlatform
import com.letapay.app.core.database.dao.ChatMessageDao
import com.letapay.app.core.database.dao.ContactDao
import com.letapay.app.core.database.dao.DeviceTokenDao
import com.letapay.app.core.database.dao.PendingMessageDao
import com.letapay.app.core.database.dao.TransactionDao
import com.letapay.app.core.database.entity.ChatMessageEntity
import com.letapay.app.core.database.entity.ContactEntity
import com.letapay.app.core.database.entity.DeviceTokenEntity
import com.letapay.app.core.database.entity.PendingMessageEntity
import com.letapay.app.core.database.entity.TransactionEntity
import com.letapay.app.core.model.chat.ChatMessageStatus
import com.letapay.app.core.model.payment.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class SqlDelightAppDatabase(
    driver: SqlDriver,
) : AppDatabase {

    private val database = LetaPayDatabase(driver)

    override val transactionDao: TransactionDao = SqlDelightTransactionDao(database)
    override val deviceTokenDao: DeviceTokenDao = SqlDelightDeviceTokenDao(database)
    override val chatMessageDao: ChatMessageDao = SqlDelightChatMessageDao(database)
    override val pendingMessageDao: PendingMessageDao = SqlDelightPendingMessageDao(database)
    override val contactDao: ContactDao = SqlDelightContactDao(database)
}

private class SqlDelightDeviceTokenDao(
    private val database: LetaPayDatabase,
) : DeviceTokenDao {
    override fun getDeviceToken(): DeviceTokenEntity? {
        return database.letaPayDatabaseQueries
            .selectDeviceToken { token, platform, registeredAt ->
                DeviceTokenEntity(
                    token = token,
                    platform = runCatching { DevicePlatform.valueOf(platform) }.getOrDefault(DevicePlatform.Web),
                    registeredAt = registeredAt,
                )
            }
            .executeAsOneOrNull()
    }

    override suspend fun upsertDeviceToken(token: DeviceTokenEntity) {
        database.letaPayDatabaseQueries.upsertDeviceToken(
            token = token.token,
            platform = token.platform.name,
            registered_at = token.registeredAt,
        )
    }

    override suspend fun clearDeviceToken() {
        database.letaPayDatabaseQueries.clearDeviceTokens()
    }
}

private class SqlDelightTransactionDao(
    private val database: LetaPayDatabase,
) : TransactionDao {
    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return database.letaPayDatabaseQueries
            .selectTransactionRecords { idempotencyKey, txHash, assetId, chainId, amount, status, updatedAt ->
                TransactionEntity(
                    idempotencyKey = idempotencyKey,
                    txHash = txHash,
                    assetId = assetId,
                    chainId = chainId,
                    amount = amount,
                    status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.Failed),
                    updatedAt = updatedAt,
                )
            }
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun upsertTransaction(transaction: TransactionEntity) {
        database.letaPayDatabaseQueries.upsertTransactionRecord(
            idempotency_key = transaction.idempotencyKey,
            tx_hash = transaction.txHash,
            asset_id = transaction.assetId,
            chain_id = transaction.chainId,
            amount = transaction.amount,
            status = transaction.status.name,
            updated_at = transaction.updatedAt,
        )
    }

    override suspend fun deleteTransaction(idempotencyKey: String) {
        database.letaPayDatabaseQueries.deleteTransactionRecord(idempotency_key = idempotencyKey)
    }

    override suspend fun getTransactionByIdempotencyKey(idempotencyKey: String): TransactionEntity? {
        return database.letaPayDatabaseQueries
            .selectTransactionRecordByIdempotencyKey(idempotency_key = idempotencyKey) {
                    key,
                    txHash,
                    assetId,
                    chainId,
                    amount,
                    status,
                    updatedAt,
                ->
                TransactionEntity(
                    idempotencyKey = key,
                    txHash = txHash,
                    assetId = assetId,
                    chainId = chainId,
                    amount = amount,
                    status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.Failed),
                    updatedAt = updatedAt,
                )
            }
            .executeAsOneOrNull()
    }
}

private class SqlDelightChatMessageDao(
    private val database: LetaPayDatabase,
) : ChatMessageDao {
    override fun observeThread(threadId: String): Flow<List<ChatMessageEntity>> {
        return database.letaPayDatabaseQueries
            .selectChatMessagesByThread(thread_id = threadId) {
                    id,
                    dbThreadId,
                    senderWallet,
                    recipientWallet,
                    content,
                    status,
                    createdAt,
                ->
                ChatMessageEntity(
                    id = id,
                    threadId = dbThreadId,
                    senderWallet = senderWallet,
                    recipientWallet = recipientWallet,
                    content = content,
                    status = status.toChatMessageStatus(),
                    createdAt = createdAt,
                )
            }
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun upsertMessages(messages: List<ChatMessageEntity>) {
        messages.forEach { message ->
            database.letaPayDatabaseQueries.upsertChatMessage(
                id = message.id,
                thread_id = message.threadId,
                sender_wallet = message.senderWallet,
                recipient_wallet = message.recipientWallet,
                content = message.content,
                status = message.status.name,
                created_at = message.createdAt,
            )
        }
    }
}

private class SqlDelightPendingMessageDao(
    private val database: LetaPayDatabase,
) : PendingMessageDao {
    override fun observeThread(threadId: String): Flow<List<PendingMessageEntity>> {
        return database.letaPayDatabaseQueries
            .selectPendingMessagesByThread(thread_id = threadId) {
                    id,
                    dbThreadId,
                    senderWallet,
                    recipientWallet,
                    localPayload,
                    status,
                    retryCount,
                    lastError,
                    createdAt,
                ->
                PendingMessageEntity(
                    id = id,
                    threadId = dbThreadId,
                    senderWallet = senderWallet,
                    recipientWallet = recipientWallet,
                    content = localPayload,
                    status = status.toChatMessageStatus(),
                    retryCount = retryCount,
                    lastError = lastError,
                    createdAt = createdAt,
                )
            }
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun upsertMessage(message: PendingMessageEntity) {
        database.letaPayDatabaseQueries.upsertPendingMessage(
            id = message.id,
            thread_id = message.threadId,
            sender_wallet = message.senderWallet,
            recipient_wallet = message.recipientWallet,
            local_payload = message.content,
            status = message.status.name,
            retry_count = message.retryCount,
            last_error = message.lastError,
            created_at = message.createdAt,
        )
    }

    override suspend fun deleteMessage(messageId: String) {
        database.letaPayDatabaseQueries.deletePendingMessage(id = messageId)
    }

    override suspend fun getAllMessages(): List<PendingMessageEntity> {
        return database.letaPayDatabaseQueries
            .selectAllPendingMessages {
                    id,
                    threadId,
                    senderWallet,
                    recipientWallet,
                    localPayload,
                    status,
                    retryCount,
                    lastError,
                    createdAt,
                ->
                PendingMessageEntity(
                    id = id,
                    threadId = threadId,
                    senderWallet = senderWallet,
                    recipientWallet = recipientWallet,
                    content = localPayload,
                    status = status.toChatMessageStatus(),
                    retryCount = retryCount,
                    lastError = lastError,
                    createdAt = createdAt,
                )
            }
            .executeAsList()
    }
}

private class SqlDelightContactDao(
    private val database: LetaPayDatabase,
) : ContactDao {
    override fun observeContacts(ownerWallet: String): Flow<List<ContactEntity>> {
        return database.letaPayDatabaseQueries
            .selectContactsByOwner(owner_wallet = ownerWallet) {
                    dbOwnerWallet,
                    contactWallet,
                    alias,
                    isFavorite,
                    lastInteractionAt,
                    source,
                ->
                ContactEntity(
                    ownerWallet = dbOwnerWallet,
                    contactWallet = contactWallet,
                    alias = alias,
                    isFavorite = isFavorite != 0L,
                    lastInteractionAt = lastInteractionAt,
                    source = source,
                )
            }
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun upsertContacts(ownerWallet: String, contacts: List<ContactEntity>) {
        contacts.forEach { contact ->
            database.letaPayDatabaseQueries.upsertContact(
                owner_wallet = ownerWallet,
                contact_wallet = contact.contactWallet,
                alias = contact.alias,
                is_favorite = if (contact.isFavorite) 1L else 0L,
                last_interaction_at = contact.lastInteractionAt,
                source = contact.source,
            )
        }
    }

    override suspend fun deleteContact(ownerWallet: String, contactWallet: String) {
        database.letaPayDatabaseQueries.deleteContact(
            owner_wallet = ownerWallet,
            contact_wallet = contactWallet,
        )
    }
}

private fun String.toChatMessageStatus(): ChatMessageStatus =
    runCatching { ChatMessageStatus.valueOf(this) }.getOrDefault(ChatMessageStatus.Failed)

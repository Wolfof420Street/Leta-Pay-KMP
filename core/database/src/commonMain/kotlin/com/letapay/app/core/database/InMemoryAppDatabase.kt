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

import com.letapay.app.core.database.dao.ChatMessageDao
import com.letapay.app.core.database.dao.ContactDao
import com.letapay.app.core.database.dao.DeviceTokenDao
import com.letapay.app.core.database.dao.PendingMessageDao
import com.letapay.app.core.database.dao.StakingPositionDao
import com.letapay.app.core.database.dao.TransactionDao
import com.letapay.app.core.database.entity.ChatMessageEntity
import com.letapay.app.core.database.entity.ContactEntity
import com.letapay.app.core.database.entity.DeviceTokenEntity
import com.letapay.app.core.database.entity.PendingMessageEntity
import com.letapay.app.core.database.entity.StakingPositionEntity
import com.letapay.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryAppDatabase : AppDatabase {
    override val transactionDao: TransactionDao = InMemoryTransactionDao()
    override val deviceTokenDao: DeviceTokenDao = InMemoryDeviceTokenDao()
    override val chatMessageDao: ChatMessageDao = InMemoryChatMessageDao()
    override val pendingMessageDao: PendingMessageDao = InMemoryPendingMessageDao()
    override val contactDao: ContactDao = InMemoryContactDao()
    override val stakingPositionDao: StakingPositionDao = InMemoryStakingPositionDao()
}

private class InMemoryDeviceTokenDao : DeviceTokenDao {
    private var deviceToken: DeviceTokenEntity? = null

    override fun getDeviceToken(): DeviceTokenEntity? = deviceToken

    override suspend fun upsertDeviceToken(token: DeviceTokenEntity) {
        deviceToken = token
    }

    override suspend fun clearDeviceToken() {
        deviceToken = null
    }
}

private class InMemoryTransactionDao : TransactionDao {
    private val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactions

    override suspend fun upsertTransaction(transaction: TransactionEntity) {
        transactions.value = transactions.value
            .filterNot { it.idempotencyKey == transaction.idempotencyKey } + transaction
    }

    override suspend fun deleteTransaction(idempotencyKey: String) {
        transactions.value = transactions.value.filterNot { it.idempotencyKey == idempotencyKey }
    }

    override suspend fun getTransactionByIdempotencyKey(idempotencyKey: String): TransactionEntity? = transactions
        .value
        .firstOrNull { it.idempotencyKey == idempotencyKey }
}

private class InMemoryChatMessageDao : ChatMessageDao {
    private val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun observeThread(threadId: String): Flow<List<ChatMessageEntity>> =
        messages.map { entries -> entries.filter { it.threadId == threadId }.sortedBy(ChatMessageEntity::createdAt) }

    override suspend fun upsertMessages(messages: List<ChatMessageEntity>) {
        this.messages.value = (this.messages.value + messages)
            .associateBy(ChatMessageEntity::id)
            .values
            .sortedBy(ChatMessageEntity::createdAt)
    }
}

private class InMemoryPendingMessageDao : PendingMessageDao {
    private val messages = MutableStateFlow<List<PendingMessageEntity>>(emptyList())

    override fun observeThread(threadId: String): Flow<List<PendingMessageEntity>> =
        messages.map { entries -> entries.filter { it.threadId == threadId }.sortedBy(PendingMessageEntity::createdAt) }

    override suspend fun upsertMessage(message: PendingMessageEntity) {
        messages.value = (messages.value.filterNot { it.id == message.id } + message)
            .sortedBy(PendingMessageEntity::createdAt)
    }

    override suspend fun deleteMessage(messageId: String) {
        messages.value = messages.value.filterNot { it.id == messageId }
    }

    override suspend fun getAllMessages(): List<PendingMessageEntity> = messages.value
}

private class InMemoryContactDao : ContactDao {
    private val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())

    override fun observeContacts(ownerWallet: String): Flow<List<ContactEntity>> =
        contacts.map { entries ->
            entries
                .filter { it.ownerWallet == ownerWallet }
                .sortedWith(compareByDescending<ContactEntity> { it.lastInteractionAt }.thenBy { it.contactWallet })
        }

    override suspend fun upsertContacts(ownerWallet: String, contacts: List<ContactEntity>) {
        val preserved = this.contacts.value.filterNot { it.ownerWallet == ownerWallet }
        this.contacts.value = preserved + contacts
    }

    override suspend fun deleteContact(ownerWallet: String, contactWallet: String) {
        contacts.value = contacts.value.filterNot {
            it.ownerWallet == ownerWallet && it.contactWallet == contactWallet
        }
    }
}

private class InMemoryStakingPositionDao : StakingPositionDao {
    private val positions = MutableStateFlow<List<StakingPositionEntity>>(emptyList())

    override fun observePositions(walletAddress: String): Flow<List<StakingPositionEntity>> =
        positions.map { entries ->
            entries
                .filter { it.walletAddress == walletAddress }
                .sortedByDescending(StakingPositionEntity::updatedAt)
        }

    override suspend fun replacePositions(walletAddress: String, positions: List<StakingPositionEntity>) {
        val retained = this.positions.value.filterNot { it.walletAddress == walletAddress }
        this.positions.value = retained + positions
    }
}

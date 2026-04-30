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
import com.letapay.app.core.data.repository.ChatRepository
import com.letapay.app.core.data.repository.ChatState
import com.letapay.app.core.data.repository.FirebaseAuthRepository
import com.letapay.app.core.data.repository.NetworkMonitor
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.database.AppDatabase
import com.letapay.app.core.database.entity.ChatMessageEntity
import com.letapay.app.core.database.entity.ContactEntity
import com.letapay.app.core.database.entity.PendingMessageEntity
import com.letapay.app.core.model.chat.ChatMessage
import com.letapay.app.core.model.chat.ChatMessageStatus
import com.letapay.app.core.model.chat.Contact
import com.letapay.app.core.network.chat.ContactsApi
import com.letapay.app.core.network.chat.FirebaseChatApi
import com.letapay.app.core.network.chat.RemoteChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import template.core.base.common.manager.DispatcherManager
import kotlin.time.Clock

class ChatRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val contactsApi: ContactsApi,
    private val firebaseChatApi: FirebaseChatApi,
    private val sessionRepository: SessionRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val networkMonitor: NetworkMonitor,
    dispatcherManager: DispatcherManager,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherManager.main)
    private val mutableState = MutableStateFlow(ChatState())
    private val activeWallet = sessionRepository.sessionState.map { it.session?.walletAddress?.value }
    private val activeThreadId = MutableStateFlow<String?>(null)
    private var contactsJob: Job? = null
    private var messagesJob: Job? = null

    override val state: StateFlow<ChatState> = mutableState.asStateFlow()

    init {
        activeWallet
            .distinctUntilChanged()
            .onEach(::bindContacts)
            .launchIn(scope)

        combine(
            networkMonitor.isOnline,
            firebaseAuthRepository.authState,
        ) { isOnline, firebaseAuthState ->
            isOnline && firebaseAuthState.isAuthenticated && !firebaseAuthState.idToken.isNullOrBlank()
        }.distinctUntilChanged()
            .onEach { shouldRetry ->
                if (shouldRetry) {
                    retryPendingMessages()
                    refreshMessages()
                }
            }
            .launchIn(scope)
    }

    override suspend fun refreshContacts() {
        val session = sessionRepository.sessionState.value.session ?: return
        mutableState.update { it.copy(isLoading = true, errorMessage = null) }

        runCatching {
            contactsApi.getContacts(session.sessionToken)
        }.onSuccess { contacts ->
            appDatabase.contactDao.upsertContacts(
                ownerWallet = session.walletAddress.value,
                contacts = contacts.map { contact ->
                    ContactEntity(
                        ownerWallet = session.walletAddress.value,
                        contactWallet = contact.walletAddress,
                        alias = contact.alias,
                        isFavorite = contact.isFavorite,
                        lastInteractionAt = contact.lastInteractionAt ?: 0L,
                        source = contact.source,
                    )
                },
            )
            mutableState.update { it.copy(isLoading = false, errorMessage = null) }
        }.onFailure { throwable ->
            mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to refresh contacts.",
                )
            }
        }
    }

    override suspend fun addContact(walletAddress: String, alias: String?) {
        val session = sessionRepository.sessionState.value.session ?: return
        appDatabase.contactDao.upsertContacts(
            ownerWallet = session.walletAddress.value,
            contacts = listOf(
                ContactEntity(
                    ownerWallet = session.walletAddress.value,
                    contactWallet = walletAddress,
                    alias = alias,
                    isFavorite = false,
                    lastInteractionAt = 0L,
                    source = "local",
                ),
            ),
        )
        selectContact(walletAddress)
    }

    override suspend fun removeContact(walletAddress: String) {
        val session = sessionRepository.sessionState.value.session ?: return
        appDatabase.contactDao.deleteContact(session.walletAddress.value, walletAddress)
        if (mutableState.value.activeContact?.walletAddress == walletAddress) {
            selectContact(null)
        }
    }

    override suspend fun selectContact(walletAddress: String?) {
        val activeContact = mutableState.value.contacts.firstOrNull { it.walletAddress == walletAddress }
        mutableState.update { it.copy(activeContact = activeContact, errorMessage = null) }
        activeThreadId.value = activeContact?.let { contact ->
            createThreadId(
                firstWallet = sessionRepository.sessionState.value.session?.walletAddress?.value.orEmpty(),
                secondWallet = contact.walletAddress,
            )
        }
        bindMessages()
        refreshMessages()
    }

    override suspend fun refreshMessages() {
        val context = activeDeliveryContext() ?: return

        val idToken = context.idToken ?: return
        runCatching {
            firebaseChatApi.upsertThreadMember(idToken, context.threadId, context.senderWallet)
            firebaseChatApi.upsertThreadMember(idToken, context.threadId, context.recipientWallet)
            firebaseChatApi.fetchMessages(idToken, context.threadId)
        }.onSuccess { messages ->
            appDatabase.chatMessageDao.upsertMessages(
                messages.map { message ->
                    ChatMessageEntity(
                        id = message.id,
                        threadId = context.threadId,
                        senderWallet = message.sender,
                        recipientWallet = message.recipient,
                        content = message.text,
                        status = ChatMessageStatus.Delivered,
                        createdAt = message.timestamp,
                    )
                },
            )
        }.onFailure { throwable ->
            mutableState.update {
                it.copy(errorMessage = throwable.message ?: "Unable to refresh chat messages.")
            }
        }
    }

    override suspend fun sendMessage(text: String) {
        val context = activeThreadId.value?.let { threadId ->
            sessionRepository.sessionState.value.session?.walletAddress?.value?.let { senderWallet ->
                mutableState.value.activeContact?.walletAddress?.let { recipientWallet ->
                    DeliveryContext(
                        threadId = threadId,
                        senderWallet = senderWallet,
                        recipientWallet = recipientWallet,
                        idToken = null,
                    )
                }
            }
        } ?: return

        val message = PendingMessageEntity(
            id = UUIDGenerator.generateUUID(),
            threadId = context.threadId,
            senderWallet = context.senderWallet,
            recipientWallet = context.recipientWallet,
            content = text.trim(),
            status = ChatMessageStatus.Queued,
            retryCount = 0L,
            lastError = null,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )

        appDatabase.pendingMessageDao.upsertMessage(message)
        mutableState.update { it.copy(isSending = true, errorMessage = null) }
        attemptDelivery(message)
        mutableState.update { it.copy(isSending = false) }
    }

    override suspend fun retryPendingMessages() {
        appDatabase.pendingMessageDao.getAllMessages().forEach { pending ->
            attemptDelivery(pending)
        }
    }

    private suspend fun attemptDelivery(message: PendingMessageEntity) {
        val idToken = firebaseAuthRepository.authState.value.idToken ?: return

        runCatching {
            firebaseChatApi.upsertThreadMember(idToken, message.threadId, message.senderWallet)
            firebaseChatApi.upsertThreadMember(idToken, message.threadId, message.recipientWallet)
            firebaseChatApi.sendMessage(
                idToken = idToken,
                threadId = message.threadId,
                message = RemoteChatMessage(
                    id = message.id,
                    sender = message.senderWallet,
                    recipient = message.recipientWallet,
                    text = message.content,
                    timestamp = message.createdAt,
                ),
            )
        }.onSuccess {
            appDatabase.chatMessageDao.upsertMessages(
                listOf(
                    ChatMessageEntity(
                        id = message.id,
                        threadId = message.threadId,
                        senderWallet = message.senderWallet,
                        recipientWallet = message.recipientWallet,
                        content = message.content,
                        status = ChatMessageStatus.Delivered,
                        createdAt = message.createdAt,
                    ),
                ),
            )
            appDatabase.pendingMessageDao.deleteMessage(message.id)
        }.onFailure { throwable ->
            val nextRetryCount = message.retryCount + 1L
            appDatabase.pendingMessageDao.upsertMessage(
                message.copy(
                    status = if (nextRetryCount >= MAX_RETRY_COUNT) {
                        ChatMessageStatus.Failed
                    } else {
                        ChatMessageStatus.Queued
                    },
                    retryCount = nextRetryCount,
                    lastError = throwable.message,
                ),
            )
            mutableState.update {
                it.copy(errorMessage = throwable.message ?: "Unable to deliver pending message.")
            }
        }
    }

    private fun bindContacts(ownerWallet: String?) {
        contactsJob?.cancel()
        if (ownerWallet.isNullOrBlank()) {
            mutableState.value = ChatState()
            return
        }

        contactsJob = appDatabase.contactDao.observeContacts(ownerWallet)
            .map { contacts -> contacts.map { it.toModel() } }
            .onEach { contacts ->
                mutableState.update { state ->
                    val retainedContact = contacts.firstOrNull {
                        it.walletAddress == state.activeContact?.walletAddress
                    }
                    state.copy(
                        contacts = contacts,
                        activeContact = retainedContact,
                    )
                }
            }
            .launchIn(scope)

        scope.launch {
            refreshContacts()
        }
    }

    private fun bindMessages() {
        messagesJob?.cancel()
        val threadId = activeThreadId.value ?: run {
            mutableState.update { it.copy(messages = emptyList()) }
            return
        }

        messagesJob = combine(
            appDatabase.chatMessageDao.observeThread(threadId),
            appDatabase.pendingMessageDao.observeThread(threadId),
        ) { delivered, pending ->
            val deliveredMessages = delivered.map { it.toModel() }
            val pendingMessages = pending.map { it.toModel() }
            (deliveredMessages + pendingMessages).sortedBy(ChatMessage::createdAtEpochMillis)
        }.onEach { messages ->
            mutableState.update { it.copy(messages = messages) }
        }.launchIn(scope)
    }

    private fun ContactEntity.toModel(): Contact = Contact(
        walletAddress = contactWallet,
        alias = alias,
        isFavorite = isFavorite,
        lastInteractionAtEpochMillis = lastInteractionAt,
        source = source,
    )

    private fun ChatMessageEntity.toModel(): ChatMessage = ChatMessage(
        id = id,
        threadId = threadId,
        senderWallet = senderWallet,
        recipientWallet = recipientWallet,
        text = content,
        createdAtEpochMillis = createdAt,
        status = status,
    )

    private fun PendingMessageEntity.toModel(): ChatMessage = ChatMessage(
        id = id,
        threadId = threadId,
        senderWallet = senderWallet,
        recipientWallet = recipientWallet,
        text = content,
        createdAtEpochMillis = createdAt,
        status = status,
        isPending = true,
        errorMessage = lastError,
    )

    private fun activeDeliveryContext(): DeliveryContext? {
        val threadId = activeThreadId.value
        val senderWallet = sessionRepository.sessionState.value.session?.walletAddress?.value
        val recipientWallet = mutableState.value.activeContact?.walletAddress
        val idToken = firebaseAuthRepository.authState.value.idToken
        val values = listOfNotNull(threadId, senderWallet, recipientWallet, idToken)
        if (values.size != 4) {
            return null
        }
        return DeliveryContext(
            threadId = values[0],
            senderWallet = values[1],
            recipientWallet = values[2],
            idToken = values[3],
        )
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3L

        private data class DeliveryContext(
            val threadId: String,
            val senderWallet: String,
            val recipientWallet: String,
            val idToken: String?,
        )

        fun createThreadId(
            firstWallet: String,
            secondWallet: String,
        ): String = listOf(firstWallet.lowercase(), secondWallet.lowercase()).sorted().joinToString("_")
    }
}

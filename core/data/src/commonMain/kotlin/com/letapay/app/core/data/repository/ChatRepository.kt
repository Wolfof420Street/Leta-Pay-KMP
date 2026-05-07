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

import com.letapay.app.core.model.chat.ChatMessage
import com.letapay.app.core.model.chat.Contact
import kotlinx.coroutines.flow.StateFlow

data class ChatState(
    val contacts: List<Contact> = emptyList(),
    val activeContact: Contact? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

interface ChatRepository {
    val state: StateFlow<ChatState>

    suspend fun refreshContacts()

    suspend fun addContact(
        walletAddress: String,
        alias: String?,
    )

    suspend fun removeContact(walletAddress: String)

    suspend fun selectContact(walletAddress: String?)

    suspend fun refreshMessages()

    suspend fun sendMessage(text: String)

    suspend fun retryPendingMessages()
}

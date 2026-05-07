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

import com.letapay.app.core.data.repository.ChatSummaryRepository
import com.letapay.app.core.data.repository.ChatSummaryState
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.network.chat.ChatSummaryApi
import com.letapay.app.core.network.chat.SseStreamReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatSummaryRepositoryImpl(
    private val sessionRepository: SessionRepository,
    private val chatSummaryApi: ChatSummaryApi,
    private val sseStreamReader: SseStreamReader,
) : ChatSummaryRepository {

    private val mutableSummaryState = MutableStateFlow(ChatSummaryState())

    override val summaryState: StateFlow<ChatSummaryState> = mutableSummaryState.asStateFlow()

    override suspend fun streamSummary(
        event: String,
        transactionResult: String,
    ) {
        val sessionToken = sessionRepository.sessionState.value.session?.sessionToken
        if (sessionToken.isNullOrBlank()) {
            mutableSummaryState.value = ChatSummaryState(
                errorMessage = "Connect a wallet session before requesting a streamed summary.",
            )
            return
        }

        mutableSummaryState.value = ChatSummaryState(isStreaming = true)

        runCatching {
            val statement = chatSummaryApi.prepareSummaryStream(
                sessionToken = sessionToken,
                event = event,
                transactionResult = transactionResult,
            )
            statement.execute { response ->
                sseStreamReader.readDataStream(response) { chunk ->
                    mutableSummaryState.update {
                        it.copy(summaryText = it.summaryText + chunk)
                    }
                }
            }
        }.onSuccess {
            mutableSummaryState.update { it.copy(isStreaming = false, errorMessage = null) }
        }.onFailure { throwable ->
            mutableSummaryState.update {
                it.copy(
                    isStreaming = false,
                    errorMessage = throwable.message ?: "Unable to stream the chat summary.",
                )
            }
        }
    }

    override suspend fun clear() {
        mutableSummaryState.value = ChatSummaryState()
    }
}

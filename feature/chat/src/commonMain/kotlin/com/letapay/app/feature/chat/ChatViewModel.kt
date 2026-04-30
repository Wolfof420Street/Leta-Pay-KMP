/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.ChatRepository
import com.letapay.app.core.data.repository.ChatSummaryRepository
import com.letapay.app.core.data.repository.FirebaseAuthRepository
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.model.chat.ChatMessage
import com.letapay.app.core.model.chat.Contact
import com.letapay.app.feature.agent.AgentOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val walletAddress: String? = null,
    val firebaseBound: Boolean = false,
    val firebaseErrorMessage: String? = null,
    val contacts: List<Contact> = emptyList(),
    val activeContact: Contact? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val newContactWallet: String = "",
    val newContactAlias: String = "",
    val messageDraft: String = "",
    val commandDraft: String = "swap 0.5 ETH to USDC",
    val agentState: String = "Idle",
    val previewSummary: String? = null,
    val clarificationPrompt: String? = null,
    val terminalMessage: String? = null,
    val streamedSummary: String = "",
    val isStreaming: Boolean = false,
    val isLoadingChat: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isCommandActionInFlight: Boolean = false,
    val errorMessage: String? = null,
)

class ChatViewModel(
    private val sessionRepository: SessionRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val chatRepository: ChatRepository,
    private val chatSummaryRepository: ChatSummaryRepository,
    private val agentOrchestrator: AgentOrchestrator,
) : ViewModel() {
    private val draftState = MutableStateFlow(ChatUiState())

    val stateFlow: StateFlow<ChatUiState> = combine(
        draftState,
        sessionRepository.sessionState,
        firebaseAuthRepository.authState,
        chatRepository.state,
        chatSummaryRepository.summaryState,
        agentOrchestrator.state,
        agentOrchestrator.currentPlan,
        agentOrchestrator.clarificationPrompt,
        agentOrchestrator.terminalMessage,
    ) { args ->
        val draft = args[0] as ChatUiState
        val sessionState = args[1] as com.letapay.app.core.data.repository.SessionState
        val firebaseState = args[2] as com.letapay.app.core.data.repository.FirebaseAuthState
        val chatState = args[3] as com.letapay.app.core.data.repository.ChatState
        val summaryState = args[4] as com.letapay.app.core.data.repository.ChatSummaryState
        val agentState = args[5] as com.letapay.app.feature.agent.OrchestratorState
        val currentPlan = args[6] as com.letapay.app.core.ai.ExecutionPlan?
        val clarification = args[7] as String?
        val terminal = args[8] as String?
        draft.copy(
            walletAddress = sessionState.session?.walletAddress?.value,
            firebaseBound = firebaseState.isAuthenticated,
            firebaseErrorMessage = firebaseState.errorMessage,
            contacts = chatState.contacts,
            activeContact = chatState.activeContact,
            chatMessages = chatState.messages,
            agentState = agentState.name,
            previewSummary = currentPlan?.previewSummary,
            clarificationPrompt = clarification,
            terminalMessage = terminal,
            streamedSummary = summaryState.summaryText,
            isStreaming = summaryState.isStreaming,
            isLoadingChat = chatState.isLoading,
            isSendingMessage = chatState.isSending,
            errorMessage = chatState.errorMessage ?: summaryState.errorMessage ?: sessionState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(),
    )

    fun onCommandDraftChanged(value: String) {
        draftState.value = draftState.value.copy(commandDraft = value)
    }

    fun onNewContactWalletChanged(value: String) {
        draftState.value = draftState.value.copy(newContactWallet = value)
    }

    fun onNewContactAliasChanged(value: String) {
        draftState.value = draftState.value.copy(newContactAlias = value)
    }

    fun onMessageDraftChanged(value: String) {
        draftState.value = draftState.value.copy(messageDraft = value)
    }

    fun refreshContacts() {
        viewModelScope.launch {
            chatRepository.refreshContacts()
        }
    }

    fun addContact() {
        val draft = stateFlow.value
        val walletAddress = draft.newContactWallet.trim()
        if (walletAddress.isEmpty()) return

        viewModelScope.launch {
            chatRepository.addContact(
                walletAddress = walletAddress,
                alias = draft.newContactAlias.trim().ifBlank { null },
            )
            draftState.value = draftState.value.copy(
                newContactWallet = "",
                newContactAlias = "",
            )
        }
    }

    fun selectContact(walletAddress: String) {
        viewModelScope.launch {
            chatRepository.selectContact(walletAddress)
        }
    }

    fun removeActiveContact() {
        val walletAddress = stateFlow.value.activeContact?.walletAddress ?: return
        viewModelScope.launch {
            chatRepository.removeContact(walletAddress)
        }
    }

    fun sendMessage() {
        val text = stateFlow.value.messageDraft.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            chatRepository.sendMessage(text)
            draftState.value = draftState.value.copy(messageDraft = "")
        }
    }

    fun retryPendingMessages() {
        viewModelScope.launch {
            chatRepository.retryPendingMessages()
        }
    }

    fun previewCommand() {
        if (stateFlow.value.isCommandActionInFlight) return
        val command = stateFlow.value.commandDraft.trim()
        if (command.isEmpty()) return

        viewModelScope.launch {
            draftState.value = draftState.value.copy(isCommandActionInFlight = true)
            runCatching {
                chatSummaryRepository.clear()
                agentOrchestrator.submitCommand(command)
            }
            draftState.value = draftState.value.copy(isCommandActionInFlight = false)
        }
    }

    fun confirmPreview() {
        if (stateFlow.value.isCommandActionInFlight) return
        val planId = agentOrchestrator.currentPlan.value?.planId ?: return
        viewModelScope.launch {
            draftState.value = draftState.value.copy(isCommandActionInFlight = true)
            runCatching {
                agentOrchestrator.confirmPlan(planId)
                val result = agentOrchestrator.terminalMessage.value
                    ?: agentOrchestrator.currentPlan.value?.previewSummary
                    ?: "No execution result available."
                chatSummaryRepository.streamSummary(
                    event = "PreviewConfirmed",
                    transactionResult = result,
                )
            }
            draftState.value = draftState.value.copy(isCommandActionInFlight = false)
        }
    }
}

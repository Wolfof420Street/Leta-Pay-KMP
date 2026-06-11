/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package com.letapay.app.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.letapay.app.core.model.chat.ChatMessage
import com.letapay.app.core.ui.component.KptPrimaryButton
import com.letapay.app.core.ui.component.KptSecondaryButton
import kmp_project_template.feature.chat.generated.resources.Res
import kmp_project_template.feature.chat.generated.resources.feature_chat_add_contact_alias_label
import kmp_project_template.feature.chat.generated.resources.feature_chat_add_contact_button
import kmp_project_template.feature.chat.generated.resources.feature_chat_add_contact_wallet_label
import kmp_project_template.feature.chat.generated.resources.feature_chat_agent_state_prefix
import kmp_project_template.feature.chat.generated.resources.feature_chat_agentkit_unavailable
import kmp_project_template.feature.chat.generated.resources.feature_chat_command_title
import kmp_project_template.feature.chat.generated.resources.feature_chat_confirm_button
import kmp_project_template.feature.chat.generated.resources.feature_chat_connect_wallet_hint
import kmp_project_template.feature.chat.generated.resources.feature_chat_contacts_connect_hint
import kmp_project_template.feature.chat.generated.resources.feature_chat_contacts_title
import kmp_project_template.feature.chat.generated.resources.feature_chat_firebase_binding_pending
import kmp_project_template.feature.chat.generated.resources.feature_chat_firebase_ready
import kmp_project_template.feature.chat.generated.resources.feature_chat_input_hint
import kmp_project_template.feature.chat.generated.resources.feature_chat_pending_suffix
import kmp_project_template.feature.chat.generated.resources.feature_chat_preview_button
import kmp_project_template.feature.chat.generated.resources.feature_chat_send_button
import kmp_project_template.feature.chat.generated.resources.feature_chat_session_prefix
import kmp_project_template.feature.chat.generated.resources.feature_chat_status_streaming
import kmp_project_template.feature.chat.generated.resources.feature_chat_sync_button
import kmp_project_template.feature.chat.generated.resources.feature_chat_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object ChatRoute

fun NavController.navigateToChat(navOptions: NavOptions? = null) = navigate(ChatRoute, navOptions)

fun NavGraphBuilder.chatDestination() {
    composable<ChatRoute> {
        ChatScreen()
    }
}

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.chatMessages.size, state.streamedSummary) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ChatInputBar(
                value = state.messageDraft,
                onValueChange = viewModel::onMessageDraftChanged,
                onSend = viewModel::sendMessage,
                isProcessing = state.isSendingMessage,
                modifier = Modifier.imePadding(),
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroHeader(
                            walletAddress = state.walletAddress,
                            firebaseBound = state.firebaseBound,
                            firebaseErrorMessage = state.firebaseErrorMessage,
                        )
                        CommandCard(
                            command = state.commandDraft,
                            onCommandChange = viewModel::onCommandDraftChanged,
                            onPreview = viewModel::previewCommand,
                            onConfirm = viewModel::confirmPreview,
                            agentState = state.agentState,
                            previewSummary = state.previewSummary,
                            clarificationPrompt = state.clarificationPrompt,
                            terminalMessage = state.terminalMessage,
                            streamedSummary = state.streamedSummary,
                            isStreaming = state.isStreaming,
                            isActionInFlight = state.isCommandActionInFlight,
                            isAgentKitUnavailable = state.isAgentKitUnavailable,
                        )
                        ContactComposer(
                            newWallet = state.newContactWallet,
                            newAlias = state.newContactAlias,
                            onWalletChange = viewModel::onNewContactWalletChanged,
                            onAliasChange = viewModel::onNewContactAliasChanged,
                            onAddContact = viewModel::addContact,
                            onSyncContacts = viewModel::refreshContacts,
                            walletAddress = state.walletAddress,
                        )
                        state.errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                items(
                    items = state.chatMessages,
                    key = { it.id },
                ) { message ->
                    ChatBubble(
                        message = message,
                        isOutgoing = message.senderWallet == state.walletAddress,
                    )
                }
            }

            if (state.isLoadingChat) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(
    walletAddress: String?,
    firebaseBound: Boolean,
    firebaseErrorMessage: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.feature_chat_title), style = MaterialTheme.typography.displaySmall)
        Text(
            text = walletAddress?.let {
                "${stringResource(Res.string.feature_chat_session_prefix)} $it"
            } ?: stringResource(Res.string.feature_chat_connect_wallet_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (firebaseBound) {
                stringResource(Res.string.feature_chat_firebase_ready)
            } else {
                firebaseErrorMessage ?: stringResource(Res.string.feature_chat_firebase_binding_pending)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (firebaseBound) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun CommandCard(
    command: String,
    onCommandChange: (String) -> Unit,
    onPreview: () -> Unit,
    onConfirm: () -> Unit,
    agentState: String,
    previewSummary: String?,
    clarificationPrompt: String?,
    terminalMessage: String?,
    streamedSummary: String,
    isStreaming: Boolean,
    isActionInFlight: Boolean,
    isAgentKitUnavailable: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.feature_chat_command_title), style = MaterialTheme.typography.titleMedium)
            BasicTextField(
                value = command,
                onValueChange = onCommandChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KptSecondaryButton(
                    onClick = onPreview,
                    enabled = !isActionInFlight && !isAgentKitUnavailable,
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.feature_chat_preview_button),
                )
                KptPrimaryButton(
                    onClick = onConfirm,
                    enabled = previewSummary != null && !isActionInFlight && !isAgentKitUnavailable,
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.feature_chat_confirm_button),
                )
            }
            if (isAgentKitUnavailable) {
                Text(
                    text = stringResource(Res.string.feature_chat_agentkit_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "${stringResource(Res.string.feature_chat_agent_state_prefix)}: $agentState",
                style = MaterialTheme.typography.labelMedium,
            )
            clarificationPrompt?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            previewSummary?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (streamedSummary.isNotBlank()) {
                Text(
                    streamedSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            terminalMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (isStreaming) {
                Text(
                    stringResource(Res.string.feature_chat_status_streaming),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ContactComposer(
    newWallet: String,
    newAlias: String,
    onWalletChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onAddContact: () -> Unit,
    onSyncContacts: () -> Unit,
    walletAddress: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.feature_chat_contacts_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = walletAddress?.let {
                    "${stringResource(Res.string.feature_chat_session_prefix)} $it"
                } ?: stringResource(Res.string.feature_chat_contacts_connect_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = newWallet,
                onValueChange = onWalletChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.feature_chat_add_contact_wallet_label)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = newAlias,
                onValueChange = onAliasChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.feature_chat_add_contact_alias_label)) },
                singleLine = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KptPrimaryButton(
                    onClick = onAddContact,
                    enabled = walletAddress != null,
                    text = stringResource(Res.string.feature_chat_add_contact_button),
                )
                KptSecondaryButton(
                    onClick = onSyncContacts,
                    enabled = walletAddress != null,
                    text = stringResource(Res.string.feature_chat_sync_button),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isOutgoing: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isOutgoing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = buildString {
                        append(message.status.name)
                        if (message.isPending) {
                            append(' ')
                            append(stringResource(Res.string.feature_chat_pending_suffix))
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                message.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !isProcessing,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.feature_chat_input_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
            Button(
                onClick = onSend,
                enabled = value.isNotBlank() && !isProcessing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.feature_chat_send_button))
                }
            }
        }
    }
}

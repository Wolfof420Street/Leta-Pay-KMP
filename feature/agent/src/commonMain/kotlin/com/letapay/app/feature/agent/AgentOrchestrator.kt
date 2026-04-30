/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.agent

import com.letapay.app.core.ai.ConfidenceBand
import com.letapay.app.core.ai.ExecutionPlan
import com.letapay.app.core.ai.ExecutionPreviewPlanner
import com.letapay.app.core.ai.NlpCommandParser
import com.letapay.app.core.ai.confidenceBand
import com.letapay.app.core.common.LoggerProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrchestratorState {
    Idle,
    Parsing,
    Clarifying,
    Planned,
    AwaitingUserConfirm,
    Executing,
    Completed,
    Failed,
}

class AgentOrchestrator(
    private val nlpParser: NlpCommandParser,
    private val previewPlanner: ExecutionPreviewPlanner,
) {
    private val _state = MutableStateFlow(OrchestratorState.Idle)
    val state: StateFlow<OrchestratorState> = _state.asStateFlow()

    private val _currentPlan = MutableStateFlow<ExecutionPlan?>(null)
    val currentPlan: StateFlow<ExecutionPlan?> = _currentPlan.asStateFlow()

    private val _clarificationPrompt = MutableStateFlow<String?>(null)
    val clarificationPrompt: StateFlow<String?> = _clarificationPrompt.asStateFlow()

    private val _terminalMessage = MutableStateFlow<String?>(null)
    val terminalMessage: StateFlow<String?> = _terminalMessage.asStateFlow()

    suspend fun submitCommand(command: String) {
        _state.value = OrchestratorState.Parsing
        _clarificationPrompt.value = null
        _terminalMessage.value = null
        _currentPlan.value = null

        try {
            val result = nlpParser.parseCommand(command)
            LoggerProvider.debug(
                TAG,
                "Parsed command provider=${result.provider} model=${result.model} confidence=${result.confidenceScore}",
            )

            when (result.confidenceBand) {
                ConfidenceBand.Reject -> {
                    _state.value = OrchestratorState.Failed
                    _terminalMessage.value =
                        result.clarificationPrompt
                            ?: "Command was too ambiguous to execute safely."
                }
                ConfidenceBand.Clarify -> {
                    _state.value = OrchestratorState.Clarifying
                    _clarificationPrompt.value = result.clarificationPrompt
                }
                ConfidenceBand.Preview -> {
                    val plan = previewPlanner.createPreviewPlan(result.intent)
                    _currentPlan.value = plan
                    _state.value = OrchestratorState.Planned
                    _state.value = OrchestratorState.AwaitingUserConfirm
                    LoggerProvider.debug(TAG, "Preview plan selected family=${plan.planFamily}")
                }
            }
        } catch (error: Exception) {
            _state.value = OrchestratorState.Failed
            _terminalMessage.value = error.message ?: "Unable to parse command."
            LoggerProvider.error(TAG, error, "Agent orchestration failed")
        }
    }

    fun confirmPlan(planId: String) {
        if (_currentPlan.value?.planId != planId) {
            _state.value = OrchestratorState.Failed
            _terminalMessage.value = "Preview plan expired. Please submit the command again."
            return
        }

        _state.value = OrchestratorState.Executing
        _terminalMessage.value =
            "Preview confirmed. Value-moving execution is intentionally gated " +
            "until build and broadcast phases are complete."
        _state.value = OrchestratorState.Completed
    }

    fun reset() {
        _state.value = OrchestratorState.Idle
        _currentPlan.value = null
        _clarificationPrompt.value = null
        _terminalMessage.value = null
    }

    private companion object {
        const val TAG = "AgentOrchestrator"
    }
}

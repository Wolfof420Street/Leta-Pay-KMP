/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.agent

import com.letapay.app.core.ai.DefaultExecutionPreviewPlanner
import com.letapay.app.core.ai.DefaultNlpCommandParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentOrchestratorTest {
    @Test
    fun submitCommandBuildsPreviewPlanForHighConfidenceSwap() = runTest {
        val orchestrator = AgentOrchestrator(
            nlpParser = DefaultNlpCommandParser(),
            previewPlanner = DefaultExecutionPreviewPlanner(),
        )

        orchestrator.submitCommand("swap 0.5 eth to usdc")

        assertEquals(OrchestratorState.AwaitingUserConfirm, orchestrator.state.value)
        assertNotNull(orchestrator.currentPlan.value)
        assertTrue(orchestrator.currentPlan.value!!.previewSummary.contains("Swap 0.5 ETH to USDC"))
    }

    @Test
    fun submitCommandRequestsClarificationWhenTransferIsIncomplete() = runTest {
        val orchestrator = AgentOrchestrator(
            nlpParser = DefaultNlpCommandParser(),
            previewPlanner = DefaultExecutionPreviewPlanner(),
        )

        orchestrator.submitCommand("send 10 usdc")

        assertEquals(OrchestratorState.Clarifying, orchestrator.state.value)
        assertTrue(orchestrator.clarificationPrompt.value!!.contains("recipient wallet address"))
    }
}

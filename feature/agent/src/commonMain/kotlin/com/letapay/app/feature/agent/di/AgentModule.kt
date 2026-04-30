/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.agent.di

import com.letapay.app.core.ai.DefaultExecutionPreviewPlanner
import com.letapay.app.core.ai.DefaultNlpCommandParser
import com.letapay.app.core.ai.ExecutionPreviewPlanner
import com.letapay.app.core.ai.NlpCommandParser
import com.letapay.app.feature.agent.AgentOrchestrator
import org.koin.dsl.module

val AgentModule = module {
    single<NlpCommandParser> { DefaultNlpCommandParser() }
    single<ExecutionPreviewPlanner> { DefaultExecutionPreviewPlanner() }
    single { AgentOrchestrator(get(), get()) }
}

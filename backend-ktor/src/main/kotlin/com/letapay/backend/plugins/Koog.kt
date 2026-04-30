/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.plugins

import com.letapay.backend.config.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.koin.ktor.ext.get

fun Application.configureKoog() {
    val appConfig = get<AppConfig>()
    val openAiConfigured = !environment.config.propertyOrNull("koog.openai.apiKey")
        ?.getString()
        .isNullOrBlank()

    log.info(
        "Koog fallback config loaded. openAiConfigured={}, parseModel={}, planModel={}, chatModel={}",
        openAiConfigured,
        appConfig.parseModel,
        appConfig.planModel,
        appConfig.chatModel,
    )
}

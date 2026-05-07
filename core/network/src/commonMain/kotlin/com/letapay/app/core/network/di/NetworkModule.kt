/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.di

import com.letapay.app.core.network.ai.AiApi
import com.letapay.app.core.network.auth.AuthApi
import com.letapay.app.core.network.chat.ChatSummaryApi
import com.letapay.app.core.network.chat.ContactsApi
import com.letapay.app.core.network.chat.FirebaseChatApi
import com.letapay.app.core.network.chat.SseStreamReader
import com.letapay.app.core.network.config.AppConfig
import com.letapay.app.core.network.config.AppEnvironment
import com.letapay.app.core.network.config.provideAppConfig
import com.letapay.app.core.network.firebase.FirebaseAuthApi
import com.letapay.app.core.network.portfolio.PortfolioApi
import com.letapay.app.core.network.swap.SwapApi
import com.letapay.app.core.network.transaction.TransactionApi
import com.letapay.app.core.network.yield.YieldApi
import org.koin.dsl.module
import template.core.base.network.httpClient
import template.core.base.network.setupDefaultHttpClient

val NetworkModule = module {
    single<AppConfig> { provideAppConfig(AppEnvironment.Prod) }

    single {
        httpClient(
            setupDefaultHttpClient(
                baseUrl = get<AppConfig>().baseUrl,
            ),
        )
    }
    single { AuthApi(get()) }
    single { FirebaseAuthApi(get(), get()) }
    single { SseStreamReader() }
    single { ChatSummaryApi(get()) }
    single { FirebaseChatApi(get(), get()) }
    single { ContactsApi(get()) }
    single { PortfolioApi(get()) }
    single { TransactionApi(get()) }
    single { SwapApi(get()) }
    single { YieldApi(get()) }
    single { AiApi(get()) }
}

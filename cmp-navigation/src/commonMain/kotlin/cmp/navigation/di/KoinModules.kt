/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.di

import cmp.navigation.AppViewModel
import cmp.navigation.authenticatednavbar.AuthenticatedNavbarNavigationViewModel
import cmp.navigation.rootnav.RootNavViewModel
import com.letapay.app.core.data.di.DataModule
import com.letapay.app.core.database.di.DatabaseModule
import com.letapay.app.core.datastore.di.DatastoreModule
import com.letapay.app.core.network.di.NetworkModule
import com.letapay.app.feature.agent.di.AgentModule
import com.letapay.app.feature.auth.di.AuthModule
import com.letapay.app.feature.chat.ChatViewModel
import com.letapay.app.feature.wallet.WalletViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import template.core.base.analytics.di.analyticsModule
import template.core.base.common.di.CommonModule
import template.core.base.platform.di.platformModule

object KoinModules {
    private val dataModule = module {
        includes(DataModule)
    }

    private val dispatcherModule = module {
        includes(CommonModule)
    }

    private val AppModule = module {
        includes(platformModule)

        viewModelOf(::AppViewModel)
        viewModelOf(::AuthenticatedNavbarNavigationViewModel)
        viewModelOf(::RootNavViewModel)
        viewModelOf(::ChatViewModel)
        viewModelOf(::WalletViewModel)
    }

    private val phase1Infrastructure = module {
        includes(NetworkModule, DatabaseModule)
    }

    private val featureModule = module {
        includes(
            AgentModule,
            AuthModule,
        )
    }

    val allModules = listOf(
        dataModule,
        dispatcherModule,
        phase1Infrastructure,
        analyticsModule,
        DatastoreModule,
        featureModule,
        AppModule,
    )
}

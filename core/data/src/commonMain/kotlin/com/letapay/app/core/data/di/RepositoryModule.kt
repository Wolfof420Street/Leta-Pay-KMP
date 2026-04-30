/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.di

import com.letapay.app.core.data.repository.ChatRepository
import com.letapay.app.core.data.repository.ChatSummaryRepository
import com.letapay.app.core.data.repository.FirebaseAuthRepository
import com.letapay.app.core.data.repository.NetworkMonitor
import com.letapay.app.core.data.repository.PortfolioRepository
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.data.repository.SwapRepository
import com.letapay.app.core.data.repository.TransactionRepository
import com.letapay.app.core.data.repository.UserDataRepository
import com.letapay.app.core.data.repository.UserLogoutManager
import com.letapay.app.core.data.repository.YieldRepository
import com.letapay.app.core.data.repositoryImpl.ChatRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.ChatSummaryRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.FirebaseAuthRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.NetworkMonitorImpl
import com.letapay.app.core.data.repositoryImpl.PortfolioRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.SessionRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.SwapRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.TransactionRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.UserDataRepositoryImpl
import com.letapay.app.core.data.repositoryImpl.UserLogoutManagerImpl
import com.letapay.app.core.data.repositoryImpl.YieldRepositoryImpl
import com.letapay.app.core.datastore.di.DatastoreModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import template.core.base.common.di.CommonModule

val DataModule = module {
    includes(platformModule, CommonModule, DatastoreModule)

    singleOf(::NetworkMonitorImpl) bind NetworkMonitor::class
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::FirebaseAuthRepositoryImpl) bind FirebaseAuthRepository::class
    singleOf(::ChatSummaryRepositoryImpl) bind ChatSummaryRepository::class
    singleOf(::SessionRepositoryImpl) bind SessionRepository::class
    singleOf(::PortfolioRepositoryImpl) bind PortfolioRepository::class
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class
    singleOf(::UserLogoutManagerImpl) bind UserLogoutManager::class
    singleOf(::TransactionRepositoryImpl) bind TransactionRepository::class
    singleOf(::SwapRepositoryImpl) bind SwapRepository::class
    singleOf(::YieldRepositoryImpl) bind YieldRepository::class
}

expect val platformModule: Module

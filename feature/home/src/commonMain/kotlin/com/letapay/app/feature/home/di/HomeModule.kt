/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.home.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.letapay.app.feature.home.service.StorageService
import com.letapay.app.feature.home.service.StorageServiceImpl
import com.letapay.app.feature.home.task.EditTaskViewModel
import com.letapay.app.feature.home.tasks.TasksViewModel

val HomeModule = module {
    single<StorageService> { StorageServiceImpl() }
    viewModelOf(::TasksViewModel)
    viewModelOf(::EditTaskViewModel)
}

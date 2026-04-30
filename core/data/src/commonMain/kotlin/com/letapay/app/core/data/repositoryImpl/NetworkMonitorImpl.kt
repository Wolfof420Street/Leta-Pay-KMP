/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repositoryImpl

import com.letapay.app.core.data.repository.NetworkMonitor
import com.letapay.app.core.data.util.connectivityProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkMonitorImpl : NetworkMonitor {
    override val isOnline: Flow<Boolean>
        get() = connectivityProvider.statusUpdates.map { it.isConnected }
}

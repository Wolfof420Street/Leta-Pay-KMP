/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.database.entity

import com.letapay.app.core.common.DevicePlatform

data class DeviceTokenEntity(
    val token: String,
    val platform: DevicePlatform,
    val registeredAt: Long,
)

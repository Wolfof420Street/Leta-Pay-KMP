/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.model.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    val fcmToken: String,
    val platform: String,
)

@Serializable
data class DeviceTokenDeleteRequest(
    val fcmToken: String,
)

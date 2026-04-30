/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.common

enum class DevicePlatform {
    Android,
    Ios,
    Desktop,
    Web,
}

interface PushNotificationManager {
    val isSupported: Boolean

    suspend fun registerDeviceToken(
        platform: DevicePlatform,
        deviceToken: String,
    )

    suspend fun getRegisteredDeviceToken(): RegisteredDeviceToken?

    suspend fun clearDeviceToken()
}

data class RegisteredDeviceToken(
    val platform: DevicePlatform,
    val token: String,
    val registeredAtEpochMillis: Long,
)

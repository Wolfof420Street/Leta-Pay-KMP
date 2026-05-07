/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.datastore.security

import com.russhwolf.settings.Settings

actual object SecureStorage {
    private val settings = Settings()

    actual suspend fun putString(
        key: String,
        value: String,
    ) {
        settings.putString(key, value)
    }

    actual suspend fun getString(key: String): String? = settings.getStringOrNull(key)

    actual suspend fun remove(key: String) {
        settings.remove(key)
    }

    actual suspend fun clear() {
        settings.clear()
    }
}

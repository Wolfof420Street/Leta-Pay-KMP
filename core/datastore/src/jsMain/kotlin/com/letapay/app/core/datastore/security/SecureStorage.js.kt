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

private val inMemoryStorage = linkedMapOf<String, String>()

actual object SecureStorage {
    actual suspend fun putString(
        key: String,
        value: String,
    ) {
        inMemoryStorage[key] = value
    }

    actual suspend fun getString(key: String): String? = inMemoryStorage[key]

    actual suspend fun remove(key: String) {
        inMemoryStorage.remove(key)
    }

    actual suspend fun clear() {
        inMemoryStorage.clear()
    }
}

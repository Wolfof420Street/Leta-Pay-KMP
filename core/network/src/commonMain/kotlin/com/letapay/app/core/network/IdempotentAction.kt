/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network

import com.letapay.app.core.common.UUIDGenerator
import kotlinx.coroutines.flow.MutableStateFlow

class IdempotentAction<T>(
    private val keyStore: MutableStateFlow<String?> = MutableStateFlow(null),
) {
    fun getOrCreateKey(): String {
        return keyStore.value ?: UUIDGenerator.generateUUID().also { keyStore.value = it }
    }
    fun clear() {
        keyStore.value = null
    }
}

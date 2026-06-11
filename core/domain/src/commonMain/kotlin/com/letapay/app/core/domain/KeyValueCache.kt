/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.domain

data class KeyValueEntry<V : Any>(
    val value: V,
    val expiresAt: Long,
)

interface KeyValueCache<V : Any> {
    suspend fun get(key: String): V?
    suspend fun getEntry(key: String): KeyValueEntry<V>?
    suspend fun put(key: String, value: V, ttlSeconds: Long)
    suspend fun invalidate(key: String)
}

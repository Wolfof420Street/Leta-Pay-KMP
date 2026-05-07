/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.security

import java.util.concurrent.ConcurrentHashMap

object SessionBootstrapRegistry {
    private val pending = ConcurrentHashMap<String, String>()

    fun register(sessionId: String, walletAddress: String) {
        pending[sessionId] = walletAddress
    }

    fun consume(sessionId: String, walletAddress: String): Boolean {
        val expected = pending[sessionId]
        return if (expected != null && expected.equals(walletAddress, ignoreCase = true)) {
            pending.remove(sessionId, expected)
        } else {
            false
        }
    }
}

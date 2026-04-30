/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.network.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppConfigTest {
    @Test
    fun resolvesDevelopmentConfigByDefault() {
        val config = provideAppConfig()

        assertEquals("http://localhost:3000", config.baseUrl)
        assertTrue(config.featureFlags.walletConnectEnabled)
    }

    @Test
    fun resolvesStagingConfigWhenRequested() {
        val config = provideAppConfig(AppEnvironment.Staging)

        assertEquals("https://staging-api.letapay.com", config.baseUrl)
        assertEquals(3, config.supportedChains.size)
    }
}

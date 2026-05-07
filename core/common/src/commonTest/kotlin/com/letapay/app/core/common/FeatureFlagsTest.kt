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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {
    @Test
    fun defaultPhaseOneFlagsMatchPhaseOneGates() {
        val flags = FeatureFlags()

        assertFalse(flags.walletConnectEnabled)
        assertFalse(flags.chatEnabled)
        assertFalse(flags.sendPaymentEnabled)
        assertFalse(flags.swapsEnabled)
        assertFalse(flags.stakingEnabled)
        assertFalse(flags.voiceInputEnabled)
        assertFalse(flags.baseStakingEnabled)
        assertFalse(flags.ensResolutionEnabled)
        assertTrue(flags.usdValueEnabled)
    }
}

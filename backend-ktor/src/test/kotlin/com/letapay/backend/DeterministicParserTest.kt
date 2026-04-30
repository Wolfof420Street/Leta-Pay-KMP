/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend

import com.letapay.backend.ai.deterministicParse
import com.letapay.backend.model.ai.IntentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeterministicParserTest {
    @Test
    fun `send eth command parses deterministically`() {
        val result = deterministicParse("send 0.5 ETH to 0x742d35Cc6634C0532925a3b844Bc454e4438f44e")
        assertNotNull(result)
        assertEquals(IntentType.SendPayment, result.intent)
    }

    @Test
    fun `send usdc ens command parses deterministically`() {
        val result = deterministicParse("send 25 USDC to vitalik.eth")
        assertNotNull(result)
        assertEquals(IntentType.SendPayment, result.intent)
    }

    @Test
    fun `swap command parses deterministically`() {
        val result = deterministicParse("swap 1 ETH for USDC on Base")
        assertNotNull(result)
        assertEquals(IntentType.SwapAsset, result.intent)
    }

    @Test
    fun `balance command parses deterministically`() {
        val result = deterministicParse("check my balance")
        assertNotNull(result)
        assertEquals(IntentType.CheckBalance, result.intent)
    }

    @Test
    fun `history command parses deterministically`() {
        val result = deterministicParse("show transaction history")
        assertNotNull(result)
        assertEquals(IntentType.ShowHistory, result.intent)
    }
}

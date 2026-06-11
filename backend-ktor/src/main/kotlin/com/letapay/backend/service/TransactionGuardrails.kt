/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.letapay.backend.config.AppConfig
import com.letapay.backend.error.TransactionValueExceededError
import java.math.BigDecimal

fun enforceTransactionLimit(amount: String, appConfig: AppConfig) {
    val max = appConfig.maxTransactionValueEth ?: return
    val parsed = amount.toBigDecimalOrNull()
        ?: throw TransactionValueExceededError(max.toPlainString())
    if (parsed < BigDecimal.ZERO || parsed > max) {
        throw TransactionValueExceededError(max.toPlainString())
    }
}

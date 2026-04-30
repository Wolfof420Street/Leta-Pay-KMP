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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ConfirmationWatcher(
    private val coinbaseService: CoinbaseService,
    private val pendingNotificationService: PendingNotificationService,
    private val pushNotificationService: PushNotificationService,
) {
    suspend fun watch() = coroutineScope {
        while (isActive) {
            try {
                val pending = pendingNotificationService.watching()
                pending.forEach { row ->
                    launch {
                        try {
                            val status = coinbaseService.getTxStatus(row.txHash, row.chain)
                            when {
                                status.confirmed -> markConfirmedAndNotify(row, status)
                                status.failed -> markFailedAndNotify(row, status)
                                row.watchUntil <= System.currentTimeMillis() -> markExpired(row)
                            }
                        } catch (exception: Exception) {
                            logger.warn("Failed to check tx {}", row.txHash, exception)
                        }
                    }
                }
            } catch (exception: Exception) {
                logger.warn("ConfirmationWatcher poll failed", exception)
            }
            delay(15_000L)
        }
    }

    private suspend fun markConfirmedAndNotify(row: PendingNotificationRecord, status: TxStatus) {
        // Fix: move the DB row first so confirmed notifications are not re-sent on the next poll cycle.
        pendingNotificationService.updateStatus(row.id, "CONFIRMED")
        pushNotificationService.notifyWallet(
            row.walletAddress,
            NotificationPayloadFactory.txConfirmed(
                amount = row.amount,
                asset = row.asset,
                txHash = row.txHash,
                chain = row.chain,
                networkName = status.networkName,
            ),
        )
    }

    private suspend fun markFailedAndNotify(row: PendingNotificationRecord, status: TxStatus) {
        pendingNotificationService.updateStatus(row.id, "FAILED")
        pushNotificationService.notifyWallet(
            row.walletAddress,
            NotificationPayloadFactory.txFailed(row.txHash, status.networkName),
        )
    }

    private suspend fun markExpired(row: PendingNotificationRecord) {
        pendingNotificationService.updateStatus(row.id, "EXPIRED")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConfirmationWatcher::class.java)
    }
}

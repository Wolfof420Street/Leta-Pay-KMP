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

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification

data class NotificationPayload(
    val title: String,
    val body: String,
    val data: Map<String, String>,
)

data class SendResult(
    val token: String,
    val success: Boolean,
    val errorCode: String? = null,
)

interface PushMessagingClient {
    suspend fun sendSingle(token: String, payload: NotificationPayload): SendResult

    suspend fun sendMany(tokens: List<String>, payload: NotificationPayload): List<SendResult>
}

class FirebaseMessagingClient(
    private val firebaseMessaging: FirebaseMessaging,
) : PushMessagingClient {
    override suspend fun sendSingle(token: String, payload: NotificationPayload): SendResult =
        try {
            firebaseMessaging.send(
                Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
                    .putAllData(payload.data)
                    .build(),
            )
            SendResult(token = token, success = true)
        } catch (exception: FirebaseMessagingException) {
            SendResult(token = token, success = false, errorCode = exception.errorCode.name)
        }

    override suspend fun sendMany(tokens: List<String>, payload: NotificationPayload): List<SendResult> {
        val response = firebaseMessaging.sendEachForMulticast(
            MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
                .putAllData(payload.data)
                .build(),
        )
        return response.responses.mapIndexed { index, result ->
            SendResult(
                token = tokens[index],
                success = result.isSuccessful,
                errorCode = result.exception?.errorCode?.name,
            )
        }
    }
}

class FakePushMessagingClient : PushMessagingClient {
    val sent = mutableListOf<Pair<List<String>, NotificationPayload>>()

    override suspend fun sendSingle(token: String, payload: NotificationPayload): SendResult {
        sent += listOf(token) to payload
        return SendResult(token = token, success = true)
    }

    override suspend fun sendMany(tokens: List<String>, payload: NotificationPayload): List<SendResult> {
        sent += tokens to payload
        return tokens.map { SendResult(token = it, success = true) }
    }
}

class PushNotificationService(
    private val deviceTokenService: DeviceTokenService,
    private val client: PushMessagingClient,
) {
    suspend fun notifyWallet(walletAddress: String, payload: NotificationPayload) {
        val activeTokens = deviceTokenService.activeTokens(walletAddress).map(DeviceTokenRecord::fcmToken)
        if (activeTokens.isEmpty()) return

        val results = if (activeTokens.size == 1) {
            listOf(client.sendSingle(activeTokens.single(), payload))
        } else {
            client.sendMany(activeTokens, payload)
        }

        // Fix: invalid device registrations are deactivated individually so one bad token does not fail the batch.
        results.filter { !it.success && it.errorCode in INVALID_FCM_CODES }
            .forEach { deviceTokenService.deactivateToken(it.token) }
    }

    companion object {
        private val INVALID_FCM_CODES = setOf("UNREGISTERED", "INVALID_ARGUMENT")
    }
}

object NotificationPayloadFactory {
    fun incomingPayment(amount: String, asset: String, txHash: String, chain: Long) =
        NotificationPayload(
            title = "Incoming payment received",
            body = "Received $amount $asset",
            data = mapOf(
                "type" to "INCOMING_PAYMENT",
                "txHash" to txHash,
                "chain" to chain.toString(),
                "deepLink" to "letapay://transactions/$txHash",
            ),
        )

    fun txConfirmed(amount: String, asset: String, txHash: String, chain: Long, networkName: String) =
        NotificationPayload(
            title = "Transaction confirmed",
            body = "Sent $amount $asset on $networkName",
            data = mapOf(
                "type" to "TX_CONFIRMED",
                "txHash" to txHash,
                "chain" to chain.toString(),
                "deepLink" to "letapay://transactions/$txHash",
            ),
        )

    fun txFailed(txHash: String, networkName: String) =
        NotificationPayload(
            title = "Transaction failed",
            body = "Your transaction on $networkName did not go through",
            data = mapOf("type" to "TX_FAILED", "txHash" to txHash, "deepLink" to "letapay://transactions/$txHash"),
        )

    fun swapConfirmed(txHash: String, fromAmount: String, fromAsset: String, toAmount: String, toAsset: String) =
        NotificationPayload(
            title = "Swap complete",
            body = "Swapped $fromAmount $fromAsset -> $toAmount $toAsset",
            data = mapOf("type" to "SWAP_CONFIRMED", "txHash" to txHash, "deepLink" to "letapay://swaps/$txHash"),
        )

    fun stakeConfirmed(positionId: String, amount: String, asset: String, provider: String) =
        NotificationPayload(
            title = "Staking confirmed",
            body = "Staked $amount $asset with $provider",
            data = mapOf(
                "type" to "STAKE_CONFIRMED",
                "positionId" to positionId,
                "deepLink" to "letapay://yield/positions/$positionId",
            ),
        )

    fun yieldRewardAvailable(positionId: String, rewardsAmount: String, rewardAsset: String) =
        NotificationPayload(
            title = "Yield rewards available",
            body = "You have $rewardsAmount $rewardAsset available",
            data = mapOf(
                "type" to "YIELD_REWARD_AVAILABLE",
                "positionId" to positionId,
                "deepLink" to "letapay://yield/positions/$positionId",
            ),
        )
}

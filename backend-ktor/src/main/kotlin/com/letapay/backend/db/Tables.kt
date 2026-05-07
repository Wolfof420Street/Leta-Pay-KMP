/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.db

import org.jetbrains.exposed.sql.Table

object Sessions : Table("sessions") {
    val id = varchar("id", 36)
    val walletAddress = varchar("wallet_address", 42).index()
    val refreshTokenSelector = varchar("refresh_token_selector", 36).uniqueIndex()
    val refreshTokenHash = text("refresh_token_hash")
    val familyId = varchar("family_id", 36).index()
    val deviceFingerprint = text("device_fingerprint").nullable()
    val expiresAt = long("expires_at")
    val revokedAt = long("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Nonces : Table("nonces") {
    val nonce = varchar("nonce", 36).uniqueIndex()
    val walletAddress = varchar("wallet_address", 42)
    val expiresAt = long("expires_at").index()
    val usedAt = long("used_at").nullable()

    override val primaryKey = PrimaryKey(nonce)
}

object IdempotencyKeys : Table("idempotency_keys") {
    val key = varchar("key", 36)
    val walletAddress = varchar("wallet_address", 42)
    val endpoint = varchar("endpoint", 200)
    val requestHash = varchar("request_hash", 64).nullable()
    val responseSnapshot = text("response_snapshot").nullable()
    val responseStatus = integer("response_status").nullable()
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")

    override val primaryKey = PrimaryKey(key, walletAddress, endpoint)
}

object Transactions : Table("transactions") {
    val txHash = varchar("tx_hash", 66)
    val walletAddress = varchar("wallet_address", 42).index()
    val toAddress = varchar("to_address", 42)
    val status = varchar("status", 40)
    val signedTx = text("signed_tx")
    val notificationType = varchar("notification_type", 30).default("TX_CONFIRMED")
    val asset = varchar("asset", 20).default("ETH")
    val amount = varchar("amount", 80).default("0")
    val chainId = long("chain_id").default(1)
    val createdAt = long("created_at").index()

    override val primaryKey = PrimaryKey(txHash)
}

object SwapQuotes : Table("swap_quotes") {
    val quoteId = varchar("quote_id", 64)
    val walletAddress = varchar("wallet_address", 42).index()
    val fromAsset = varchar("from_asset", 20)
    val toAsset = varchar("to_asset", 20)
    val fromAmount = varchar("from_amount", 80)
    val toAmount = varchar("to_amount", 80)
    val rate = varchar("rate", 80)
    val priceImpactBps = integer("price_impact_bps")
    val estimatedFeeUsd = varchar("estimated_fee_usd", 40)
    val expiresAt = long("expires_at").index()
    val calldata = text("calldata")
    val chainId = long("chain_id")

    override val primaryKey = PrimaryKey(quoteId)
}

object StakingPositions : Table("staking_positions") {
    val positionId = varchar("position_id", 64)
    val opportunityId = varchar("opportunity_id", 64).index()
    val walletAddress = varchar("wallet_address", 42).index()
    val stakedAmount = varchar("staked_amount", 80)
    val currentValue = varchar("current_value", 80)
    val currentValueUsd = varchar("current_value_usd", 80)
    val accruedRewards = varchar("accrued_rewards", 80)
    val status = varchar("status", 40)
    val chainId = long("chain_id")
    val createdAt = long("created_at").index()

    override val primaryKey = PrimaryKey(positionId)
}

object DeviceTokens : Table("device_tokens") {
    val id = varchar("id", 36)
    val walletAddress = varchar("wallet_address", 42).index()
    val fcmToken = text("fcm_token")
    val platform = varchar("platform", 10)
    val updatedAt = long("updated_at")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)
}

object PendingNotifications : Table("pending_notifications") {
    val id = varchar("id", 36)
    val walletAddress = varchar("wallet_address", 42).index()
    val txHash = varchar("tx_hash", 66).index()
    val chain = long("chain")
    val asset = varchar("asset", 20)
    val amount = varchar("amount", 80)
    val notificationType = varchar("notification_type", 30)
    val status = varchar("status", 20).default("WATCHING")
    val watchUntil = long("watch_until")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

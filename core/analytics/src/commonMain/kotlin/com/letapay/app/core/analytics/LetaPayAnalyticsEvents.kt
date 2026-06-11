/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.analytics

/**
 * Leta Pay specific analytics event types
 */
object LetaPayEventTypes {
    // Auth & Onboarding
    const val WALLET_CONNECT_REQUESTED = "wallet_connect_requested"
    const val WALLET_CONNECTED = "wallet_connected"
    const val NONCE_REQUESTED = "nonce_requested"
    const val SIWE_VERIFIED = "siwe_verified"
    const val AUTH_FAILED = "auth_failed"

    // AI & Chat
    const val AI_COMMAND_SUBMITTED = "ai_command_submitted"
    const val AI_INTENT_PARSED = "ai_intent_parsed"
    const val AI_PLAN_CONFIRMED = "ai_plan_confirmed"
    const val AI_RESPONSE_STREAMED = "ai_response_streamed"
    const val CHAT_MESSAGE_SENT = "chat_message_sent"
    const val CONTACT_ADDED = "contact_added"

    // DeFi Operations
    const val SWAP_QUOTE_REQUESTED = "swap_quote_requested"
    const val SWAP_EXECUTED = "swap_executed"
    const val STAKE_INITIATED = "stake_initiated"
    const val UNSTAKE_INITIATED = "unstake_initiated"
    const val GAS_ESTIMATED = "gas_estimated"
    const val TRANSACTION_SENT = "transaction_sent"
    const val TRANSACTION_CONFIRMED = "transaction_confirmed"
    const val TRANSACTION_FAILED = "transaction_failed"

    // Wallet & Balance
    const val BALANCE_REFRESHED = "balance_refreshed"
    const val PRICE_FETCHED = "price_fetched"
}

/**
 * Leta Pay specific parameter keys
 */
object LetaPayParamKeys {
    const val WALLET_ADDRESS = "wallet_address"
    const val CHAIN_ID = "chain_id"
    const val SIGNATURE = "signature"
    const val ERROR_MESSAGE = "error_message"

    const val COMMAND_TEXT = "command_text"
    const val INTENT_TYPE = "intent_type"
    const val PLAN_ID = "plan_id"

    const val FROM_TOKEN = "from_token"
    const val TO_TOKEN = "to_token"
    const val FROM_AMOUNT = "from_amount"
    const val TO_AMOUNT = "to_amount"
    const val TRANSACTION_HASH = "tx_hash"
    const val ESTIMATED_FEE_USD = "estimated_fee_usd"

    const val ASSET_SYMBOL = "asset_symbol"
    const val ACTION_TYPE = "action_type"
    const val STATUS = "status"
}

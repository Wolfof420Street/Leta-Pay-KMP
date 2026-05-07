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

import com.letapay.app.core.common.FeatureFlags
import com.letapay.app.core.model.blockchain.ChainId

enum class AppEnvironment {
    Dev,
    Staging,
    Prod,
}

interface AppConfig {
    val baseUrl: String
    val realtimeDatabaseUrl: String
    val firebaseProjectId: String
    val firebaseApiKey: String
    val walletConnectProjectId: String
    val supportedChains: Set<ChainId>
    val featureFlags: FeatureFlags
    val isDebug: Boolean
}

object DevConfig : AppConfig {
    override val baseUrl: String = "http://localhost:3000"
    override val realtimeDatabaseUrl: String = "https://dev-letapay-chat.firebaseio.com"
    override val firebaseProjectId: String = "mifos-mobile-apps"
    override val firebaseApiKey: String = "AIzaSyCUz3P8uUExMFcPHa1Ga3DBKhjK5zxNn70"
    override val walletConnectProjectId: String = "dev-wallet-connect-project"
    override val supportedChains: Set<ChainId> = setOf(ChainId.Ethereum, ChainId.Polygon, ChainId.Base)
    override val featureFlags: FeatureFlags = FeatureFlags(
        yieldEnabled = true,
        swapEnabled = true,
        voiceInputEnabled = true,
        baseStakingEnabled = true,
        ensResolutionEnabled = true,
        usdValueEnabled = true,
        walletConnectEnabled = true,
        chatEnabled = true,
        sendPaymentEnabled = true,
        historyEnabled = true,
        pushNotificationsEnabled = true,
    )
    override val isDebug: Boolean = true
}

object ProdConfig : AppConfig {
    override val baseUrl: String = "https://api.letapay.com"
    override val realtimeDatabaseUrl: String = "https://letapay-chat.firebaseio.com"
    override val firebaseProjectId: String = "mifos-mobile-apps"
    override val firebaseApiKey: String = "AIzaSyCUz3P8uUExMFcPHa1Ga3DBKhjK5zxNn70"
    override val walletConnectProjectId: String = "prod-wallet-connect-project"
    override val supportedChains: Set<ChainId> = setOf(ChainId.Ethereum, ChainId.Polygon, ChainId.Base)
    override val featureFlags: FeatureFlags = FeatureFlags(
        yieldEnabled = false,
        swapEnabled = false,
        voiceInputEnabled = false,
        baseStakingEnabled = false,
        ensResolutionEnabled = false,
        usdValueEnabled = true,
        walletConnectEnabled = true,
        chatEnabled = true,
        sendPaymentEnabled = false,
        historyEnabled = true,
        pushNotificationsEnabled = true,
    )
    override val isDebug: Boolean = false
}

object StagingConfig : AppConfig {
    override val baseUrl: String = "https://staging-api.letapay.com"
    override val realtimeDatabaseUrl: String = "https://staging-letapay-chat.firebaseio.com"
    override val firebaseProjectId: String = "mifos-mobile-apps"
    override val firebaseApiKey: String = "AIzaSyCUz3P8uUExMFcPHa1Ga3DBKhjK5zxNn70"
    override val walletConnectProjectId: String = "staging-wallet-connect-project"
    override val supportedChains: Set<ChainId> = setOf(ChainId.Ethereum, ChainId.Polygon, ChainId.Base)
    override val featureFlags: FeatureFlags = FeatureFlags(
        yieldEnabled = true,
        swapEnabled = true,
        voiceInputEnabled = false,
        baseStakingEnabled = true,
        ensResolutionEnabled = true,
        usdValueEnabled = true,
        walletConnectEnabled = true,
        chatEnabled = true,
        sendPaymentEnabled = true,
        historyEnabled = true,
        pushNotificationsEnabled = true,
    )
    override val isDebug: Boolean = true
}

fun provideAppConfig(environment: AppEnvironment = AppEnvironment.Dev): AppConfig = when (environment) {
    AppEnvironment.Dev -> DevConfig
    AppEnvironment.Staging -> StagingConfig
    AppEnvironment.Prod -> ProdConfig
}

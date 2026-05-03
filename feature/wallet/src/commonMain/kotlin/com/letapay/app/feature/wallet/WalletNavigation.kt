/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("MatchingDeclarationName")

package com.letapay.app.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kmp_project_template.feature.wallet.generated.resources.Res
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_amount_prefix
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_balance_empty
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_chain_prefix
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_disconnect_button
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_loading_balances
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_no_session
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_refresh_button
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_refreshing_button
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_title
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_total_prefix
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_usd_prefix
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object WalletRoute

fun NavController.navigateToWallet(navOptions: NavOptions? = null) = navigate(WalletRoute, navOptions)

fun NavGraphBuilder.walletDestination() {
    composable<WalletRoute> {
        WalletScreen()
    }
}

@Composable
fun WalletRouteScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.feature_wallet_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = state.walletAddress ?: stringResource(Res.string.feature_wallet_no_session),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${stringResource(Res.string.feature_wallet_total_prefix)}: ${state.totalUsdValue ?: "--"}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = viewModel::refreshBalance,
                        enabled = !state.isLoading,
                    ) {
                        Text(
                            if (state.isLoading) {
                                stringResource(Res.string.feature_wallet_refreshing_button)
                            } else {
                                stringResource(Res.string.feature_wallet_refresh_button)
                            },
                        )
                    }
                    Button(
                        onClick = viewModel::logout,
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(Res.string.feature_wallet_disconnect_button))
                    }
                }
            }
        }

        state.errorMessage?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.balances.isEmpty()) {
            item {
                Text(
                    text = if (state.isLoading) {
                        stringResource(Res.string.feature_wallet_loading_balances)
                    } else {
                        stringResource(Res.string.feature_wallet_balance_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(state.balances, key = { "${it.chainId.value}-${it.assetId.value}" }) { balance ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(balance.symbol, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${stringResource(Res.string.feature_wallet_amount_prefix)}: ${balance.amount}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${stringResource(Res.string.feature_wallet_chain_prefix)}: ${balance.chainId.value}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        balance.usdValue?.let {
                            Text(
                                "${stringResource(Res.string.feature_wallet_usd_prefix)}: $it",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

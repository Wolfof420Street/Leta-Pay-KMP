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

package com.letapay.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.letapay.app.core.model.blockchain.WalletAddress
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object AuthRoute

fun NavController.navigateToAuth(navOptions: NavOptions? = null) = navigate(AuthRoute, navOptions)

fun NavGraphBuilder.authDestination() {
    composable<AuthRoute> {
        AuthScreen()
    }
}

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Leta Pay",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Connect your wallet, sign once, and keep moving.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.walletAddress,
                    onValueChange = viewModel::onWalletAddressChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Wallet address") },
                    singleLine = true,
                )

                Button(
                    onClick = {
                        runCatching { WalletAddress(state.walletAddress.trim()) }
                            .onSuccess(viewModel::requestNonce)
                            .onFailure {
                                viewModel.setLocalError("Enter a valid EVM wallet address first.")
                            }
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Request nonce")
                }

                state.challenge?.let { challenge ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Nonce", style = MaterialTheme.typography.titleSmall)
                            Text(challenge.nonce, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = challenge.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } ?: Text(
                    text = "Request a nonce to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.signature,
                    onValueChange = viewModel::onSignatureChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Signed message") },
                    minLines = 4,
                )

                OutlinedTextField(
                    value = state.deviceToken,
                    onValueChange = viewModel::onDeviceTokenChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Device token (optional)") },
                    singleLine = true,
                )

                Button(
                    onClick = viewModel::connectWallet,
                    enabled = !state.isLoading && state.challenge != null && state.signature.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Connect Wallet")
                    }
                }
            }
        }

        state.errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

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
import androidx.compose.material3.Card
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
import com.letapay.app.core.ui.component.KptPrimaryButton
import kmp_project_template.feature.auth.generated.resources.Res
import kmp_project_template.feature.auth.generated.resources.feature_auth_connect_wallet_button
import kmp_project_template.feature.auth.generated.resources.feature_auth_device_token_label
import kmp_project_template.feature.auth.generated.resources.feature_auth_error_invalid_wallet
import kmp_project_template.feature.auth.generated.resources.feature_auth_error_nonce_required
import kmp_project_template.feature.auth.generated.resources.feature_auth_error_signature_required
import kmp_project_template.feature.auth.generated.resources.feature_auth_error_wallet_required
import kmp_project_template.feature.auth.generated.resources.feature_auth_nonce_title
import kmp_project_template.feature.auth.generated.resources.feature_auth_request_nonce_button
import kmp_project_template.feature.auth.generated.resources.feature_auth_request_nonce_hint
import kmp_project_template.feature.auth.generated.resources.feature_auth_signed_message_label
import kmp_project_template.feature.auth.generated.resources.feature_auth_subtitle
import kmp_project_template.feature.auth.generated.resources.feature_auth_title
import kmp_project_template.feature.auth.generated.resources.feature_auth_wallet_label
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
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
            text = stringResource(Res.string.feature_auth_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.feature_auth_subtitle),
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
                    label = { Text(stringResource(Res.string.feature_auth_wallet_label)) },
                    singleLine = true,
                )

                KptPrimaryButton(
                    onClick = {
                        runCatching { WalletAddress(state.walletAddress.trim()) }
                            .onSuccess(viewModel::requestNonce)
                            .onFailure {
                                viewModel.setLocalError(AuthValidationError.InvalidWalletFormat)
                            }
                    },
                    enabled = !state.isLoading,
                    text = stringResource(Res.string.feature_auth_request_nonce_button),
                )

                state.challenge?.let { challenge ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(Res.string.feature_auth_nonce_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(challenge.nonce, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = challenge.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } ?: Text(
                    text = stringResource(Res.string.feature_auth_request_nonce_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.signature,
                    onValueChange = viewModel::onSignatureChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.feature_auth_signed_message_label)) },
                    minLines = 4,
                )

                OutlinedTextField(
                    value = state.deviceToken,
                    onValueChange = viewModel::onDeviceTokenChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.feature_auth_device_token_label)) },
                    singleLine = true,
                )

                KptPrimaryButton(
                    onClick = viewModel::connectWallet,
                    enabled = !state.isLoading && state.challenge != null && state.signature.isNotBlank(),
                    text = stringResource(Res.string.feature_auth_connect_wallet_button),
                    isLoading = state.isLoading,
                )
            }
        }

        val validationErrorMessage = when (state.validationError) {
            AuthValidationError.InvalidWalletFormat -> stringResource(Res.string.feature_auth_error_invalid_wallet)
            AuthValidationError.NonceRequired -> stringResource(Res.string.feature_auth_error_nonce_required)
            AuthValidationError.WalletRequired -> stringResource(Res.string.feature_auth_error_wallet_required)
            AuthValidationError.SignatureRequired -> stringResource(Res.string.feature_auth_error_signature_required)
            null -> null
        }
        (validationErrorMessage ?: state.errorMessage)?.let {
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

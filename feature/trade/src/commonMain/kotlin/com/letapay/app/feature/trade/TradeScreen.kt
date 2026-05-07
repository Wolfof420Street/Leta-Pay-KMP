/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letapay.app.core.designsystem.theme.LetaSpacing
import kmp_project_template.feature.trade.generated.resources.Res
import kmp_project_template.feature.trade.generated.resources.feature_trade_describe_hint
import kmp_project_template.feature.trade.generated.resources.feature_trade_fee_label
import kmp_project_template.feature.trade.generated.resources.feature_trade_from_label
import kmp_project_template.feature.trade.generated.resources.feature_trade_get_quote_button
import kmp_project_template.feature.trade.generated.resources.feature_trade_getting_quote_button
import kmp_project_template.feature.trade.generated.resources.feature_trade_placeholder_amount
import kmp_project_template.feature.trade.generated.resources.feature_trade_swap_label
import kmp_project_template.feature.trade.generated.resources.feature_trade_title
import kmp_project_template.feature.trade.generated.resources.feature_trade_to_estimated_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CardMedium = RoundedCornerShape(16.dp)
private val Pill = RoundedCornerShape(50)

@Composable
fun TradeScreen(
    modifier: Modifier = Modifier,
    viewModel: TradeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LetaSpacing.md, vertical = LetaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LetaSpacing.md),
    ) {
        Text(
            text = stringResource(Res.string.feature_trade_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Swap card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(LetaSpacing.md)) {
                Text(
                    text = stringResource(Res.string.feature_trade_swap_label),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )

                // From field
                TokenAmountField(
                    label = stringResource(Res.string.feature_trade_from_label),
                    value = uiState.fromAmount,
                    onValueChange = viewModel::updateFromAmount,
                    tokenSymbol = uiState.fromToken,
                )

                // Arrow divider
                Text(
                    text = "↓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                // To field
                TokenAmountField(
                    label = stringResource(Res.string.feature_trade_to_estimated_label),
                    value = uiState.toAmount,
                    onValueChange = {},
                    tokenSymbol = uiState.toToken,
                    readOnly = true,
                )

                if (uiState.estimatedFeeUsd.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(Res.string.feature_trade_fee_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            uiState.estimatedFeeUsd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Surface(
                    onClick = viewModel::toggleLoading,
                    shape = Pill,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (uiState.isLoading) {
                                stringResource(Res.string.feature_trade_getting_quote_button)
                            } else {
                                stringResource(Res.string.feature_trade_get_quote_button)
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(Res.string.feature_trade_describe_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TokenAmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tokenSymbol: String,
    readOnly: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(LetaSpacing.md)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = readOnly,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Light,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                stringResource(Res.string.feature_trade_placeholder_amount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                            )
                        }
                        inner()
                    },
                )
                Surface(shape = Pill, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = tokenSymbol.ifEmpty { "---" },
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

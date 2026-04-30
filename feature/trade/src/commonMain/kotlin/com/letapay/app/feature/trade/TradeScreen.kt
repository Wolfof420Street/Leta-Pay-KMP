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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF061722)
private val SurfaceHigh = Color(0xFF0D2A38)
private val SurfaceBorder = Color(0xFF1F4E65)
private val AccentPrimary = Color(0xFF0EA5A6)
private val TextPrimary = Color(0xFFE8F5FA)
private val TextTertiary = Color(0xFF93B8C7)
private val CardMedium = RoundedCornerShape(16.dp)
private val Pill = RoundedCornerShape(50)

@Composable
fun TradeScreen(modifier: Modifier = Modifier) {
    var fromAmount by remember { mutableStateOf("") }
    var toAmount by remember { mutableStateOf("") }
    val fromToken = "ETH"
    val toToken = "USDC"
    val estimatedFeeUsd = "$2.50"
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Trade",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )

        // Swap card
        Surface(
            color = SurfaceHigh,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Swap",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )

                // From field
                TokenAmountField(
                    label = "From",
                    value = fromAmount,
                    onValueChange = { fromAmount = it },
                    tokenSymbol = fromToken,
                )

                // Arrow divider
                Text(
                    text = "↓",
                    color = AccentPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                // To field
                TokenAmountField(
                    label = "To (estimated)",
                    value = toAmount,
                    onValueChange = {},
                    tokenSymbol = toToken,
                    readOnly = true,
                )

                if (estimatedFeeUsd.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Network fee",
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(estimatedFeeUsd, color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Surface(
                    onClick = { isLoading = !isLoading },
                    shape = Pill,
                    color = AccentPrimary,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isLoading) {
                                "Getting quote..."
                            } else {
                                "Get Quote"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Text(
            text = "Or describe your trade in chat →",
            style = MaterialTheme.typography.bodyLarge,
            color = TextTertiary,
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
        color = Background,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = readOnly,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Light,
                    ),
                    cursorBrush = SolidColor(AccentPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                "0.00",
                                color = TextTertiary,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                            )
                        }
                        inner()
                    },
                )
                Surface(shape = Pill, color = SurfaceHigh) {
                    Text(
                        text = tokenSymbol.ifEmpty { "---" },
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

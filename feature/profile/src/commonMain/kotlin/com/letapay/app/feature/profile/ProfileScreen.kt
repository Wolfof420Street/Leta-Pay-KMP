/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF0A0A0F)
private val SurfaceHigh = Color(0xFF1C1C26)
private val TextPrimary = Color(0xFFF0F0F5)
private val TextSecondary = Color(0xFF9898B0)
private val TextTertiary = Color(0xFF5A5A70)
private val Error = Color(0xFFE74C3C)
private val CardMedium = RoundedCornerShape(16.dp)

@Composable
internal fun ProfileScreen(modifier: Modifier = Modifier) {
    val walletAddress = "0x7a3...9b24"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(24.dp))

        // Profile Card
        Surface(
            color = SurfaceHigh,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(TextTertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🤖", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Connected Wallet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = walletAddress,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(16.dp))

        SettingsRow("Security & Privacy")
        SettingsRow("Notifications")
        SettingsRow("Network Preferences")
        SettingsRow("App Theme")

        Spacer(Modifier.weight(1f))

        Surface(
            onClick = { /* Handle logout */ },
            color = SurfaceHigh,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Disconnect Wallet",
                style = MaterialTheme.typography.titleMedium,
                color = Error,
                modifier = Modifier.padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
        Text(
            text = "→",
            style = MaterialTheme.typography.bodyLarge,
            color = TextTertiary,
        )
    }
}

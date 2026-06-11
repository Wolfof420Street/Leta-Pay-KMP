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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letapay.app.feature.profile.generated.resources.Res
import com.letapay.app.feature.profile.generated.resources.feature_profile_account
import com.letapay.app.feature.profile.generated.resources.feature_profile_connected_wallet
import com.letapay.app.feature.profile.generated.resources.feature_profile_disconnect_wallet
import com.letapay.app.feature.profile.generated.resources.feature_profile_network_preferences
import com.letapay.app.feature.profile.generated.resources.feature_profile_no_active_session
import com.letapay.app.feature.profile.generated.resources.feature_profile_notifications
import com.letapay.app.feature.profile.generated.resources.feature_profile_security_privacy
import com.letapay.app.feature.profile.generated.resources.feature_profile_settings
import org.jetbrains.compose.resources.stringResource

private val CardMedium = RoundedCornerShape(16.dp)

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    val walletAddress = uiState.walletAddress ?: stringResource(Res.string.feature_profile_no_active_session)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.feature_profile_account),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))

        // Profile Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🤖", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.feature_profile_connected_wallet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = walletAddress,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.feature_profile_settings),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(16.dp))

        SettingsRow(stringResource(Res.string.feature_profile_security_privacy))
        SettingsRow(stringResource(Res.string.feature_profile_notifications))
        SettingsRow(stringResource(Res.string.feature_profile_network_preferences))
        SettingsRow(stringResource(Res.string.feature_profile_settings))

        Spacer(Modifier.weight(1f))

        Surface(
            onClick = { },
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CardMedium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.feature_profile_disconnect_wallet),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
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
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "→",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

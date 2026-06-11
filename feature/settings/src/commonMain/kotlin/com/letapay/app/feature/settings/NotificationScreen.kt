/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.letapay.app.core.ui.scaffold.KptScaffold
import com.letapay.app.feature.settings.generated.resources.Res
import com.letapay.app.feature.settings.generated.resources.feature_settings_notification_body
import com.letapay.app.feature.settings.generated.resources.feature_settings_notification_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    NotificationScreenContent(
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun NotificationScreenContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    KptScaffold(
        onNavigationIconClick = onBackClick,
        title = stringResource(Res.string.feature_settings_notification_title),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // NotificationScreenContent
            Text(
                text = stringResource(Res.string.feature_settings_notification_body),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

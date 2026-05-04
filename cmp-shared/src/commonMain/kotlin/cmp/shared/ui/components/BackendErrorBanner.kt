/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cmp.shared.generated.resources.Res
import cmp.shared.generated.resources.backend_error_banner_dismiss
import cmp.shared.ui.theme.LetaColors
import cmp.shared.ui.theme.LetaShapes
import cmp.shared.ui.theme.LetaSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackendErrorBanner(
    code: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    retryAfter: Int? = null,
) {
    val message = when (code) {
        "ADDRESS_REJECTED" -> "This address cannot receive funds."
        "RATE_LIMIT_EXCEEDED" -> "Too many requests. Please wait ${retryAfter ?: 60}s."
        "KILL_SWITCH_ACTIVE" -> "Transactions are temporarily paused."
        "QUOTE_EXPIRED" -> "Price quote expired. Refreshing..."
        "SLIPPAGE_EXCEEDED" -> "Price moved outside your slippage tolerance."
        "CIRCUIT_OPEN" -> "Service temporarily unavailable."
        "UPSTREAM_TIMEOUT" -> "Taking longer than expected. Try again."
        "TOKEN_FAMILY_REVOKED", "INVALID_REFRESH_TOKEN" -> "Your session was signed out for security reasons."
        else -> "Something went wrong. Please try again."
    }
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        modifier = modifier,
    ) {
        Surface(
            color = LetaColors.Error.copy(alpha = 0.12f),
            shape = LetaShapes.CardSmall,
            border = BorderStroke(1.dp, LetaColors.Error.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.padding(LetaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    color = LetaColors.Error,
                    modifier = Modifier.padding(end = LetaSpacing.md),
                )
                Button(onClick = onDismiss) {
                    Text(stringResource(Res.string.backend_error_banner_dismiss))
                }
            }
        }
    }
}

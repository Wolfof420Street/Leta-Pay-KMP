/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.letapay.app.core.designsystem.theme.LetaColors

private val LetaColorScheme = darkColorScheme(
    background = LetaColors.Background,
    surface = LetaColors.SurfaceElevated,
    surfaceVariant = LetaColors.SurfaceHigh,
    primary = LetaColors.AccentPrimary,
    secondary = LetaColors.AccentSecondary,
    tertiary = LetaColors.AccentGold,
    error = LetaColors.Error,
    onBackground = LetaColors.TextPrimary,
    onSurface = LetaColors.TextPrimary,
    onPrimary = LetaColors.TextOnAccent,
    outline = LetaColors.SurfaceBorder,
)

private val LetaTypographyTokens = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
)

@Composable
fun LetaPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LetaColorScheme,
        typography = LetaTypographyTokens,
        content = content,
    )
}

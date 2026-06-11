/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

object LetaColors {
    val Background = Color(0xFF0A0A0F)
    val SurfaceElevated = Color(0xFF13131A)
    val SurfaceHigh = Color(0xFF1C1C26)
    val SurfaceBorder = Color(0xFF2A2A38)

    val AccentPrimary = Color(0xFF6C63FF)
    val AccentSecondary = Color(0xFF4ECDC4)
    val AccentGold = Color(0xFFFFB547)

    val Success = Color(0xFF2ECC71)
    val Warning = Color(0xFFFFB547)
    val Error = Color(0xFFE74C3C)
    val Pending = Color(0xFF3498DB)

    val TextPrimary = Color(0xFFF0F0F5)
    val TextSecondary = Color(0xFF9898B0)
    val TextTertiary = Color(0xFF5A5A70)
    val TextOnAccent = Color(0xFFFFFFFF)

    val BubbleOutgoing = Color(0xFF6C63FF).copy(alpha = 0.15f)
    val BubbleIncoming = Color(0xFF1C1C26)
    val BubbleAi = Color(0xFF13131A)

    val ChainEthereum = Color(0xFF627EEA)
    val ChainPolygon = Color(0xFF8247E5)
    val ChainBase = Color(0xFF0052FF)
}

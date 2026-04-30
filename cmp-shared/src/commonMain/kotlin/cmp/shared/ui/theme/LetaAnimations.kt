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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object LetaAnimations {
    val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOutSoft = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val QUICK = 150
    const val NORMAL = 300
    const val SLOW = 500

    fun <T> normalTween() = tween<T>(NORMAL, easing = EaseOutExpo)
    fun <T> quickTween() = tween<T>(QUICK, easing = EaseInOutSoft)
}

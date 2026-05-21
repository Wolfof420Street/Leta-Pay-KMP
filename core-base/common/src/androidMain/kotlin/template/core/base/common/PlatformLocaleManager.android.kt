/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.common

import java.util.Locale

// Minimal Android `actual` implementation that avoids AndroidX dependencies.
// This acts as a safe stub for compilation and basic locale handling.
actual class PlatformLocaleManager() : LocaleManager {
    override fun setLocale(languageCode: String) {
        try {
            Locale.setDefault(Locale.forLanguageTag(languageCode))
        } catch (_: Exception) {
            // best-effort: ignore invalid tags during compile-time stub
        }
    }

    override fun resetToSystem() {
        Locale.setDefault(Locale.getDefault())
    }

    override fun currentLocale(): String =
        Locale.getDefault().toLanguageTag()
}

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

actual class PlatformLocaleManager() : LocaleManager {
    override fun setLocale(languageCode: String) {
        val locale = languageCode.toLocale()
        Locale.setDefault(locale)
    }

    override fun resetToSystem() {
        val systemLocale = Locale.getDefault(Locale.Category.DISPLAY)
        Locale.setDefault(systemLocale)
    }

    override fun currentLocale(): String = Locale.getDefault().toLanguageTag()

    private fun String.toLocale(): Locale {
        return if (contains("-")) {
            val parts = split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(this)
        }
    }
}

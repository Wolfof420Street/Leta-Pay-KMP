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

import kotlinx.browser.document
import kotlinx.browser.window

actual class PlatformLocaleManager actual constructor() : LocaleManager {
    actual override fun setLocale(languageCode: String) {
        document.documentElement?.setAttribute("lang", languageCode)
    }

    actual override fun resetToSystem() {
        val browserLang = window.navigator.language
        document.documentElement?.setAttribute("lang", browserLang)
    }

    actual override fun currentLocale(): String = window.navigator.language
}

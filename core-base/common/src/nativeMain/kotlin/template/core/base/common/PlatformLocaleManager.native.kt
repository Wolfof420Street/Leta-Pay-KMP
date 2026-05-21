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

actual class PlatformLocaleManager() : LocaleManager {
    override fun setLocale(languageCode: String) {
        // Platform specific implementation for iOS/macOS
    }

    override fun resetToSystem() {
        // Platform specific implementation for iOS/macOS
    }

    override fun currentLocale(): String = "en" // Fallback
}

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.common

import co.touchlab.kermit.Logger

object LoggerProvider {
    fun debug(tag: String, message: String) {
        Logger.d(tag, null, message)
    }

    fun error(tag: String, throwable: Throwable? = null, message: String) {
        Logger.e(tag, throwable, message)
    }
}

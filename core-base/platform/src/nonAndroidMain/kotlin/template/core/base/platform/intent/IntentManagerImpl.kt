/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package template.core.base.platform.intent

import androidx.compose.ui.graphics.ImageBitmap
import template.core.base.platform.model.MimeType

class IntentManagerImpl : IntentManager {
    override fun startActivity(intent: Any) {
        println("IntentManagerImpl.startActivity is a no-op on this platform: $intent")
    }

    override fun launchUri(uri: String) {
        println("IntentManagerImpl.launchUri is a no-op on this platform: $uri")
    }

    override fun shareText(text: String) {
        println("IntentManagerImpl.shareText is a no-op on this platform: $text")
    }

    override fun shareFile(fileUri: String, mimeType: MimeType) {
        println("IntentManagerImpl.shareFile is a no-op on this platform: $fileUri ($mimeType)")
    }

    override fun shareFile(fileUri: String, mimeType: MimeType, extraText: String) {
        println("IntentManagerImpl.shareFile(extraText) is a no-op on this platform: $fileUri ($mimeType)")
    }

    override suspend fun shareImage(title: String, image: ImageBitmap) {
        println("IntentManagerImpl.shareImage is a no-op on this platform: $title")
    }

    override fun createDocumentIntent(fileName: String): Any {
        println("IntentManagerImpl.createDocumentIntent is a no-op on this platform: $fileName")
        return fileName
    }

    override fun startApplicationDetailsSettingsActivity() {
        println("IntentManagerImpl.startApplicationDetailsSettingsActivity is a no-op on this platform")
    }
}

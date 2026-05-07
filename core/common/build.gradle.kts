/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
plugins {
    alias(libs.plugins.kmp.library.convention)
    alias(libs.plugins.kotlin.parcelize)
}

val xcodebuildAvailable = providers.systemProperty("os.name")
    .map { osName -> osName.contains("mac", ignoreCase = true) && file("/usr/bin/xcrun").exists() }
    .orElse(false)
    .get()

android {
    namespace = "com.letapay.app.core.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kermit.logging)
            api(libs.kotlinx.datetime)
            implementation(projects.coreBase.common)
        }
    }
}

tasks.matching {
    it.name.startsWith("link") && it.name.contains("Ios") && it.name.contains("Test")
}.configureEach {
    enabled = xcodebuildAvailable
}

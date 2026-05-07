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
    alias(libs.plugins.letapay.kmp.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

val xcodebuildAvailable = providers.systemProperty("os.name")
    .map { osName -> osName.contains("mac", ignoreCase = true) && file("/usr/bin/xcrun").exists() }
    .orElse(false)
    .get()

android {
    namespace = "com.letapay.app.core.database"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(projects.core.common)
            api(projects.core.model)
        }

        nonJsCommonMain.dependencies {
            implementation(libs.sqldelight.coroutines)
            implementation(projects.coreBase.database)
        }
    }
}

sqldelight {
    databases {
        create("LetaPayDatabase") {
            packageName.set("com.letapay.app.core.database")
        }
    }
}

tasks.matching {
    it.name.startsWith("link") && it.name.contains("Ios") && it.name.contains("Test")
}.configureEach {
    enabled = xcodebuildAvailable
}

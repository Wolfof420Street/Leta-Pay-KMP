/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
plugins {
    alias(libs.plugins.cmp.feature.convention)
}

android {
    namespace = "com.letapay.app.feature.chat"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.agent)
            implementation(projects.core.model)
            implementation(projects.core.data)
            implementation(projects.core.ai)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

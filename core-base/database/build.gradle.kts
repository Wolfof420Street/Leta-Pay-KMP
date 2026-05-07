/*
 * Copyright 2025 Leta Pay
 */
plugins {
    alias(libs.plugins.kmp.core.base.library.convention)
}

android {
    namespace = "template.core.base.database"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        desktopMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        nativeMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

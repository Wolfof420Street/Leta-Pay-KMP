package org.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure the Kotlin Multiplatform plugin with the default hierarchy template and additional targets.
 * This includes JVM, Android, iOS, JS and WASM targets.
 * @see KotlinMultiplatformExtension
 * @see configure
 */
@OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)
internal fun Project.configureKotlinMultiplatform() {
    val enableWasmTargets = providers
        .gradleProperty("letapay.enableWasm")
        .map(String::toBoolean)
        .orElse(false)
        .get()

    extensions.configure<KotlinMultiplatformExtension> {
        applyProjectHierarchyTemplate()

        jvm("desktop")
        androidTarget()
        iosSimulatorArm64()
        iosX64()
        iosArm64()
        js(IR) {
            this.nodejs()
            binaries.executable()
        }

        if (enableWasmTargets) {
            wasmJs() {
                // Library modules run wasm tests on Node to avoid browser webpack coupling.
                nodejs()
            }
        }

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }
}

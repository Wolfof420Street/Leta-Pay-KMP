/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
import io.gitlab.arturbosch.detekt.Detekt

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
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt.convention)
    alias(libs.plugins.spotless.convention)
}

group = "com.letapay.backend"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.letapay.backend.ApplicationKt")
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.sse)
    implementation("io.ktor:ktor-server-caching-headers:${libs.versions.ktorVersion.get()}")
    implementation("io.ktor:ktor-server-metrics-micrometer:${libs.versions.ktorVersion.get()}")
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktorVersion.get()}")
    implementation("io.ktor:ktor-server-double-receive:${libs.versions.ktorVersion.get()}")
    implementation("io.ktor:ktor-server-request-validation:${libs.versions.ktorVersion.get()}")

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.3")

    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.koog.agents)
    implementation(libs.koog.ktor)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation("io.lettuce:lettuce-core:6.5.5.RELEASE")

    implementation(libs.firebase.admin)
    implementation(libs.web3j)
    implementation(libs.argon2)
    implementation(libs.java.jwt)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.h2)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    runtimeOnly(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("buildFatJar") {
    archiveClassifier.set("all")
    isZip64 = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.EC",
        "META-INF/MANIFEST.MF",
    )
    manifest {
        attributes["Main-Class"] = "com.letapay.backend.ApplicationKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) },
    )
}

tasks.named<Detekt>("detekt") {
    setSource(files("src/main/kotlin", "src/test/kotlin"))
    include("**/*.kt")
    exclude("**/resources/**")
    exclude("**/build/**")
    exclude("**/generated/**")
}

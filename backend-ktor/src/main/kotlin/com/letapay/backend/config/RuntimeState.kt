/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.config

import com.google.firebase.FirebaseApp
import io.ktor.server.config.ApplicationConfig
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

object RuntimeState {
    private val misconfigured = AtomicBoolean(false)
    private val firebaseHealthy = AtomicBoolean(false)

    fun markMisconfigured(value: Boolean) {
        misconfigured.set(value)
    }

    fun isMisconfigured(): Boolean = misconfigured.get()

    fun markFirebaseHealthy(value: Boolean) {
        firebaseHealthy.set(value)
    }

    fun isFirebaseHealthy(): Boolean = firebaseHealthy.get()
}

fun initFirebase(config: ApplicationConfig) {
    val serviceAccount = System.getenv("FIREBASE_SA_JSON")
        ?.takeIf(String::isNotBlank)
        ?.let { ByteArrayInputStream(it.toByteArray()) }
        ?: System.getenv("FIREBASE_SA_PATH")
            ?.takeIf(String::isNotBlank)
            ?.let(::FileInputStream)
        ?: config.propertyOrNull("firebase.serviceAccountPath")
            ?.getString()
            ?.takeIf(String::isNotBlank)
            ?.let(::FileInputStream)
        ?: error("Firebase service account credentials are missing.")

    val options = com.google.firebase.FirebaseOptions.builder()
        .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
        .build()

    if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options)
    }
    RuntimeState.markFirebaseHealthy(true)
}

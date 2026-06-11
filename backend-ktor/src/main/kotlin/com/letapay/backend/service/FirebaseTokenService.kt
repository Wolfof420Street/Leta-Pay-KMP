/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.letapay.backend.error.FirebaseUnavailableError
import java.io.ByteArrayInputStream
import java.io.FileInputStream

interface FirebaseTokenService {
    fun createCustomToken(walletAddress: String, sessionId: String): String
}

class FirebaseAdminTokenService : FirebaseTokenService {
    private val auth: FirebaseAuth by lazy {
        initializeFirebase()
        FirebaseAuth.getInstance()
    }

    override fun createCustomToken(walletAddress: String, sessionId: String): String =
        auth.createCustomToken(
            walletAddress,
            mapOf(
                "wallet_address" to walletAddress,
                "session_id" to sessionId,
            ),
        )

    private fun initializeFirebase() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            return
        }

        val serviceAccount = System.getenv("FIREBASE_SA_JSON")
            ?.takeIf(String::isNotBlank)
            ?.byteInputStream()
            ?: System.getenv("FIREBASE_SA_PATH")
                ?.takeIf(String::isNotBlank)
                ?.let(::FileInputStream)
            ?: ByteArrayInputStream("{}".toByteArray())

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()
        FirebaseApp.initializeApp(options)
    }
}

class DisabledFirebaseTokenService : FirebaseTokenService {
    override fun createCustomToken(walletAddress: String, sessionId: String): String =
        throw FirebaseUnavailableError()
}

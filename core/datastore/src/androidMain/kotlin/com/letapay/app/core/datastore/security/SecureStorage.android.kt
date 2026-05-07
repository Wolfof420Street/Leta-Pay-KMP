/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.datastore.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.russhwolf.settings.Settings
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object SecureStorage {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "letapay_secure_storage_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private val settings = Settings()
    private val keyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }
    private val secretKey: SecretKey
        get() {
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            return createSecretKey()
        }

    actual suspend fun putString(
        key: String,
        value: String,
    ) {
        settings.putString(key, encrypt(value))
    }

    actual suspend fun getString(key: String): String? {
        val encrypted = settings.getStringOrNull(key) ?: return null
        return runCatching { decrypt(encrypted) }.getOrNull()
    }

    actual suspend fun remove(key: String) {
        settings.remove(key)
    }

    actual suspend fun clear() {
        settings.clear()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.encodeToByteArray())
        val merged = ByteArray(iv.size + encrypted.size)
        iv.copyInto(merged, endIndex = IV_SIZE_BYTES)
        encrypted.copyInto(merged, destinationOffset = IV_SIZE_BYTES)
        return Base64.getEncoder().encodeToString(merged)
    }

    private fun decrypt(payload: String): String {
        val decoded = Base64.getDecoder().decode(payload)
        val iv = decoded.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = decoded.copyOfRange(IV_SIZE_BYTES, decoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext).decodeToString()
    }
}

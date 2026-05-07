/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.common

import kotlin.random.Random

actual object UUIDGenerator {
    actual fun generateUUID(): String {
        return randomUuidV4()
    }
}

private fun randomUuidV4(): String {
    val bytes = ByteArray(16) { Random.nextInt(0, 256).toByte() }
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    return bytes.toUuidString()
}

private fun ByteArray.toUuidString(): String = buildString(36) {
    for (index in indices) {
        append(this@toUuidString[index].toHexByte())
        when (index) {
            3, 5, 7, 9 -> append('-')
        }
    }
}

private fun Byte.toHexByte(): String = ((toInt() and 0xff) + 0x100).toString(16).substring(1)

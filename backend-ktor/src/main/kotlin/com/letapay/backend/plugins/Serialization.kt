/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.plugins

import com.letapay.backend.model.ai.ParseRequest
import com.letapay.backend.model.ai.PlanRequest
import com.letapay.backend.model.auth.NonceRequest
import com.letapay.backend.model.auth.RefreshRequest
import com.letapay.backend.model.auth.VerifyRequest
import com.letapay.backend.model.device.DeviceTokenDeleteRequest
import com.letapay.backend.model.device.DeviceTokenRequest
import com.letapay.backend.model.swap.SwapExecuteRequest
import com.letapay.backend.model.swap.SwapQuoteRequest
import com.letapay.backend.model.transaction.BuildRequest
import com.letapay.backend.model.transaction.SendRequest
import com.letapay.backend.model.yield.StakeRequest
import com.letapay.backend.model.yield.UnstakeRequest
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import kotlinx.serialization.json.Json
import java.math.BigDecimal

fun Application.configureSerialization() {
    install(DoubleReceive)
    install(RequestValidation) {
        registerDtoValidators()
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                explicitNulls = false
            },
        )
    }
}

private fun RequestValidationConfig.registerDtoValidators() {
    validate<NonceRequest> { it.validateNonceRequest() }
    validate<VerifyRequest> { it.validateVerifyRequest() }
    validate<RefreshRequest> { it.required("refreshToken", it.refreshToken) }
    validate<BuildRequest> { it.required("to", it.to) }
    validate<SendRequest> {
        if (it.signedTx.startsWith("0x")) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("signedTx must be hex prefixed with 0x.")
        }
    }
    validate<DeviceTokenRequest> { it.validateDeviceTokenRequest() }
    validate<DeviceTokenDeleteRequest> { it.required("fcmToken", it.fcmToken) }
    validate<SwapQuoteRequest> { it.validateSwapQuoteRequest() }
    validate<SwapExecuteRequest> { it.validateSwapExecuteRequest() }
    validate<StakeRequest> { it.validateStakeRequest() }
    validate<UnstakeRequest> { it.validateUnstakeRequest() }
    validate<ParseRequest> { it.required("message", it.message) }
    validate<PlanRequest> { it.required("message", it.message) }
}

private fun NonceRequest.validateNonceRequest(): ValidationResult =
    if (walletAddress.startsWith("0x") && walletAddress.length == 42) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid("walletAddress must be a 42-char 0x address.")
    }

private fun VerifyRequest.validateVerifyRequest(): ValidationResult =
    when {
        walletAddress.isBlank() || !walletAddress.startsWith("0x") ->
            ValidationResult.Invalid("walletAddress is invalid.")
        message.isBlank() -> ValidationResult.Invalid("message is required.")
        signature.isBlank() -> ValidationResult.Invalid("signature is required.")
        else -> ValidationResult.Valid
    }

private fun DeviceTokenRequest.validateDeviceTokenRequest(): ValidationResult =
    if (fcmToken.isBlank() || platform.isBlank()) {
        ValidationResult.Invalid("fcmToken and platform are required.")
    } else {
        ValidationResult.Valid
    }

private fun SwapQuoteRequest.validateSwapQuoteRequest(): ValidationResult {
    val amountValue = amount.toBigDecimalOrNull()
    return when {
        fromAsset.isBlank() || toAsset.isBlank() ->
            ValidationResult.Invalid("fromAsset and toAsset are required.")
        amountValue == null || amountValue <= BigDecimal.ZERO ->
            ValidationResult.Invalid("amount must be a positive decimal.")
        slippageBps !in 10..1_000 ->
            ValidationResult.Invalid("slippageBps must be between 10 and 1000.")
        else -> ValidationResult.Valid
    }
}

private fun SwapExecuteRequest.validateSwapExecuteRequest(): ValidationResult =
    if (quoteId.isBlank() || idempotencyKey.isBlank()) {
        ValidationResult.Invalid("quoteId and idempotencyKey are required.")
    } else {
        ValidationResult.Valid
    }

private fun StakeRequest.validateStakeRequest(): ValidationResult =
    if (opportunityId.isBlank() || idempotencyKey.isBlank()) {
        ValidationResult.Invalid("opportunityId and idempotencyKey are required.")
    } else {
        ValidationResult.Valid
    }

private fun UnstakeRequest.validateUnstakeRequest(): ValidationResult =
    if (positionId.isBlank() || idempotencyKey.isBlank()) {
        ValidationResult.Invalid("positionId and idempotencyKey are required.")
    } else {
        ValidationResult.Valid
    }

private fun Any.required(name: String, value: String): ValidationResult =
    if (value.isBlank()) ValidationResult.Invalid("$name is required.") else ValidationResult.Valid

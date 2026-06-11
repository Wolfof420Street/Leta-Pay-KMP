/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.backend.error

import io.ktor.http.HttpStatusCode

sealed class BackendException(
    val code: String,
    val status: HttpStatusCode,
    message: String,
    val retryAfterSeconds: Int? = null,
) : RuntimeException(message)

class InvalidWalletSignatureError :
    BackendException("INVALID_WALLET_SIGNATURE", HttpStatusCode.Unauthorized, "Wallet signature verification failed.")

class NonceExpiredError :
    BackendException("NONCE_EXPIRED", HttpStatusCode.Unauthorized, "Nonce has expired.")

class NonceAlreadyUsedError :
    BackendException("NONCE_ALREADY_USED", HttpStatusCode.Unauthorized, "Nonce has already been consumed.")

class NonceMissingError :
    BackendException("NONCE_MISSING", HttpStatusCode.Unauthorized, "Nonce was not found in the signed message.")

class InvalidRefreshTokenError :
    BackendException("INVALID_REFRESH_TOKEN", HttpStatusCode.Unauthorized, "Refresh token is invalid.")

class RefreshTokenExpiredError :
    BackendException("REFRESH_TOKEN_EXPIRED", HttpStatusCode.Unauthorized, "Refresh token has expired.")

class TokenFamilyRevokedError :
    BackendException("TOKEN_FAMILY_REVOKED", HttpStatusCode.Unauthorized, "Refresh token family has been revoked.")

class RateLimitExceededError(retryAfterSeconds: Int) :
    BackendException("RATE_LIMIT_EXCEEDED", HttpStatusCode.TooManyRequests, "Too many requests.", retryAfterSeconds)

class KillSwitchActiveError :
    BackendException(
        "KILL_SWITCH_ACTIVE",
        HttpStatusCode.ServiceUnavailable,
        "Value-moving operations are temporarily suspended.",
    )

class MissingIdempotencyKeyError :
    BackendException("MISSING_IDEMPOTENCY_KEY", HttpStatusCode.BadRequest, "Idempotency-Key header is required.")

class IdempotencyConflictError :
    BackendException(
        "IDEMPOTENCY_CONFLICT",
        HttpStatusCode.Conflict,
        "Idempotency key was already used with a different payload.",
    )

class InvalidTxHashError :
    BackendException("INVALID_TX_HASH", HttpStatusCode.BadRequest, "Transaction hash format is invalid.")

class AddressRejectedError :
    BackendException("ADDRESS_REJECTED", HttpStatusCode.UnprocessableEntity, "Recipient address failed screening.")

class QuoteExpiredError :
    BackendException("QUOTE_EXPIRED", HttpStatusCode.UnprocessableEntity, "Swap quote has expired.")

class QuoteMismatchError :
    BackendException(
        "QUOTE_MISMATCH",
        HttpStatusCode.UnprocessableEntity,
        "Swap execution parameters no longer match the quote.",
    )

class SlippageExceededError :
    BackendException(
        "SLIPPAGE_EXCEEDED",
        HttpStatusCode.UnprocessableEntity,
        "Quoted price impact exceeds slippage tolerance.",
    )

class CoinbaseApiError(
    val providerCode: String? = null,
    message: String,
) : BackendException("COINBASE_API_ERROR", HttpStatusCode.BadGateway, message)

class PositionNotFoundError :
    BackendException("POSITION_NOT_FOUND", HttpStatusCode.NotFound, "Staking position was not found.")

class PositionWrongOwnerError :
    BackendException(
        "POSITION_WRONG_OWNER",
        HttpStatusCode.Forbidden,
        "Position does not belong to the authenticated wallet.",
    )

class PositionNotActiveError :
    BackendException("POSITION_NOT_ACTIVE", HttpStatusCode.Conflict, "Position is not active.")

class OpportunityNotFoundError :
    BackendException("OPPORTUNITY_NOT_FOUND", HttpStatusCode.BadRequest, "Yield opportunity was not found.")

class OpportunityDisabledError :
    BackendException("OPPORTUNITY_DISABLED", HttpStatusCode.BadRequest, "Yield opportunity is disabled.")

class UnauthorizedSessionError :
    BackendException("UNAUTHORIZED_SESSION", HttpStatusCode.Unauthorized, "Session token is invalid or expired.")

class FirebaseUnavailableError :
    BackendException("FIREBASE_UNAVAILABLE", HttpStatusCode.ServiceUnavailable, "Firebase auth is unavailable.")

class UpstreamTimeoutError :
    BackendException("UPSTREAM_TIMEOUT", HttpStatusCode.GatewayTimeout, "Upstream service timed out.")

class CircuitOpenError(name: String) :
    BackendException("CIRCUIT_OPEN", HttpStatusCode.ServiceUnavailable, "$name circuit breaker is open.")

class PayloadTooLargeError :
    BackendException("PAYLOAD_TOO_LARGE", HttpStatusCode.PayloadTooLarge, "Request body exceeds the 64 KB limit.")

class InvalidRequestError(message: String) :
    BackendException("INVALID_REQUEST", HttpStatusCode.BadRequest, message)

class TransactionValueExceededError(maxTransactionValueEth: String) :
    BackendException(
        "MAX_TRANSACTION_VALUE_EXCEEDED",
        HttpStatusCode.BadRequest,
        "Transaction amount exceeds MAX_TRANSACTION_VALUE_ETH ($maxTransactionValueEth).",
    )

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.core.data.repositoryImpl

import com.letapay.app.core.common.DevicePlatform
import com.letapay.app.core.common.currentDevicePlatform
import com.letapay.app.core.data.repository.ConnectWalletRequest
import com.letapay.app.core.data.repository.FirebaseAuthRepository
import com.letapay.app.core.data.repository.SessionRepository
import com.letapay.app.core.data.repository.SessionState
import com.letapay.app.core.datastore.UserPreferencesRepository
import com.letapay.app.core.datastore.security.SecureStorage
import com.letapay.app.core.model.auth.WalletSession
import com.letapay.app.core.model.blockchain.WalletAddress
import com.letapay.app.core.network.BackendApiException
import com.letapay.app.core.network.auth.AuthApi
import com.letapay.app.core.network.auth.VerifySignatureRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import template.core.base.common.manager.DispatcherManager

class SessionRepositoryImpl(
    private val authApi: AuthApi,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    dispatcherManager: DispatcherManager,
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherManager.main)
    private val mutableSessionState = MutableStateFlow(SessionState())

    override val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    init {
        scope.launch {
            bootstrap()
        }
    }

    override suspend fun bootstrap() {
        mutableSessionState.update { it.copy(isLoading = true, errorMessage = null) }

        val storedSession = loadPersistedSession()
        if (storedSession == null) {
            clearPersistedSession()
            mutableSessionState.update { SessionState(isBootstrapped = true) }
            return
        }

        mutableSessionState.update {
            it.copy(
                session = storedSession,
                isLoading = true,
                errorMessage = null,
            )
        }

        val storedRefreshToken = storedSession.refreshToken
        val refreshedSession = if (storedRefreshToken.isNullOrBlank()) {
            storedSession
        } else {
            runCatching {
                authApi.refreshSession(
                    refreshToken = storedRefreshToken,
                    walletAddress = storedSession.walletAddress,
                )
            }.getOrElse { throwable ->
                if (throwable.isFatalSessionError()) {
                    clearPersistedSession()
                    mutableSessionState.update { SessionState(isBootstrapped = true) }
                } else {
                    clearPersistedSession()
                    mutableSessionState.update {
                        SessionState(
                            isBootstrapped = true,
                            errorMessage = throwable.message ?: "Unable to restore session.",
                        )
                    }
                }
                return
            }
        }

        persistSession(refreshedSession)
        mutableSessionState.update {
            it.copy(
                isBootstrapped = true,
                isLoading = false,
                session = refreshedSession,
                errorMessage = null,
            )
        }
    }

    override suspend fun requestNonce(walletAddress: WalletAddress) {
        mutableSessionState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                challenge = null,
            )
        }

        runCatching {
            authApi.requestNonce(walletAddress)
        }.onSuccess { challenge ->
            mutableSessionState.update {
                it.copy(
                    isBootstrapped = true,
                    isLoading = false,
                    challenge = challenge,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            val shouldClearState = throwable.isFatalSessionError()
            if (shouldClearState) {
                clearPersistedSession()
            }
            mutableSessionState.update {
                it.copy(
                    isBootstrapped = true,
                    isLoading = false,
                    errorMessage = throwable.toUserMessage(),
                )
            }
        }
    }

    override suspend fun authenticate(request: ConnectWalletRequest) {
        mutableSessionState.update { it.copy(isLoading = true, errorMessage = null) }

        runCatching {
            authApi.verifySignature(
                VerifySignatureRequest(
                    walletAddress = request.walletAddress,
                    signature = request.signature,
                    nonce = request.nonce,
                    message = request.message,
                ),
            )
        }.onSuccess { session ->
            persistSession(session)
            mutableSessionState.update {
                it.copy(
                    isBootstrapped = true,
                    isLoading = false,
                    session = session,
                    challenge = null,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            if (throwable.isNonceRecoveryRequired()) {
                clearPersistedSession()
            }
            mutableSessionState.update {
                it.copy(
                    isBootstrapped = true,
                    isLoading = false,
                    errorMessage = throwable.toUserMessage(),
                )
            }
        }
    }

    override suspend fun refreshSession() {
        val refreshToken = sessionState.value.session?.refreshToken
            ?: SecureStorage.getString(REFRESH_TOKEN_KEY)

        if (refreshToken.isNullOrBlank()) {
            mutableSessionState.update {
                it.copy(errorMessage = "No refresh token is available for this session.")
            }
            return
        }

        mutableSessionState.update { it.copy(isLoading = true, errorMessage = null) }

        runCatching {
            authApi.refreshSession(
                refreshToken = refreshToken,
                walletAddress = sessionState.value.session?.walletAddress ?: run {
                    mutableSessionState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No wallet address is available for this session.",
                        )
                    }
                    return
                },
            )
        }.onSuccess { session ->
            persistSession(session)
            mutableSessionState.update {
                it.copy(
                    isBootstrapped = true,
                    isLoading = false,
                    session = session,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            clearPersistedSession()
            if (throwable.isFatalSessionError()) {
                mutableSessionState.update { SessionState(isBootstrapped = true) }
            } else {
                mutableSessionState.update {
                    SessionState(
                        isBootstrapped = true,
                        errorMessage = throwable.toUserMessage(),
                    )
                }
            }
        }
    }

    override suspend fun clearError() {
        mutableSessionState.update { it.copy(errorMessage = null) }
    }

    override suspend fun logout() {
        val sessionToken = sessionState.value.session?.sessionToken

        if (!sessionToken.isNullOrBlank()) {
            runCatching {
                authApi.revokeSession(sessionToken)
            }
        }

        clearPersistedSession()
        mutableSessionState.update { SessionState(isBootstrapped = true) }
    }

    private suspend fun loadPersistedSession(): WalletSession? {
        val sessionToken = SecureStorage.getString(SESSION_TOKEN_KEY)
        val walletAddress = SecureStorage.getString(WALLET_ADDRESS_KEY)?.let {
            runCatching { WalletAddress(it) }.getOrNull()
        }

        if (sessionToken == null || walletAddress == null) {
            return null
        }

        return WalletSession(
            walletAddress = walletAddress,
            sessionToken = sessionToken,
            refreshToken = SecureStorage.getString(REFRESH_TOKEN_KEY),
            firebaseToken = SecureStorage.getString(FIREBASE_TOKEN_KEY),
            userId = SecureStorage.getString(USER_ID_KEY) ?: walletAddress.value,
            displayName = SecureStorage.getString(DISPLAY_NAME_KEY),
            avatarUrl = SecureStorage.getString(AVATAR_URL_KEY),
            createdAtEpochMillis = SecureStorage.getString(CREATED_AT_KEY)?.toLongOrNull(),
            lastActiveAtEpochMillis = SecureStorage.getString(LAST_ACTIVE_AT_KEY)?.toLongOrNull(),
        )
    }

    private suspend fun persistSession(session: WalletSession) {
        SecureStorage.putString(SESSION_TOKEN_KEY, session.sessionToken)
        SecureStorage.putString(WALLET_ADDRESS_KEY, session.walletAddress.value)
        SecureStorage.putString(USER_ID_KEY, session.userId)

        val refreshToken = session.refreshToken
        if (!refreshToken.isNullOrBlank() && currentDevicePlatform() != DevicePlatform.Web) {
            SecureStorage.putString(REFRESH_TOKEN_KEY, refreshToken)
        } else {
            SecureStorage.remove(REFRESH_TOKEN_KEY)
        }

        val firebaseToken = session.firebaseToken
        if (!firebaseToken.isNullOrBlank()) {
            SecureStorage.putString(FIREBASE_TOKEN_KEY, firebaseToken)
        } else {
            SecureStorage.remove(FIREBASE_TOKEN_KEY)
        }

        session.displayName?.let {
            SecureStorage.putString(DISPLAY_NAME_KEY, it)
        } ?: SecureStorage.remove(DISPLAY_NAME_KEY)

        session.avatarUrl?.let {
            SecureStorage.putString(AVATAR_URL_KEY, it)
        } ?: SecureStorage.remove(AVATAR_URL_KEY)

        session.createdAtEpochMillis?.let {
            SecureStorage.putString(CREATED_AT_KEY, it.toString())
        } ?: SecureStorage.remove(CREATED_AT_KEY)

        session.lastActiveAtEpochMillis?.let {
            SecureStorage.putString(LAST_ACTIVE_AT_KEY, it.toString())
        } ?: SecureStorage.remove(LAST_ACTIVE_AT_KEY)

        userPreferencesRepository.setActiveUserId(session.userId)
        userPreferencesRepository.setIsAuthenticated(true)

        session.firebaseToken?.let { firebaseToken ->
            firebaseAuthRepository.signIn(
                customToken = firebaseToken,
                walletAddress = session.walletAddress.value,
                sessionId = session.userId,
            )
        }
    }

    private suspend fun clearPersistedSession() {
        SecureStorage.remove(SESSION_TOKEN_KEY)
        SecureStorage.remove(REFRESH_TOKEN_KEY)
        SecureStorage.remove(WALLET_ADDRESS_KEY)
        SecureStorage.remove(USER_ID_KEY)
        SecureStorage.remove(FIREBASE_TOKEN_KEY)
        SecureStorage.remove(DISPLAY_NAME_KEY)
        SecureStorage.remove(AVATAR_URL_KEY)
        SecureStorage.remove(CREATED_AT_KEY)
        SecureStorage.remove(LAST_ACTIVE_AT_KEY)
        firebaseAuthRepository.signOut()
        userPreferencesRepository.clearUserData()
    }

    private companion object {
        const val SESSION_TOKEN_KEY = "session_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val WALLET_ADDRESS_KEY = "wallet_address"
        const val USER_ID_KEY = "wallet_user_id"
        const val FIREBASE_TOKEN_KEY = "wallet_firebase_token"
        const val DISPLAY_NAME_KEY = "wallet_display_name"
        const val AVATAR_URL_KEY = "wallet_avatar_url"
        const val CREATED_AT_KEY = "wallet_created_at"
        const val LAST_ACTIVE_AT_KEY = "wallet_last_active_at"
    }
}

private fun Throwable.isFatalSessionError(): Boolean =
    this is BackendApiException && code in setOf("INVALID_REFRESH_TOKEN", "TOKEN_FAMILY_REVOKED")

private fun Throwable.isNonceRecoveryRequired(): Boolean =
    this is BackendApiException && code in setOf("NONCE_EXPIRED", "NONCE_ALREADY_USED")

private fun Throwable.toUserMessage(): String = when (this) {
    is BackendApiException -> when (code) {
        "RATE_LIMIT_EXCEEDED" -> {
            val retryAfterText = retryAfterSeconds?.let { " Please wait $it seconds." }.orEmpty()
            "Too many requests.$retryAfterText"
        }
        "NONCE_EXPIRED",
        "NONCE_ALREADY_USED",
        -> "Your sign-in challenge expired. Please reconnect your wallet."
        "INVALID_REFRESH_TOKEN" -> "Your session expired. Please connect your wallet again."
        "TOKEN_FAMILY_REVOKED" -> "Your session was revoked for security reasons. Please reconnect."
        "KILL_SWITCH_ACTIVE" -> "Transactions are temporarily paused. Try again shortly."
        else -> message
    }
    else -> message ?: "Unable to complete the request."
}

/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letapay.app.core.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val walletAddress: String? = null,
    val hasActiveSession: Boolean = false,
)

class ProfileViewModel(
    sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = sessionRepository.sessionState
        .map { state ->
            ProfileUiState(
                walletAddress = state.session?.walletAddress?.value,
                hasActiveSession = state.session != null,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )
}

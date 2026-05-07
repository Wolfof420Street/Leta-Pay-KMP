/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package com.letapay.app.feature.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letapay.app.core.designsystem.theme.LetaSpacing
import com.letapay.app.core.model.blockchain.ChainId
import com.letapay.app.core.model.wallet.AssetBalance
import kmp_project_template.feature.wallet.generated.resources.Res
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_action_history
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_action_send
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_action_stake
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_action_swap
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_assets
import kmp_project_template.feature.wallet.generated.resources.feature_wallet_total_balance
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// Chain-specific colors remain hardcoded as they represent brand colors
private val ChainEthereum = androidx.compose.ui.graphics.Color(0xFF627EEA)
private val ChainPolygon = androidx.compose.ui.graphics.Color(0xFF8247E5)
private val ChainBase = androidx.compose.ui.graphics.Color(0xFF0052FF)

private val CardMedium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
private val Pill = androidx.compose.foundation.shape.RoundedCornerShape(50)

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = koinViewModel(),
) {
    val uiState by viewModel.stateFlow.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            BalanceHeroSection(
                totalUsd = uiState.totalUsdValue ?: "$0.00",
                isLoading = uiState.isLoading,
            )
        }
        item {
            ActionButtonRow(
                onSend = { },
                onSwap = { },
                onStake = { },
                onHistory = { },
            )
        }
        item {
            Text(
                text = stringResource(Res.string.feature_wallet_assets),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = LetaSpacing.md, vertical = LetaSpacing.sm),
            )
        }
        if (uiState.isLoading) {
            items(count = 4) {
                ShimmerAssetRow()
            }
        } else {
            itemsIndexed(uiState.balances) { index, asset ->
                StaggeredAssetRow(asset = asset, index = index)
            }
        }
    }
}

@Composable
private fun BalanceHeroSection(totalUsd: String, isLoading: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(600),
        label = "heroAlpha",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LetaSpacing.lg, vertical = LetaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.feature_wallet_total_balance),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(LetaSpacing.sm))
        if (isLoading) {
            Box(
                Modifier
                    .width(200.dp)
                    .height(52.dp)
                    .clip(CardMedium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Text(
                text = totalUsd,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = animatedAlpha),
                fontWeight = FontWeight.Light,
            )
        }
    }
}

@Composable
private fun ActionButtonRow(
    onSend: () -> Unit,
    onSwap: () -> Unit,
    onStake: () -> Unit,
    onHistory: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LetaSpacing.md, vertical = LetaSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(LetaSpacing.sm),
    ) {
        ActionChip(
            label = stringResource(Res.string.feature_wallet_action_send),
            color = MaterialTheme.colorScheme.primary,
            onClick = onSend,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            label = stringResource(Res.string.feature_wallet_action_swap),
            color = MaterialTheme.colorScheme.primary,
            onClick = onSwap,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            label = stringResource(Res.string.feature_wallet_action_stake),
            color = MaterialTheme.colorScheme.tertiary,
            onClick = onStake,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            label = stringResource(Res.string.feature_wallet_action_history),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onHistory,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = Pill,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = LetaSpacing.sm, vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun StaggeredAssetRow(asset: AssetBalance, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = tween(300),
        ) + fadeIn(tween(300)),
    ) {
        AssetRow(asset = asset)
    }
}

@Composable
private fun AssetRow(asset: AssetBalance) {
    val chainColor = when (asset.chainId.value) {
        ChainId.Ethereum.value -> ChainEthereum
        ChainId.Polygon.value -> ChainPolygon
        ChainId.Base.value -> ChainBase
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LetaSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Token icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(chainColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = asset.symbol.take(2),
                style = MaterialTheme.typography.labelMedium,
                color = chainColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                asset.symbol,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                asset.symbol,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = asset.usdValue ?: "$0.00",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = asset.amount,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShimmerAssetRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LetaSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(CardMedium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.25f)
                    .height(12.dp)
                    .clip(CardMedium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .width(70.dp)
                    .height(14.dp)
                    .clip(CardMedium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier
                    .width(50.dp)
                    .height(12.dp)
                    .clip(CardMedium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

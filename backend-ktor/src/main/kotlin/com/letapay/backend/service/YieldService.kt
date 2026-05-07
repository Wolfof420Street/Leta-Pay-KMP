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

import com.letapay.backend.db.StakingPositions
import com.letapay.backend.error.InvalidRequestError
import com.letapay.backend.error.OpportunityDisabledError
import com.letapay.backend.error.OpportunityNotFoundError
import com.letapay.backend.error.PositionNotActiveError
import com.letapay.backend.error.PositionNotFoundError
import com.letapay.backend.error.PositionWrongOwnerError
import com.letapay.backend.model.swap.UnsignedTx
import com.letapay.backend.model.yield.StakeResponse
import com.letapay.backend.model.yield.StakingPosition
import com.letapay.backend.model.yield.StakingStatus
import com.letapay.backend.model.yield.UnstakeResponse
import com.letapay.backend.model.yield.YieldOpportunity
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint16
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

interface YieldService {
    suspend fun getOpportunities(chain: Long?): List<YieldOpportunity>

    suspend fun buildStakeTx(
        opportunityId: String,
        amount: String,
        walletAddress: String,
    ): UnsignedTx

    suspend fun buildUnstakeTx(
        positionId: String,
        amount: String,
        walletAddress: String,
    ): UnsignedTx

    suspend fun getPositions(walletAddress: String): List<StakingPosition>

    suspend fun createStakePosition(
        opportunityId: String,
        amount: String,
        walletAddress: String,
    ): StakeResponse

    suspend fun prepareUnstake(
        positionId: String,
        amount: String,
        walletAddress: String,
    ): UnstakeResponse

    suspend fun requireOpportunity(opportunityId: String): YieldOpportunity

    suspend fun reconcilePositions()
}

@Suppress("TooManyFunctions")
class DefaultYieldService(
    private val pricingService: PricingService,
) : YieldService {
    private suspend fun <T> db(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun getOpportunities(chain: Long?): List<YieldOpportunity> =
        opportunities.filter { chain == null || it.chain == chain }

    override suspend fun buildStakeTx(
        opportunityId: String,
        amount: String,
        walletAddress: String,
    ): UnsignedTx {
        val opportunity = findOpportunity(opportunityId)
        ensureEnabled(opportunity)
        ensureMinAmount(amount, opportunity)

        return when (opportunity.opportunityId) {
            LIDO_OPPORTUNITY_ID -> buildLidoStakeTx(amount, opportunity.chain)
            AAVE_OPPORTUNITY_ID -> buildAaveSupplyTx(amount, walletAddress, opportunity.chain)
            else -> throw OpportunityNotFoundError()
        }
    }

    override suspend fun buildUnstakeTx(
        positionId: String,
        amount: String,
        walletAddress: String,
    ): UnsignedTx {
        ensurePositiveAmount(amount)
        val position = loadPosition(positionId)
        ensurePositionOwner(position, walletAddress)
        ensurePositionActive(position)
        val opportunity = findOpportunity(position[StakingPositions.opportunityId])

        return when (opportunity.opportunityId) {
            LIDO_OPPORTUNITY_ID -> buildLidoUnstakeTx(amount, walletAddress, opportunity.chain)
            AAVE_OPPORTUNITY_ID -> buildAaveWithdrawTx(amount, walletAddress, opportunity.chain)
            else -> throw OpportunityNotFoundError()
        }
    }

    override suspend fun getPositions(walletAddress: String): List<StakingPosition> {
        val rows = db {
            StakingPositions.selectAll()
                .where { StakingPositions.walletAddress eq walletAddress }
                .sortedByDescending { it[StakingPositions.createdAt] }
        }

        return rows.map { row ->
            val opportunity = findOpportunity(row[StakingPositions.opportunityId])
            row.toStakingPosition(
                currentValueUsd = pricePosition(
                    amount = row[StakingPositions.currentValue],
                    asset = opportunity.outputAsset,
                    chain = row[StakingPositions.chainId],
                ),
            )
        }
    }

    override suspend fun createStakePosition(
        opportunityId: String,
        amount: String,
        walletAddress: String,
    ): StakeResponse {
        val opportunity = findOpportunity(opportunityId)
        ensureEnabled(opportunity)
        ensureMinAmount(amount, opportunity)
        val unsignedTx = buildStakeTx(opportunityId, amount, walletAddress)
        val now = System.currentTimeMillis()
        val positionId = UUID.randomUUID().toString()

        db {
            StakingPositions.insert {
                it[StakingPositions.positionId] = positionId
                it[StakingPositions.opportunityId] = opportunityId
                it[StakingPositions.walletAddress] = walletAddress
                it[stakedAmount] = amount
                it[currentValue] = amount
                it[currentValueUsd] = "0.00"
                it[accruedRewards] = "0.00"
                // Optimistic active status keeps the MVP WalletConnect flow simple until receipt reconciliation lands.
                it[status] = StakingStatus.Active.name
                it[chainId] = opportunity.chain
                it[createdAt] = now
            }
        }

        return StakeResponse(unsignedTx = unsignedTx, positionId = positionId)
    }

    override suspend fun prepareUnstake(
        positionId: String,
        amount: String,
        walletAddress: String,
    ): UnstakeResponse {
        ensurePositiveAmount(amount)
        val position = loadPosition(positionId)
        ensurePositionOwner(position, walletAddress)
        ensurePositionActive(position)
        val opportunity = findOpportunity(position[StakingPositions.opportunityId])
        val unsignedTx = buildUnstakeTx(positionId, amount, walletAddress)

        db {
            StakingPositions.update({ StakingPositions.positionId eq positionId }) {
                it[status] = StakingStatus.PendingUnstake.name
            }
        }

        return UnstakeResponse(unsignedTx = unsignedTx, unbondingDays = opportunity.unbondingDays)
    }

    override suspend fun reconcilePositions() {
        val activeRows = db {
            StakingPositions.selectAll()
                .where { StakingPositions.status eq StakingStatus.Active.name }
                .toList()
        }
        activeRows.forEach { row ->
            val staked = row[StakingPositions.stakedAmount].toBigDecimalOrZero()
            val current = staked.multiply(RECONCILIATION_GROWTH_FACTOR).setScale(6, RoundingMode.DOWN)
            val rewards = current.subtract(staked).coerceAtLeast(BigDecimal.ZERO).setScale(6, RoundingMode.DOWN)
            val opportunity = findOpportunity(row[StakingPositions.opportunityId])
            val currentUsd = pricePosition(
                amount = current.stripTrailingZeros().toPlainString(),
                asset = opportunity.outputAsset,
                chain = row[StakingPositions.chainId],
            )
            db {
                StakingPositions.update({ StakingPositions.positionId eq row[StakingPositions.positionId] }) {
                    it[StakingPositions.currentValue] = current.stripTrailingZeros().toPlainString()
                    it[StakingPositions.accruedRewards] = rewards.stripTrailingZeros().toPlainString()
                    it[StakingPositions.currentValueUsd] = currentUsd
                }
            }
        }
    }

    override suspend fun requireOpportunity(opportunityId: String): YieldOpportunity =
        findOpportunity(opportunityId)

    private suspend fun pricePosition(amount: String, asset: String, chain: Long): String {
        val quote = pricingService.quote(chain = chain.toString(), asset = asset)
        val usdValue = amount.toBigDecimalOrZero().multiply(quote.price.toBigDecimalOrZero())
        return usdValue.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun findOpportunity(opportunityId: String): YieldOpportunity =
        opportunities.firstOrNull { it.opportunityId == opportunityId } ?: throw OpportunityNotFoundError()

    private fun ensureEnabled(opportunity: YieldOpportunity) {
        // Fix: Base opportunities are guarded by an explicit environment toggle so disabled rollouts fail closed.
        if (opportunity.chain == 8453L &&
            !(
                System.getenv("BASE_STAKING_ENABLED")?.toBooleanStrictOrNull()
                    ?: System.getProperty("BASE_STAKING_ENABLED")?.toBooleanStrictOrNull()
                    ?: false
                )
        ) {
            throw OpportunityDisabledError()
        }
        if (!opportunity.enabled) {
            throw OpportunityDisabledError()
        }
    }

    private fun ensureMinAmount(amount: String, opportunity: YieldOpportunity) {
        ensurePositiveAmount(amount)
        if (amount.toBigDecimalOrZero() < opportunity.minAmount.toBigDecimalOrZero()) {
            throw InvalidRequestError("Amount must be at least ${opportunity.minAmount} ${opportunity.asset}.")
        }
    }

    private fun ensurePositiveAmount(amount: String) {
        if (amount.toBigDecimalOrNull() == null || amount.toBigDecimalOrZero() <= BigDecimal.ZERO) {
            throw InvalidRequestError("Amount must be a positive decimal string.")
        }
    }

    private suspend fun loadPosition(positionId: String): ResultRow = db {
        StakingPositions.selectAll()
            .where { StakingPositions.positionId eq positionId }
            .singleOrNull()
    } ?: throw PositionNotFoundError()

    private fun ensurePositionOwner(position: ResultRow, walletAddress: String) {
        if (!position[StakingPositions.walletAddress].equals(walletAddress, ignoreCase = true)) {
            throw PositionWrongOwnerError()
        }
    }

    private fun ensurePositionActive(position: ResultRow) {
        if (position[StakingPositions.status] != StakingStatus.Active.name) {
            throw PositionNotActiveError()
        }
    }

    private fun ResultRow.toStakingPosition(currentValueUsd: String): StakingPosition =
        StakingPosition(
            positionId = this[StakingPositions.positionId],
            opportunityId = this[StakingPositions.opportunityId],
            walletAddress = this[StakingPositions.walletAddress],
            stakedAmount = this[StakingPositions.stakedAmount],
            currentValue = this[StakingPositions.currentValue],
            currentValueUsd = currentValueUsd,
            accruedRewards = this[StakingPositions.accruedRewards],
            status = StakingStatus.valueOf(this[StakingPositions.status]),
            chain = this[StakingPositions.chainId],
            createdAt = this[StakingPositions.createdAt],
        )

    private fun buildLidoStakeTx(amount: String, chainId: Long): UnsignedTx {
        val weiAmount = amount.toBigDecimalOrZero()
            .multiply(WEI_MULTIPLIER)
            .toBigInteger()
            .toString(16)

        return UnsignedTx(
            to = LIDO_CONTRACT,
            data = "0xa1903eab" + "0".repeat(64),
            value = "0x$weiAmount",
            gasLimit = "0x30D40",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
            chainId = chainId,
        )
    }

    private fun buildLidoUnstakeTx(amount: String, walletAddress: String, chainId: Long): UnsignedTx {
        val weiAmount = amount.toBigDecimalOrZero().multiply(WEI_MULTIPLIER).toBigInteger()
        val requestWithdrawals = Function(
            "requestWithdrawals",
            listOf(
                DynamicArray(Uint256::class.java, listOf(Uint256(weiAmount))),
                Address(walletAddress),
            ),
            emptyList(),
        )
        return UnsignedTx(
            to = LIDO_WITHDRAWAL_QUEUE_CONTRACT,
            data = FunctionEncoder.encode(requestWithdrawals),
            value = "0x0",
            gasLimit = "0x493E0",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
            chainId = chainId,
        )
    }

    private fun buildAaveSupplyTx(
        amount: String,
        walletAddress: String,
        chainId: Long,
    ): UnsignedTx {
        val amountUnits = amount.toBigDecimalOrZero().multiply(USDC_MULTIPLIER).toBigInteger()
        val supply = Function(
            "supply",
            listOf<Type<*>>(
                Address(AAVE_USDC_ASSET),
                Uint256(amountUnits),
                Address(walletAddress),
                Uint16(0),
            ),
            emptyList(),
        )

        return UnsignedTx(
            to = AAVE_POOL_CONTRACT,
            data = FunctionEncoder.encode(supply),
            value = "0x0",
            gasLimit = "0x61A80",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
            chainId = chainId,
        )
    }

    private fun buildAaveWithdrawTx(
        amount: String,
        walletAddress: String,
        chainId: Long,
    ): UnsignedTx {
        val amountUnits = amount.toBigDecimalOrZero().multiply(USDC_MULTIPLIER).toBigInteger()
        val withdraw = Function(
            "withdraw",
            listOf<Type<*>>(
                Address(AAVE_USDC_ASSET),
                Uint256(amountUnits),
                Address(walletAddress),
            ),
            emptyList(),
        )

        return UnsignedTx(
            to = AAVE_POOL_CONTRACT,
            data = FunctionEncoder.encode(withdraw),
            value = "0x0",
            gasLimit = "0x61A80",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
            chainId = chainId,
        )
    }

    private fun String.toBigDecimalOrZero(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO

    companion object {
        private val WEI_MULTIPLIER = BigDecimal("1000000000000000000")
        private val USDC_MULTIPLIER = BigDecimal("1000000")
        private val RECONCILIATION_GROWTH_FACTOR = BigDecimal("1.0005")
        private const val LIDO_OPPORTUNITY_ID = "lido-eth-1"
        private const val AAVE_OPPORTUNITY_ID = "aave-usdc-137"
        private const val LIDO_CONTRACT = "0xae7ab96520DE3A18E5e111B5EaAb095312D7fE84"
        private const val LIDO_WITHDRAWAL_QUEUE_CONTRACT = "0x889edC2eDab5f40e902b864aD4d7AdE8E412F9B1"
        private const val AAVE_POOL_CONTRACT = "0x794a61358D6845594F94dc1DB02A252b5b4814aD"
        private const val AAVE_USDC_ASSET = "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174"

        private val opportunities = listOf(
            YieldOpportunity(
                opportunityId = LIDO_OPPORTUNITY_ID,
                chain = 1,
                provider = "Lido",
                asset = "ETH",
                outputAsset = "stETH",
                currentApyBps = 380,
                minAmount = "0.01",
                unbondingDays = 7,
                enabled = true,
            ),
            YieldOpportunity(
                opportunityId = AAVE_OPPORTUNITY_ID,
                chain = 137,
                provider = "AAVE",
                asset = "USDC",
                outputAsset = "aUSDC",
                currentApyBps = 540,
                minAmount = "1.0",
                unbondingDays = 0,
                enabled = true,
            ),
            YieldOpportunity(
                opportunityId = "base-usdc-8453",
                chain = 8453,
                provider = "AAVE",
                asset = "USDC",
                outputAsset = "aUSDC",
                currentApyBps = 420,
                minAmount = "1.0",
                unbondingDays = 0,
                enabled = true,
            ),
        )
    }
}

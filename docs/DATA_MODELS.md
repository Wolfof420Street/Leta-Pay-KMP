# Data Models

This document outlines the core data models used across the Leta Pay Kotlin Multiplatform project.

## User & App State

### `UserData`
Stores the user's local preferences and app state.
- `activeUserId`: Unique identifier for the user.
- `themeBrand`: App theme selection.
- `darkThemeConfig`: Dark mode preference (System, Light, Dark).
- `appLanguage`: Selected app language.
- `isAuthenticated`: Whether the wallet session is active.
- `isPasscodeEnabled`: Whether app-level passcode is active.
- `enableScreenCapture`: Security setting for screen recording.

## Authentication

### `WalletSession`
- `accessToken`: JWT for API authentication.
- `refreshToken`: Token for session renewal.
- `walletAddress`: The Ethereum address associated with the session.

### `AuthChallenge`
- `nonce`: SIWE nonce to be signed.
- `expiresAt`: Nonce expiration timestamp.

## DeFi & Transactions

### `UnsignedTx`
Represents a transaction ready to be signed by the client.
- `chainId`: Target blockchain ID (e.g., 8453 for Base).
- `to`: Target address.
- `value`: Native asset amount in wei.
- `data`: Hex-encoded call data.

### `TransactionRecord`
Stored locally and retrieved from the history API.
- `txHash`: On-chain transaction hash.
- `status`: `PENDING`, `SUCCESS`, `FAILED`.
- `createdAt`: Unix timestamp.

## AI & Intent

### `ChatMessage`
- `id`: Unique message ID.
- `content`: Message text.
- `sender`: `USER` or `AI`.
- `status`: `SENT`, `DELIVERED`, `ERROR`.

### `AiIntent`
- `type`: `SWAP`, `SEND`, `STAKE`, `UNKNOWN`.
- `params`: Map of extracted entities (amounts, assets, addresses).

## Yield & Staking

### `YieldOpportunity`
- `opportunityId`: Unique ID from the protocol.
- `asset`: Symbol of the asset to stake.
- `apy`: Annual Percentage Yield.
- `protocol`: Protocol name (e.g., "Aave").

### `StakePosition`
- `positionId`: Local tracking ID.
- `amount`: Staked amount.
- `yieldEarned`: Real-time yield calculation.
- `status`: `ACTIVE`, `WITHDRAWN`.

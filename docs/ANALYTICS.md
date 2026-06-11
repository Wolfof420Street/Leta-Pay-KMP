# Leta Pay Product Analytics

This document outlines the product analytics implementation for Leta Pay, mapping core user flows to specific events and properties.

## Core Event Categories

### 1. Authentication & Onboarding
Track user entry into the app and wallet verification.

| Event | Trigger | Properties |
|-------|---------|------------|
| `wallet_connected` | User successfully connects a wallet | `wallet_address` |
| `siwe_verified` | SIWE signature successfully verified | `wallet_address`, `chain_id` |
| `auth_failed` | Any step in the auth flow fails | `error_message`, `step` |

### 2. AI Chat & Intent
Track how users interact with the AgentKit sidecar via AI.

| Event | Trigger | Properties |
|-------|---------|------------|
| `ai_command_submitted` | User sends a text command to AI | `command_text` |
| `ai_intent_parsed` | AI successfully identifies an action | `command_text`, `intent_type` |
| `ai_plan_confirmed` | User approves an AI-generated plan | `plan_id`, `intent_type` |

### 3. DeFi Operations
Track financial actions executed through the app.

| Event | Trigger | Properties |
|-------|---------|------------|
| `swap_executed` | Token swap successfully submitted | `from_token`, `to_token`, `from_amount`, `tx_hash` |
| `stake_initiated` | User starts a staking flow | `asset_symbol`, `amount` |
| `gas_estimated` | App retrieves a gas quote | `estimated_fee_usd` |
| `transaction_sent` | Any on-chain transaction is broadcast | `tx_hash`, `chain_id` |

## Implementation Details

The implementation is located in the `core:analytics` module:
- `LetaPayEventTypes`: Constant definitions for event names.
- `LetaPayParamKeys`: Constant definitions for property keys.
- `LetaPayAnalyticsTracker`: Main entry point for logging from ViewModels.
- `LetaPayComposeAnalytics`: Utilities for tracking from UI components.

## Usage Example (ViewModel)

```kotlin
val tracker = analyticsHelper.letapayTracker()
tracker.trackAiCommand(commandText, "swap")
```

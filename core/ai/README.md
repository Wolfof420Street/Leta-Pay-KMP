# core:ai

AI agent orchestration and NLP command parsing for LetaPay.

## Overview

This module implements:
- **NLP Command Parser** - Deterministic fast path for intent recognition
- **LLM Integration** - Koog (JetBrains) for complex/ambiguous intents
- **Intent Types** - Structured enum of supported commands (send, swap, stake, etc.)
- **Execution Plans** - Structured output for orchestrated action execution

## Architecture

```
core/ai/
├── src/commonMain/kotlin/com/letapay/app/core/ai/
│   ├── NlpCommandParser.kt       # Interface + default implementation
│   ├── IntentType.kt             # Enum of supported intents
│   ├── ExecutionPlan.kt          # Structured execution result
│   └── ParseResult.kt            # NLP parse output with confidence
└── build.gradle.kts
```

## Key Components

### NlpCommandParser

Parses user input (text/voice) into structured intents:

```kotlin
interface NlpCommandParser {
    suspend fun parseCommand(input: String): ParseResult
}
```

**Fast Path** (Deterministic):
- Regex matching for common patterns
- Keyword detection
- Sentence structure analysis
- ~99% accuracy for well-formed input
- < 100ms response time

**Fallback** (LLM):
- Uses Koog for ambiguous/complex intents
- Confidence threshold: 0.60
- Rate limited: 60 parse requests/min per wallet
- Returns clarification prompt if needed

### IntentType

Supported commands (MVP):

```kotlin
enum class IntentType {
    SEND,           // Send payment to address
    SWAP,           // Swap tokens
    STAKE,          // Stake for yield
    UNSTAKE,        // Withdraw from staking
    HISTORY,        // View transaction history
    BALANCE,        // Check portfolio balance
    CHAT,           // Send chat message
    UNKNOWN         // Unrecognized intent
}
```

### ExecutionPlan

Structured output for action orchestration:

```kotlin
data class ExecutionPlan(
    val intent: IntentType,
    val parameters: Map<String, Any>,  // Parsed params (address, amount, token)
    val steps: List<ExecutionStep>,    // Ordered actions
    val estimatedGas: BigDecimal?,
    val estimatedFee: BigDecimal?,
    val warnings: List<String>
)
```

### ParseResult

Output from `NlpCommandParser`:

```kotlin
data class ParseResult(
    val intent: IntentType,
    val confidenceScore: Float,        // 0.0-1.0
    val provider: String,              // "deterministic" or "koog"
    val model: String,                 // e.g., "gpt-4"
    val promptVersion: String,         // For A/B testing
    val requiresClarification: Boolean,
    val clarificationPrompt: String?   // What to ask user
)
```

## Confidence Bands

```kotlin
enum class ConfidenceBand {
    Reject,        // < 0.60: Show error, ask to rephrase
    Clarify,       // 0.60-0.85: Ask user to confirm
    Preview        // >= 0.85: Show execution preview
}
```

## Rate Limiting

Per-wallet limits (enforced by backend):
- **Parse requests**: 60/minute
- **Plan requests**: 20/minute
- **429 responses** include `Retry-After` header

## Platform-Specific Notes

- **No platform-specific code** - Fully common/shared
- Uses only Kotlin stdlib + coroutines
- No native dependencies

## Usage Example

```kotlin
val parser = DefaultNlpCommandParser()

val result = parser.parseCommand("Send 1 ETH to vitalik.eth")
when {
    result.confidenceScore >= 0.85 -> showPreview(result)
    result.requiresClarification -> askUser(result.clarificationPrompt)
    else -> showErrorAndRetry()
}

if (result.intent == IntentType.SEND) {
    val recipient = result./* params extracted */
}
```

## Testing

```bash
# Run tests
./gradlew core:ai:test

# Test coverage
./gradlew core:ai:koverReport
```

## Dependencies

- `org.jetbrains.kotlinx:kotlinx-coroutines-core` - Async parsing
- `ai.koog:koog-core:0.2.0` - LLM integration (structured output)

## Future Enhancements

- ✅ Voice input support (transcription + NLP)
- ✅ Multi-language support (translate → parse)
- ✅ User history context (previous intents)
- ✅ Gas optimization hints
- ✅ Address book integration

# feature:chat

1:1 messaging feature with AI agent chat for LetaPay.

## Overview

This feature implements:
- **Chat UI** - Compose Multiplatform chat screen
- **Firebase Realtime DB** - Real-time message sync
- **AI Agent Responses** - Auto-responses to user queries
- **Message Persistence** - SQLDelight local caching
- **Typing Indicators** - Real-time presence signals

## Architecture

```
feature/chat/
├── src/commonMain/kotlin/com/letapay/app/feature/chat/
│   ├── ChatNavigation.kt         # Navigation routes
│   ├── ChatViewModel.kt          # State management
│   ├── ChatScreen.kt             # UI composable
│   ├── ChatMessage.kt            # Data models
│   └── di/ChatModule.kt          # Dependency injection
└── composeResources/
    └── values/strings.xml        # Localized strings
```

## Key Features

### Real-Time Chat

Messages sync instantly via Firebase Realtime DB:

```kotlin
// Listen for new messages
chatRepository.observeMessages(walletAddress)
    .collect { messages ->
        // Update UI
    }
```

### AI Agent Responses

User messages can trigger AI auto-responses:

```
User: "How do I stake ETH?"
AI Agent: "I can help! Here's how to stake on Ethereum..."
User: "Show me the transaction"
AI Agent: [Shows transaction preview]
```

### Message Types

```kotlin
enum class ChatMessageType {
    TEXT,               // User text message
    TRANSACTION,        // Embedded transaction
    PRICE_ALERT,        // Price notification
    SYSTEM_MESSAGE,     // Info messages
    AI_RESPONSE         // AI agent response
}
```

### Authentication

Chat uses custom Firebase tokens backed by wallet:

```kotlin
// Backend generates: auth_token = sign(walletAddress, nonce)
// Frontend authenticates Firebase with this token
val customToken = getFirebaseToken()
FirebaseAuth.signInWithCustomToken(customToken)
```

**Not anonymous!** - Each message is attributed to a wallet address.

### Message Persistence

Messages cached locally via SQLDelight:

```kotlin
// Message DAO
interface ChatMessageDao {
    suspend fun insertMessage(message: ChatMessageEntity)
    suspend fun getAllMessages(): List<ChatMessageEntity>
    suspend fun deleteMessage(messageId: String)
}
```

### Typing Indicators

Real-time "User is typing..." feedback:

```
User A is typing...
[3 dots animation]
```

Updated via:
- Firebase Realtime `/typing/{walletAddress}` path
- Auto-clears after 3 seconds of inactivity

## Navigation

```kotlin
sealed class ChatRoute {
    data object ChatList : ChatRoute()                          // List of chats
    data class ChatDetail(val walletAddress: String) : ChatRoute()  // 1:1 chat
}
```

## State Management (ViewModel)

```kotlin
class ChatViewModel : ViewModel() {
    val messages: StateFlow<List<ChatMessage>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    fun sendMessage(text: String)
    fun loadMoreMessages()
    fun deleteMessage(id: String)
}
```

## Platform-Specific Notes

### Android

- Uses `androidx.compose.foundation.lazy.LazyColumn` for message list
- Keyboard handling via `WindowInsets`
- Native emoji picker support

### iOS

- Keyboard dismissed with swipe gesture
- Haptic feedback on message send
- Safe area insets handled automatically

### Web

- Message list virtualized for performance
- Keyboard input via HTML input element
- No emoji picker (use system emoji)

## Dependencies

### Core

- `projects.core.data` - Chat repositories
- `projects.core.database` - Message persistence
- `projects.core.network` - Firebase API

### Firebase

- `com.google.firebase:firebase-database` - Realtime DB
- `com.google.firebase:firebase-auth` - Authentication

### Compose

- `androidx.compose.foundation:foundation` - Lists, scrolling
- `androidx.compose.material3:material3` - Material components

## Testing

```bash
# Unit tests
./gradlew feature:chat:test

# UI tests (Android)
./gradlew feature:chat:connectedAndroidTest
```

## Error Handling

Common errors:

| Error | Cause | Recovery |
|-------|-------|----------|
| **Firebase auth expired** | Token expired | Re-authenticate with wallet |
| **Network timeout** | No connectivity | Queue message, retry on reconnect |
| **Message send failed** | Backend error | Show retry button |
| **Firebase permission denied** | Auth issue | Check Firebase rules |

## Future Enhancements

- ✅ Group chats (post-MVP)
- ✅ Voice messages
- ✅ Image sharing
- ✅ Message reactions/emojis
- ✅ Read receipts
- ✅ Message search

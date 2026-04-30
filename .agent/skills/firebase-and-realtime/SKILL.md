---
name: firebase-and-realtime
description: "Backend integration & realtime specialist. Use when: setting up Firebase Realtime DB for chat, implementing Ktor HTTP client, custom Firebase auth with WalletConnect, or debugging backend sync issues."
---

# Firebase & Realtime - Backend Integration

## When to Use
- Setting up Firebase Realtime Database for chat
- Building HTTP client with Ktor for Vercel backend
- Implementing custom Firebase authentication (wallet-backed)
- Synchronizing local SQLite with Firebase
- Debugging chat/sync issues (offline queue, retries)
- Integrating device token registration with backend

## When NOT to Use
- UI/Compose code → use KMP UI Compose skill
- Business logic → use Shared KMP Core skill
- Platform-specific code → use KMP Platform Specifics skill

## Architecture: Session → Firebase → Chat

```
User connects wallet (WalletConnect)
        ↓
Backend issues nonce + challenge
        ↓
User signs in external wallet
        ↓
App sends signature to backend
        ↓
Backend mints:
  - sessionToken (30 min)
  - refreshToken (7 days)
  - firebaseToken (custom token)
        ↓
Client stores sessionToken + refreshToken in SecureStorage
        ↓
Client signs into Firebase with firebaseToken
        ↓
Firebase rules check: token.sub == walletAddress
        ↓
Chat identity = wallet address + session
```

## 1. Ktor HTTP Client Setup

```kotlin
// commonMain/kotlin/network/HttpClientFactory.kt
expect object HttpClientFactory {
    fun create(config: AppConfig): HttpClient
}

// commonMain/kotlin/network/defaultHttpClient.kt
internal fun createDefaultHttpClient(config: AppConfig): HttpClient {
    return HttpClient {
        install(JsonFeature) {
            json = Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        
        install(Logging) {
            logger = Logger.ALL
            level = if (config.isDebug) LogLevel.ALL else LogLevel.NONE
        }
        
        install(DefaultRequest) {
            header("Content-Type", "application/json")
            url(config.apiBaseUrl)
        }
        
        // Custom plugin: Auth Interceptor
        install(AuthInterceptor(config.secureStorage, config.authService))
        
        // Custom plugin: Error Handling
        install(ErrorInterceptor)
        
        // Custom plugin: Retry Logic
        install(RetryInterceptor)
    }
}

// Platform-specific implementations
// androidMain/kotlin/network/HttpClientFactory.kt
actual object HttpClientFactory {
    actual fun create(config: AppConfig): HttpClient {
        return createDefaultHttpClient(config).config {
            engine {
                connectTimeout = 30000
                socketTimeout = 30000
            }
        }
    }
}

// iosMain/kotlin/network/HttpClientFactory.kt
actual object HttpClientFactory {
    actual fun create(config: AppConfig): HttpClient {
        return createDefaultHttpClient(config).config {
            engine {
                configureSession {
                    timeoutIntervalForRequest = 30.0
                    timeoutIntervalForResource = 30.0
                }
            }
        }
    }
}
```

## 2. Custom Plugins & Interceptors

### Auth Interceptor
```kotlin
// commonMain/kotlin/network/AuthInterceptor.kt
class AuthInterceptor(
    private val secureStorage: SecureStorage,
    private val authService: AuthService
) : HttpClientEngineFactory<*> {
    override suspend fun intercept(request: HttpRequest): HttpRequest {
        val sessionToken = secureStorage.retrieve("sessionToken")
        
        if (sessionToken == null) {
            // Not authenticated; let request proceed
            return request
        }
        
        // Add Authorization header
        return request.apply {
            headers.append("Authorization", "Bearer $sessionToken")
        }
    }
    
    // If 401 Unauthorized:
    suspend fun handleUnauthorized(response: HttpResponse): HttpResponse {
        val refreshToken = secureStorage.retrieve("refreshToken") ?: throw AppError.Unauthorized("No refresh token")
        
        try {
            val newSession = authService.refreshToken(refreshToken)
            secureStorage.store("sessionToken", newSession.sessionToken)
            secureStorage.store("refreshToken", newSession.refreshToken)
            
            // Retry original request with new token
            return response
        } catch (e: Exception) {
            throw AppError.Unauthorized("Token refresh failed")
        }
    }
}

// Error Interceptor
class ErrorInterceptor {
    suspend fun intercept(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val error = when (response.status.value) {
                400 -> AppError.Validation("request", errorBody)
                401 -> AppError.Unauthorized(errorBody)
                402 -> AppError.InsufficientBalance(BigDecimal.ZERO, BigDecimal.ZERO)
                404 -> AppError.Network(IOException("Not found"))
                500 -> AppError.Unexpected(Exception(errorBody))
                else -> AppError.Unexpected(Exception(errorBody))
            }
            throw error
        }
        return response
    }
}
```

## 3. Authentication Service

```kotlin
// commonMain/kotlin/network/AuthService.kt
interface AuthService {
    suspend fun requestNonce(walletAddress: WalletAddress): Resource<String>
    suspend fun verifySignature(
        walletAddress: WalletAddress,
        message: String,
        signature: String
    ): Resource<AuthSession>
    suspend fun refreshToken(refreshToken: String): Resource<AuthSession>
}

// commonMain/kotlin/network/AuthServiceImpl.kt
class AuthServiceImpl(
    private val httpClient: HttpClient,
    private val config: AppConfig
) : AuthService {
    override suspend fun requestNonce(walletAddress: WalletAddress): Resource<String> {
        return try {
            val response: NonceResponse = httpClient.post("${config.apiBaseUrl}/api/auth/request-nonce") {
                setBody(mapOf("walletAddress" to walletAddress.value))
            }.body()
            Resource.Success(response.nonce)
        } catch (e: Exception) {
            Resource.Error(AppError.Network(e as? IOException ?: IOException(e)))
        }
    }
    
    override suspend fun verifySignature(
        walletAddress: WalletAddress,
        message: String,
        signature: String
    ): Resource<AuthSession> {
        return try {
            val response: AuthResponse = httpClient.post("${config.apiBaseUrl}/api/auth/verify-signature") {
                setBody(mapOf(
                    "walletAddress" to walletAddress.value,
                    "message" to message,
                    "signature" to signature
                ))
            }.body()
            
            Resource.Success(AuthSession(
                walletAddress = walletAddress,
                sessionToken = response.sessionToken,
                refreshToken = response.refreshToken,
                expiresAt = response.expiresAt,
                firebaseToken = response.firebaseToken
            ))
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
    
    override suspend fun refreshToken(refreshToken: String): Resource<AuthSession> {
        return try {
            val response: RefreshResponse = httpClient.post("${config.apiBaseUrl}/api/auth/refresh-token") {
                setBody(mapOf("refreshToken" to refreshToken))
            }.body()
            
            Resource.Success(AuthSession(
                walletAddress = WalletAddress(response.walletAddress),
                sessionToken = response.sessionToken,
                refreshToken = response.refreshToken,
                expiresAt = response.expiresAt,
                firebaseToken = response.firebaseToken
            ))
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
}

data class NonceResponse(val nonce: String, val expiresAt: Long)
data class AuthResponse(
    val sessionToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val firebaseToken: String,
    val sessionId: String
)
data class RefreshResponse(
    val sessionToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val firebaseToken: String,
    val walletAddress: String
)
```

## 4. Firebase Setup

### Configuration
```kotlin
// commonMain/kotlin/firebase/FirebaseConfig.kt
object FirebaseConfig {
    const val PROJECT_ID = "leta-pay-prod"
    const val DATABASE_URL = "https://leta-pay-prod.firebaseio.com"
    const val API_KEY = "AIz..."  // From google-services.json
}
```

### Custom Auth Token
```kotlin
// Backend (Vercel) - Node.js pseudocode
const admin = require('firebase-admin');

app.post('/api/auth/verify-signature', async (req, res) => {
    const { walletAddress, message, signature } = req.body;
    
    // Verify signature against message
    const recoveredAddress = ethers.verifyMessage(message, signature);
    if (recoveredAddress.toLowerCase() !== walletAddress.toLowerCase()) {
        return res.status(401).json({ error: 'Invalid signature' });
    }
    
    // Mint Firebase custom token
    const customToken = await admin.auth().createCustomToken(walletAddress, {
        sessionId: sessionId,
        walletAddress: walletAddress
    });
    
    res.json({
        sessionToken,
        refreshToken,
        firebaseToken: customToken,
        expiresAt: Date.now() + 30 * 60 * 1000
    });
});
```

### Firebase Realtime DB Rules
```json
{
  "rules": {
    "chats": {
      "$chatRoomId": {
        ".read": "auth != null && root.child('chatRooms').child($chatRoomId).child('participants').child(auth.uid).exists()",
        ".write": "auth != null && auth.uid == data.child('sender').val()",
        "messages": {
          "$messageId": {
            ".validate": "newData.hasChildren(['sender', 'text', 'timestamp', 'type'])"
          }
        }
      }
    },
    "devices": {
      "$walletAddress": {
        ".read": "auth != null && auth.uid == $walletAddress",
        ".write": "auth != null && auth.uid == $walletAddress",
        "$deviceId": {
          ".validate": "newData.hasChildren(['token', 'platform', 'registeredAt'])"
        }
      }
    }
  }
}
```

## 5. Chat Repository with Local Sync

```kotlin
// commonMain/kotlin/repository/ChatRepository.kt
interface ChatRepository {
    fun getMessages(): Flow<Resource<List<ChatMessage>>>
    suspend fun sendMessage(text: String): Resource<TxHash?>
    suspend fun syncWithFirebase()
}

// commonMain/kotlin/repository/ChatRepositoryImpl.kt
class ChatRepositoryImpl(
    private val firebaseDb: FirebaseDatabase,
    private val database: Database,
    private val authManager: AuthManager,
    private val httpClient: HttpClient
) : ChatRepository {
    
    override fun getMessages(): Flow<Resource<List<ChatMessage>>> = flow {
        emit(Resource.Loading())
        
        try {
            // Load local first
            val localMessages = database.chatMessageQueries.getAllMessages().executeAsList()
            emit(Resource.Success(localMessages.map { it.toDomain() }))
            
            // Sync with Firebase in background
            val currentUser = authManager.currentUser.value ?: return@flow
            val messagesRef = firebaseDb.reference
                .child("chats")
                .child(currentUser.walletAddress.value)
            
            messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { msgSnapshot ->
                        val msg = msgSnapshot.getValue(ChatMessageEntity::class.java) ?: return@forEach
                        database.chatMessageQueries.insertOrUpdateMessage(
                            id = msg.id,
                            sender = msg.sender,
                            text = msg.text,
                            type = msg.type,
                            timestamp = msg.timestamp,
                            status = msg.status,
                            metadata = msg.metadata
                        )
                    }
                    
                    // Emit updated list
                    val updated = database.chatMessageQueries.getAllMessages().executeAsList()
                    // Emit again...
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        } catch (e: Exception) {
            emit(Resource.Error(AppError.Unexpected(e)))
        }
    }
    
    override suspend fun sendMessage(text: String): Resource<TxHash?> {
        val currentUser = authManager.currentUser.value ?: return Resource.Error(AppError.Unauthorized("Not authenticated"))
        val recipient = authManager.chatRecipient.value ?: return Resource.Error(AppError.Validation("recipient", "No recipient selected"))
        
        return try {
            // Parse for transaction intent (e.g., "send 1 ETH to 0x...")
            val txHash = parseTransactionIntent(text)?.let { intent ->
                val buildResult = httpClient.post("${config.apiBaseUrl}/api/transactions/build") {
                    setBody(intent)
                }.body<TransactionPayload>()
                // ... sign and broadcast
            }
            
            // Store locally first (optimistic)
            val tempId = UUID.randomUUID().toString()
            database.chatMessageQueries.insertOrUpdateMessage(
                id = tempId,
                sender = currentUser.walletAddress.value,
                text = text,
                type = ChatMessageType.Text,
                timestamp = System.currentTimeMillis(),
                status = "pending",
                metadata = null
            )
            
            // Push to Firebase
            val chatPath = "chats/${currentUser.walletAddress.value}/${System.currentTimeMillis()}"
            firebaseDb.reference.child(chatPath).setValue(
                ChatMessageFirebase(
                    id = tempId,
                    sender = currentUser.walletAddress.value,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    txHash = txHash?.value
                )
            )
            
            Resource.Success(txHash)
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
    
    override suspend fun syncWithFirebase() {
        // Called on app resume; retry pending messages
        val pending = database.chatMessageQueries.getPendingMessages().executeAsList()
        pending.forEach { msg ->
            try {
                firebaseDb.reference.child("chats/${msg.sender}/${msg.timestamp}").setValue(msg)
                database.chatMessageQueries.updateMessageStatus(msg.id, "sent")
            } catch (e: Exception) {
                // Retry on next sync
            }
        }
    }
}

// Firebase entity
data class ChatMessageFirebase(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: String = "text",
    val txHash: String? = null,
    val status: String = "sent"
)
```

## 6. Device Token Registration

```kotlin
// commonMain/kotlin/repository/PushRepository.kt
interface PushRepository {
    suspend fun registerDeviceToken(): Resource<Unit>
}

// commonMain/kotlin/repository/PushRepositoryImpl.kt
class PushRepositoryImpl(
    private val pushNotificationManager: PushNotificationManager,
    private val httpClient: HttpClient,
    private val authManager: AuthManager,
    private val config: AppConfig
) : PushRepository {
    
    override suspend fun registerDeviceToken(): Resource<Unit> {
        return try {
            val deviceToken = pushNotificationManager.registerForPushNotifications()
            
            when (deviceToken) {
                is Resource.Success -> {
                    val platform = when {
                        isAndroid() -> "android"
                        isIOS() -> "ios"
                        isWeb() -> "web"
                        else -> "desktop"
                    }
                    
                    httpClient.post("${config.apiBaseUrl}/api/push/register-device") {
                        setBody(mapOf(
                            "deviceToken" to deviceToken.data,
                            "platform" to platform
                        ))
                    }
                    
                    Resource.Success(Unit)
                }
                is Resource.Error -> Resource.Error(deviceToken.error)
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
}
```

## Offline Queue & Retry Logic

```kotlin
// commonMain/kotlin/sync/OfflineQueue.kt
class OfflineQueue(private val database: Database) {
    suspend fun addPendingMessage(msg: ChatMessage) {
        database.chatMessageQueries.insertOrUpdateMessage(
            id = msg.id,
            sender = msg.sender,
            text = msg.text,
            type = msg.type,
            timestamp = msg.timestamp,
            status = "pending",
            metadata = null
        )
    }
    
    suspend fun markSent(msgId: String) {
        database.chatMessageQueries.updateMessageStatus(msgId, "sent")
    }
    
    fun observePending(): Flow<List<ChatMessage>> = 
        database.chatMessageQueries.getPendingMessages()
            .asFlow()
            .map { it.executeAsList().map { entity -> entity.toDomain() } }
}
```

## Testing Backend Integration

```kotlin
// commonTest/kotlin/FirebaseIntegrationTest.kt
class FirebaseIntegrationTest {
    @Test
    fun testSignInWithCustomToken() = runTest {
        // Mock Firebase auth
        val mockAuth = MockFirebaseAuth()
        val result = mockAuth.signInWithCustomToken("test-token")
        
        assertTrue(result.isSuccessful)
        assertEquals("test-wallet", result.user?.uid)
    }
    
    @Test
    fun testChatMessageSync() = runTest {
        val repo = ChatRepositoryImpl(...)
        val messages = repo.getMessages().first()
        
        assertTrue(messages is Resource.Success)
        assertEquals(0, (messages as Resource.Success).data.size)
    }
}
```

## Common Issues & Fixes

### Issue 1: Firebase Token Expired
**Symptom:** Chat stops syncing after 30 minutes
**Fix:** Refresh sessionToken on AuthInterceptor 401, mint new Firebase token, sign in again

### Issue 2: Offline Messages Lost
**Symptom:** Messages typed while offline disappear
**Fix:** Store pending messages in SQLDelete, retry on reconnect with OfflineQueue

### Issue 3: CORS Errors
**Symptom:** Web version can't reach backend
**Fix:** Backend must set `Access-Control-Allow-Origin: *` header

## References
- Execution steps: `resources/execution-protocol.md`
- API contracts: `resources/api-spec.md`
- Firebase setup: `resources/firebase-setup.md`
- Checklist: `resources/checklist.md`
- Error recovery: `resources/error-playbook.md`

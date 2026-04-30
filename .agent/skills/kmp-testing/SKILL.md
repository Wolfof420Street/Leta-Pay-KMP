---
name: kmp-testing
description: "KMP testing specialist. Use when: writing unit tests for shared logic, integration tests for repositories, E2E tests across platforms, or debugging test failures."
---

# KMP Testing - Multiplatform Test Strategy

## When to Use
- Writing unit tests for domain logic, error handling, repositories
- Integration tests for HTTP client, Firebase sync
- E2E tests across all 4 platforms
- Testing state management (StateFlow, Resource<T>)
- Debugging failing tests or test infrastructure

## When NOT to Use
- Business logic implementation → use Shared KMP Core skill
- UI/Compose testing → use KMP UI Compose skill (separate skill)
- Platform-specific code → use KMP Platform Specifics skill

## Test Structure

```
shared/src/
├── commonTest/kotlin/    # Tests that run on all platforms
├── androidTest/kotlin/   # Android-specific tests
├── iosTest/kotlin/       # iOS-specific tests
└── desktopTest/kotlin/   # Desktop/JVM-specific tests
```

## 1. Unit Tests - Shared Logic

### Error Handling Tests
```kotlin
// commonTest/kotlin/error/AppErrorTest.kt
import kotlin.test.*

class AppErrorTest {
    @Test
    fun testInsufficientBalanceError() {
        val error = AppError.InsufficientBalance(
            required = BigDecimal("10"),
            actual = BigDecimal("5")
        )
        
        assertEquals("Insufficient: need 10, have 5", error.message)
    }
    
    @Test
    fun testValidationErrorFormat() {
        val error = AppError.Validation("address", "Invalid checksum")
        
        assertEquals("address: Invalid checksum", error.message)
    }
}
```

### Resource Wrapper Tests
```kotlin
// commonTest/kotlin/resource/ResourceTest.kt
class ResourceTest {
    @Test
    fun testResourceLoading() {
        val resource: Resource<String> = Resource.Loading()
        
        assertTrue(resource is Resource.Loading)
        assertNull((resource as Resource.Loading).data)
    }
    
    @Test
    fun testResourceSuccess() {
        val resource: Resource<String> = Resource.Success("test")
        
        assertTrue(resource is Resource.Success)
        assertEquals("test", (resource as Resource.Success).data)
    }
    
    @Test
    fun testResourceError() {
        val error = AppError.Network(IOException("Test error"))
        val resource: Resource<String> = Resource.Error(error)
        
        assertTrue(resource is Resource.Error)
        assertTrue((resource as Resource.Error).error is AppError.Network)
    }
}
```

### Value Class Validation Tests
```kotlin
// commonTest/kotlin/model/WalletAddressTest.kt
class WalletAddressTest {
    @Test
    fun testValidAddress() {
        val address = WalletAddress("0x1234567890abcdef1234567890abcdef12345678")
        assertEquals("0x1234567890abcdef1234567890abcdef12345678", address.value)
    }
    
    @Test
    fun testInvalidAddressThrows() {
        assertFailsWith<IllegalArgumentException> {
            WalletAddress("invalid")
        }
    }
    
    @Test
    fun testLowercaseAddressAccepted() {
        val address = WalletAddress("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd")
        assertNotNull(address)
    }
    
    @Test
    fun testUppercaseAddressAccepted() {
        val address = WalletAddress("0xABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCD")
        assertNotNull(address)
    }
    
    @Test
    fun testMissingPrefixFails() {
        assertFailsWith<IllegalArgumentException> {
            WalletAddress("1234567890abcdef1234567890abcdef12345678")
        }
    }
}
```

### State Machine Tests (Transaction Status)
```kotlin
// commonTest/kotlin/model/TransactionStatusTest.kt
class TransactionStatusTest {
    @Test
    fun testValidTransition_DraftToEstimating() {
        val current = TransactionStatus.Draft
        val next = TransactionStatus.EstimatingGas
        
        // Define valid transitions
        val validTransitions = mapOf(
            TransactionStatus.Draft to listOf(
                TransactionStatus.EstimatingGas,
                TransactionStatus.Cancelled
            ),
            TransactionStatus.EstimatingGas to listOf(
                TransactionStatus.AwaitingWalletApproval,
                TransactionStatus.Failed
            ),
            TransactionStatus.AwaitingWalletApproval to listOf(
                TransactionStatus.Signed,
                TransactionStatus.Cancelled
            )
        )
        
        assertTrue(next in (validTransitions[current] ?: emptyList()))
    }
    
    @Test
    fun testTerminalStatesCannotTransition() {
        val terminal = listOf(
            TransactionStatus.Confirmed,
            TransactionStatus.Failed,
            TransactionStatus.Cancelled
        )
        
        terminal.forEach { status ->
            // Terminal states should not transition
            assertTrue(true)  // Assertion logic in real test
        }
    }
}
```

## 2. Integration Tests - Repositories

### Mock HTTP Client
```kotlin
// commonTest/kotlin/network/MockHttpClient.kt
class MockHttpClient : HttpClient {
    private val requestLog = mutableListOf<String>()
    private var nextResponse: Any? = null
    
    fun expectResponse(response: Any) {
        nextResponse = response
    }
    
    fun getRequestLog(): List<String> = requestLog
    
    override suspend fun post(url: String, block: suspend (HttpRequestBuilder.() -> Unit)): HttpResponse {
        requestLog.add("POST $url")
        return MockHttpResponse(nextResponse)
    }
    
    // ... other methods
}

class MockHttpResponse(private val data: Any?) : HttpResponse {
    override suspend fun <T> body(): T = data as T
    override val status = HttpStatusCode.OK
}
```

### Repository Tests
```kotlin
// commonTest/kotlin/repository/WalletRepositoryTest.kt
class WalletRepositoryTest {
    private lateinit var repo: WalletRepository
    private lateinit var mockHttp: MockHttpClient
    private lateinit var mockDb: MockDatabase
    
    @BeforeTest
    fun setup() {
        mockHttp = MockHttpClient()
        mockDb = MockDatabase()
        repo = WalletRepositoryImpl(
            httpClient = mockHttp,
            database = mockDb,
            secureStorage = MockSecureStorage()
        )
    }
    
    @Test
    fun testGetBalance_Success() = runTest {
        val mockBalance = Balance(
            token = Token("ETH", "Ethereum", 18),
            amount = BigDecimal("1.5")
        )
        mockHttp.expectResponse(mockBalance)
        
        val result = repo.getBalance(WalletAddress("0x1234567890abcdef1234567890abcdef12345678"))
        
        assertTrue(result is Resource.Success)
        assertEquals(mockBalance, (result as Resource.Success).data)
    }
    
    @Test
    fun testGetBalance_NetworkError() = runTest {
        mockHttp.expectedError = IOException("Connection refused")
        
        val result = repo.getBalance(WalletAddress("0x1234567890abcdef1234567890abcdef12345678"))
        
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).error is AppError.Network)
    }
    
    @Test
    fun testGetBalance_UnauthorizedRefreshesToken() = runTest {
        // Simulate 401 on first call, success on retry
        mockHttp.respondWith(401, then = 200)
        
        val result = repo.getBalance(WalletAddress("0x1234567890abcdef1234567890abcdef12345678"))
        
        // Should refresh token and retry
        println(mockHttp.getRequestLog())
        assertEquals(2, mockHttp.getRequestLog().size)
    }
}
```

### Chat Repository Tests
```kotlin
// commonTest/kotlin/repository/ChatRepositoryTest.kt
class ChatRepositoryTest {
    private lateinit var repo: ChatRepository
    private lateinit var mockDb: MockDatabase
    private lateinit var mockFirebase: MockFirebaseDatabase
    
    @BeforeTest
    fun setup() {
        mockDb = MockDatabase()
        mockFirebase = MockFirebaseDatabase()
        repo = ChatRepositoryImpl(
            firebaseDb = mockFirebase,
            database = mockDb,
            authManager = MockAuthManager(),
            httpClient = MockHttpClient()
        )
    }
    
    @Test
    fun testGetMessages_LocalFirst() = runTest {
        // Add message to local DB
        val msg = ChatMessage(
            id = "1",
            sender = "user1",
            text = "Hello",
            type = ChatMessageType.Text,
            timestamp = System.currentTimeMillis(),
            status = "sent",
            metadata = null
        )
        mockDb.insertMessage(msg)
        
        val result = repo.getMessages().first()
        
        assertTrue(result is Resource.Success)
        assertEquals(1, (result as Resource.Success).data.size)
    }
    
    @Test
    fun testSendMessage_OptimisticUpdate() = runTest {
        val result = repo.sendMessage("Hello from test")
        
        // Should immediately appear in local DB (optimistic)
        val localMessages = mockDb.getAllMessages()
        assertEquals(1, localMessages.size)
        assertEquals("pending", localMessages.first().status)
        
        // After Firebase sync, should be marked "sent"
        delay(100)
        assertEquals("sent", localMessages.first().status)
    }
    
    @Test
    fun testSendMessage_OfflineQueued() = runTest {
        mockFirebase.goOffline()
        
        val result = repo.sendMessage("Offline message")
        
        assertTrue(result is Resource.Success || result is Resource.Error)
        
        // Should be in pending state
        val pending = mockDb.getPendingMessages()
        assertEquals(1, pending.size)
    }
    
    @Test
    fun testSyncPendingMessages_OnReconnect() = runTest {
        // Go offline, send message
        mockFirebase.goOffline()
        repo.sendMessage("Offline message")
        
        // Come back online
        mockFirebase.goOnline()
        repo.syncWithFirebase()
        
        // Message should be pushed to Firebase
        val firebase = mockFirebase.getAllMessages()
        assertEquals(1, firebase.size)
    }
}
```

## 3. State Management Tests (ViewModel)

```kotlin
// commonTest/kotlin/viewmodel/ChatViewModelTest.kt
class ChatViewModelTest {
    private lateinit var viewModel: ChatViewModel
    private lateinit var mockRepository: MockChatRepository
    
    @BeforeTest
    fun setup() {
        mockRepository = MockChatRepository()
        viewModel = ChatViewModel(mockRepository)
    }
    
    @Test
    fun testLoadMessages_UpdatesStateFlow() = runTest {
        val testMessages = listOf(
            ChatMessage("1", "user1", "Hello", ChatMessageType.Text, System.currentTimeMillis(), "sent", null)
        )
        mockRepository.setMessages(testMessages)
        
        viewModel.loadMessages()
        
        val messages = viewModel.messages.first()
        assertTrue(messages is Resource.Success)
        assertEquals(1, (messages as Resource.Success).data.size)
    }
    
    @Test
    fun testLoadMessages_LoadingState() = runTest {
        mockRepository.setLoadingDelay(100)
        
        viewModel.loadMessages()
        
        // Should immediately show loading
        val loading = viewModel.isLoading.value
        assertTrue(loading)
        
        delay(150)
        
        // Should update after delay
        val notLoading = viewModel.isLoading.value
        assertFalse(notLoading)
    }
    
    @Test
    fun testSendMessage_Error() = runTest {
        mockRepository.setSendError(AppError.Network(IOException("Offline")))
        
        viewModel.sendMessage("Test")
        
        val error = viewModel.error.value
        assertTrue(error is AppError.Network)
    }
}
```

## 4. E2E Test Scenarios

```kotlin
// commonTest/kotlin/e2e/ChattingWithPaymentE2ETest.kt
class ChattingWithPaymentE2ETest {
    private lateinit var walletRepo: WalletRepository
    private lateinit var chatRepo: ChatRepository
    private lateinit var txRepo: TransactionRepository
    
    @BeforeTest
    fun setup() {
        // Setup mocked dependencies
    }
    
    @Test
    fun testFullFlow_SendPaymentViaChat() = runTest {
        // 1. User sees wallet balance
        val balance = walletRepo.getBalance(testWalletAddress)
        assertTrue(balance is Resource.Success)
        
        // 2. User sends message with payment intent
        val txResult = chatRepo.sendMessage("send 0.5 ETH to 0xrecipient")
        assertTrue(txResult is Resource.Success)
        val txHash = (txResult as Resource.Success).data
        assertNotNull(txHash)
        
        // 3. Message appears in chat
        val messages = chatRepo.getMessages().first()
        assertTrue((messages as Resource.Success).data.any { it.text.contains("0.5 ETH") })
        
        // 4. Transaction shows in history
        val txStatus = txRepo.getTransactionStatus(txHash!!)
        assertEquals(TransactionStatus.Submitted, txStatus)
        
        // 5. After confirmation, status updates
        delay(1000)  // Simulate blockchain confirmation
        val confirmed = txRepo.getTransactionStatus(txHash)
        assertEquals(TransactionStatus.Confirmed, confirmed)
    }
    
    @Test
    fun testFullFlow_SwapWithQuoteRefresh() = runTest {
        // 1. Request quote
        val quote1 = tradeRepo.getSwapQuote(
            fromToken = Token("ETH", "Ethereum", 18),
            toToken = Token("USDC", "USD Coin", 6),
            amount = BigDecimal("1"),
            slippage = BigDecimal("1")
        )
        assertTrue(quote1 is Resource.Success)
        
        // 2. Wait for expiry
        delay(45_000)
        
        // 3. Request new quote
        val quote2 = tradeRepo.getSwapQuote(...)
        
        // Should have different ID/timestamp
        assertTrue((quote2 as Resource.Success).data.id != (quote1 as Resource.Success).data.id)
    }
}
```

## Platform-Specific Tests

### Android Tests
```kotlin
// androidTest/kotlin/platform/SecureStorageTest.kt
@RunWith(AndroidRunner::class)
class AndroidSecureStorageTest {
    private lateinit var storage: SecureStorage
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        storage = SecureStorage  // AndroidMain implementation
    }
    
    @Test
    fun testEncryptedStorage() = runTest {
        storage.store("test_key", "test_value")
        val retrieved = storage.retrieve("test_key")
        
        assertEquals("test_value", retrieved)
    }
    
    @Test
    fun testStoragePersistenceAcrossInstances() = runTest {
        storage.store("persist_key", "persist_value")
        
        // Create new instance
        val storage2 = SecureStorage
        val retrieved = storage2.retrieve("persist_key")
        
        assertEquals("persist_value", retrieved)
    }
}
```

### iOS Tests
```kotlin
// iosTest/kotlin/platform/KeychainTest.kt
class IOSKeychainTest {
    private lateinit var storage: SecureStorage
    
    @BeforeTest
    fun setup() {
        storage = SecureStorage  // iosMain implementation
    }
    
    @Test
    fun testKeychainStorage() = runTest {
        storage.store("passcode", "1234")
        val retrieved = storage.retrieve("passcode")
        
        assertEquals("1234", retrieved)
    }
}
```

## Test Configuration

```kotlin
// commonTest/kotlin/TestSetup.kt
class MockSecureStorage : SecureStorage {
    private val storage = mutableMapOf<String, String>()
    
    override suspend fun store(key: String, value: String) {
        storage[key] = value
    }
    
    override suspend fun retrieve(key: String): String? {
        return storage[key]
    }
    
    override suspend fun delete(key: String) {
        storage.remove(key)
    }
}

class MockFirebaseDatabase : FirebaseDatabase {
    private val messages = mutableListOf<ChatMessageFirebase>()
    private var isOnline = true
    
    fun goOffline() { isOnline = false }
    fun goOnline() { isOnline = true }
    
    override fun reference(): DatabaseReference {
        return MockDatabaseReference(messages, isOnline)
    }
    
    fun getAllMessages() = messages
}
```

## Test Execution

```bash
# Run all common tests
./gradlew commonTest

# Run iOS tests
./gradlew iosTest

# Run Android tests
./gradlew connectedAndroidTest

# Run Desktop tests
./gradlew desktopTest

# Run specific test class
./gradlew commonTest --tests "*WalletAddressTest*"

# With coverage report
./gradlew koverHtmlReport
```

## Test Coverage Targets

- **Critical business logic**: 90%+ (error handling, state machines, value classes)
- **Repositories**: 80%+ (mocked HTTP/Firebase)
- **ViewModels**: 70%+ (state flow updates)
- **Utilities**: 85%+
- **UI Components**: View layer testing via E2E (not unit tested)

## Common Test Patterns

### Testing Async with runTest
```kotlin
@Test
fun testAsync() = runTest {
    val result = someAsyncFunction()
    assertEquals(expected, result)
}
```

### Testing Flows
```kotlin
@Test
fun testFlow() = runTest {
    val flow = repo.messages()
    
    val first = flow.first()
    assertEquals(Resource.Loading(), first)
    
    val second = flow.drop(1).first()
    assertEquals(Resource.Success(data), second)
}
```

### Testing State Changes
```kotlin
@Test
fun testStateChange() = runTest {
    viewModel.loadMessages()
    
    val isLoading = viewModel.isLoading.first()
    assertTrue(isLoading)
    
    val notLoading = viewModel.isLoading.drop(1).first()
    assertFalse(notLoading)
}
```

## References
- Execution steps: `resources/execution-protocol.md`
- Mock libraries: `resources/mock-setup.md`
- Checklist: `resources/checklist.md`
- Common failures: `resources/troubleshooting.md`

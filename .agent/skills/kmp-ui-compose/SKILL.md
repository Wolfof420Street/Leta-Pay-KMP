---
name: kmp-ui-compose
description: "KMP Compose Multiplatform UI specialist. Use when: building UI screens, implementing composables, handling navigation, styling with Material 3 tokens, or solving UI/UX issues across all platforms."
---

# KMP UI Compose - Cross-Platform User Interface

## When to Use
- Building UI screens (ChatScreen, WalletScreen, TradeScreen, YieldScreen)
- Creating reusable Composables and components
- Implementing Compose Multiplatform navigation
- Styling with Material 3 design tokens
- Handling platform-specific UI patterns (iOS HIG vs Material Design)
- Debugging UI issues across platforms

## When NOT to use
- Business logic → use Shared KMP Core skill
- Platform-specific code → use KMP Platform Specifics skill
- State management setup → use Firebase & Realtime skill

## Core Rules

### 1. Compose Multiplatform Architecture
```
shared/
├── src/commonMain/kotlin/
│   ├── ui/
│   │   ├── screens/          # Screen composables (ChatScreen, WalletScreen, etc)
│   │   ├── components/       # Reusable components (TokenCard, TransactionItem, etc)
│   │   ├── theme/            # Material 3 theme, colors, typography
│   │   └── navigation/       # NavHost and route definitions
```

### 2. Screen Template
```kotlin
// commonMain/kotlin/ui/screens/ChatScreen.kt
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onNavigateToTransactionDetail: (TxHash) -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()
    
    LaunchedEffect(Unit) {
        chatViewModel.loadMessages()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        ChatHeader()
        
        // Messages list
        when {
            isLoading -> LoadingSkeletons()
            error != null -> ErrorMessage(error!!) { chatViewModel.loadMessages() }
            messages.isEmpty() -> EmptyChatState()
            else -> MessagesList(
                messages = messages,
                onMessageClick = { msg ->
                    if (msg.txHash != null) {
                        onNavigateToTransactionDetail(msg.txHash)
                    }
                }
            )
        }
        
        // Input box
        ChatInputBox(onSend = { text -> chatViewModel.sendMessage(text) })
    }
}
```

### 3. component Composable Pattern
```kotlin
// commonMain/kotlin/ui/components/TokenCard.kt
@Composable
fun TokenCard(
    token: Token,
    balance: BigDecimal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Token info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Balance
            Text(
                text = balance.toWalletDisplay(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

### 4. Handling Async States (Resource<T>)
```kotlin
// ✅ GOOD - Load, Success, Error states
@Composable
fun WalletScreen(viewModel: WalletViewModel) {
    val balances by viewModel.balances.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val resource = balances) {
            is Resource.Loading -> {
                LoadingSkeletons()
            }
            is Resource.Success -> {
                LazyColumn {
                    items(resource.data) { balance ->
                        TokenCard(
                            token = balance.token,
                            balance = balance.amount
                        )
                    }
                }
            }
            is Resource.Error -> {
                ErrorCard(
                    error = resource.error,
                    onRetry = { viewModel.refreshBalances() }
                )
            }
        }
    }
}
```

### 5. Material 3 Theme Setup
```kotlin
// commonMain/kotlin/ui/theme/Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    error = ErrorColor,
)

@Composable
fun LetaPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = LetaPayTypography,
        content = content
    )
}
```

### 6. Navigation
```kotlin
// commonMain/kotlin/ui/navigation/Routes.kt
sealed class Route(val path: String) {
    object Chat : Route("chat")
    object Wallet : Route("wallet")
    object Trade : Route("trade")
    object Yield : Route("yield")
    object Account : Route("account")
    object Auth : Route("auth")
    data class TransactionDetail(val txHash: String) : Route("transaction/$txHash")
}

// commonMain/kotlin/ui/navigation/NavHost.kt
@Composable
fun LetaPayNavHost(
    navController: NavController,
    startRoute: String = Route.Chat.path
) {
    NavHost(navController = navController, startDestination = startRoute) {
        composable(Route.Chat.path) {
            ChatScreen(
                chatViewModel = getKoinInstance(),
                onNavigateToTransactionDetail = { txHash ->
                    navController.navigate(Route.TransactionDetail(txHash.value).path)
                }
            )
        }
        composable(Route.Wallet.path) {
            WalletScreen(walletViewModel = getKoinInstance())
        }
        composable(Route.Trade.path) {
            TradeScreen(tradeViewModel = getKoinInstance())
        }
        composable(Route.Yield.path) {
            YieldScreen(yieldViewModel = getKoinInstance())
        }
        composable(Route.Account.path) {
            AccountScreen()
        }
        composable(Route.Auth.path) {
            AuthScreen(authViewModel = getKoinInstance())
        }
        composable(Route.TransactionDetail("{txHash}").path) { backStackEntry ->
            val txHash = backStackEntry.arguments?.getString("txHash") ?: return@composable
            TransactionDetailScreen(txHash = TxHash(txHash))
        }
    }
}
```

### 7. State Management with StateFlow
```kotlin
// commonMain/kotlin/ui/viewmodel/ChatViewModel.kt
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _messages = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val messages: StateFlow<Resource<List<ChatMessage>>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()
    
    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getMessages()
                .collect { resource ->
                    _messages.value = resource
                    _isLoading.value = false
                    if (resource is Resource.Error) {
                        _error.value = resource.error
                    }
                }
        }
    }
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(text)
                .collect { resource ->
                    if (resource is Resource.Error) {
                        _error.value = resource.error
                    }
                }
        }
    }
}
```

## Platform-Specific UI Patterns

### Material Design (Android/Web)
```kotlin
// Use Material 3 by default
@Composable
fun MaterialButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text)
    }
}
```

### iOS Human Interface Guidelines (iOS/macOS)
```kotlin
// iOS follows standard Compose patterns, but adjust:
// - Use more rounded corners
// - Larger tap targets (44dp minimum)
// - Bottom sheet instead of dialog
@Composable
fun IOSButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text)
    }
}
```

## Typography System
```kotlin
// commonMain/kotlin/ui/theme/Typography.kt
val LetaPayTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

## Loading Skeletons
```kotlin
// commonMain/kotlin/ui/components/LoadingSkeletons.kt
@Composable
fun TokenCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmer()
        )
    }
}

// commonMain/kotlin/ui/components/Shimmer.kt
fun Modifier.shimmer(): Modifier = composed {
    val shimmerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatingAnimation(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        )
    )
    
    background(
        brush = LinearGradientBrush(
            colors = listOf(shimmerColor, MaterialTheme.colorScheme.background, shimmerColor),
            start = Offset(offset, 0f),
            end = Offset(offset + 1000f, 0f)
        )
    )
}
```

## Common Composables Library

### Error Card
```kotlin
@Composable
fun ErrorCard(error: AppError, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Error: ${error.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Retry")
            }
        }
    }
}
```

### Empty State
```kotlin
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

## Validation in Forms
```kotlin
@Composable
fun WalletInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Recipient Address") },
        isError = isError,
        supportingText = if (isError) { { Text(errorMessage) } } else null,
        modifier = Modifier.fillMaxWidth()
    )
}
```

## References
- Execution steps: `resources/execution-protocol.md`
- Code examples: `resources/examples.md`
- Checklist: `resources/checklist.md`
- Component library: `resources/components.md`
- Color tokens: `resources/design-tokens.md`
- Lessons learned: `.agent/.shared/lessons-learned.md`

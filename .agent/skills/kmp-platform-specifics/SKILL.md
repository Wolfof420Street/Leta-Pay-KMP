---
name: kmp-platform-specifics
description: "KMP platform specialist. Use when: implementing platform-specific code (androidMain, iosMain, desktopMain, jsMain), handling platform APIs (Keychain, EncryptedSharedPreferences, FCM, APNs), or solving platform-specific bugs."
---

# KMP Platform Specifics - Cross-Platform Implementation

## When to Use
- Implementing expect/actual declarations (SecureStorage, PushNotificationManager, DispatcherProvider)
- Android-specific code (EncryptedSharedPreferences, FCM token registration)
- iOS-specific code (Keychain, APNs setup, CocoaPods dependencies)
- Desktop-specific code (JVM keystore, file encryption)
- Web-specific code (IndexedDB, Web Push API, sessionStorage)
- Handling platform permissions and lifecycle
- Debugging platform-specific issues

## When NOT to Use
- Shared business logic → use Shared KMP Core skill
- UI/Compose code → use KMP UI Compose skill
- Testing logic → use KMP Testing skill
- Backend integration → use Firebase & Realtime skill

## Platform-Specific Implementation Rules

### 1. Android (androidMain)

**SecureStorage Implementation**:
```kotlin
// androidMain/kotlin/platform/SecureStorage.kt
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual object SecureStorage {
    private val masterKey = MasterKey.Builder(AndroidContext.context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        AndroidContext.context,
        "encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    actual suspend fun store(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    actual suspend fun retrieve(key: String): String? {
        return sharedPreferences.getString(key, null)
    }
    
    actual suspend fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}
```

**Firebase Cloud Messaging (FCM)**:
```kotlin
// androidMain/kotlin/platform/PushNotificationManager.kt
import com.google.firebase.messaging.FirebaseMessaging

actual object PushNotificationManager {
    actual suspend fun registerForPushNotifications(): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val token = suspendCancellableCoroutine<String> { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception("Unknown FCM error"))
                    }
                }
            }
            Resource.Success(token)
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
}

// AndroidManifest.xml
<service
    android:name="com.google.firebase.messaging.FirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

**DispatcherProvider**:
```kotlin
// androidMain/kotlin/platform/DispatcherProvider.kt
actual object DispatcherProvider {
    actual val io: CoroutineDispatcher = Dispatchers.IO
    actual val main: CoroutineDispatcher = Dispatchers.Main
    actual val default: CoroutineDispatcher = Dispatchers.Default
}
```

### 2. iOS (iosMain)

**Keychain Storage**:
```kotlin
// iosMain/kotlin/platform/SecureStorage.kt
import platform.Security.*
import platform.Foundation.*
import kotlinx.cinterop.*

actual object SecureStorage {
    actual suspend fun store(key: String, value: String) {
        val keyData = key.encodeToByteArray().toNSData()
        val valueData = value.encodeToByteArray().toNSData()
        
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to keyData,
            kSecValueData to valueData
        )
        
        // Delete existing
        memScoped {
            SecItemDelete(query as CFDictionary)
        }
        
        // Add new
        memScoped {
            SecItemAdd(query as CFDictionary, null)
        }
    }
    
    actual suspend fun retrieve(key: String): String? {
        val keyData = key.encodeToByteArray().toNSData()
        
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to keyData,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimitOne to kSecMatchLimitOne
        )
        
        var result: NSData? = null
        memScoped {
            val resultRef = alloc<COpaquePointerVar>()
            SecItemCopyMatching(query as CFDictionary, resultRef.ptr)
            result = resultRef.value as? NSData
        }
        
        return result?.let { String(it.bytes!!, it.length.toInt()) }
    }
    
    actual suspend fun delete(key: String) {
        val keyData = key.encodeToByteArray().toNSData()
        val query = mutableMapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to keyData
        )
        memScoped {
            SecItemDelete(query as CFDictionary)
        }
    }
}
```

**Apple Push Notification (APNs)**:
```kotlin
// iosMain/kotlin/platform/PushNotificationManager.kt
import platform.UserNotifications.*

actual object PushNotificationManager {
    actual suspend fun registerForPushNotifications(): Resource<String> {
        return try {
            // Request user permission
            suspendCancellableCoroutine<String> { continuation ->
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                    completionHandler = { granted, error ->
                        if (granted) {
                            // Cannot get APNs token from Kotlin directly, 
                            // must handle in Swift AppDelegate
                            println("APNs permission granted")
                            continuation.resume("native-apns-token-from-swift")
                        } else {
                            continuation.resumeWithException(Exception("APNs permission denied"))
                        }
                    }
                )
            }
            Resource.Success("apns-token")
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
}
```

**DispatcherProvider**:
```kotlin
// iosMain/kotlin/platform/DispatcherProvider.kt
actual object DispatcherProvider {
    actual val io: CoroutineDispatcher = Dispatchers.Default
    actual val main: CoroutineDispatcher = Dispatchers.Main
    actual val default: CoroutineDispatcher = Dispatchers.Default
}
```

### 3. Desktop (desktopMain/JVM)

**Encrypted File Storage**:
```kotlin
// desktopMain/kotlin/platform/SecureStorage.kt
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

actual object SecureStorage {
    private val storageFile = File(System.getProperty("user.home"), ".letapay/secure_storage")
    private val cipher = Cipher.getInstance("AES")
    
    init {
        storageFile.parentFile?.mkdirs()
    }
    
    private fun getKey(): SecretKeySpec {
        // In production, load from system keystore
        val keyBytes = ByteArray(32) // Hardcoded for MVP; use proper key management
        return SecretKeySpec(keyBytes, 0, keyBytes.size, 0, 32, "AES")
    }
    
    actual suspend fun store(key: String, value: String) {
        val encryptedValue = cipher.apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }.doFinal(value.toByteArray())
        
        val encoded = Base64.getEncoder().encodeToString(encryptedValue)
        storageFile.appendText("$key=$encoded\n")
    }
    
    actual suspend fun retrieve(key: String): String? {
        return storageFile.readLines().firstNotNullOfOrNull { line ->
            if (line.startsWith("$key=")) {
                val encoded = line.substringAfter("=")
                val encrypted = Base64.getDecoder().decode(encoded)
                cipher.apply { init(Cipher.DECRYPT_MODE, getKey()) }
                    .doFinal(encrypted)
                    .decodeToString()
            } else null
        }
    }
    
    actual suspend fun delete(key: String) {
        val lines = storageFile.readLines().filter { !it.startsWith("$key=") }
        storageFile.writeText(lines.joinToString("\n"))
    }
}
```

### 4. Web (jsMain)

**IndexedDB Storage + SessionStorage for Tokens**:
```kotlin
// jsMain/kotlin/platform/SecureStorage.kt
import org.w3c.dom.Storage
import org.w3c.indexeddb.IndexedDB

actual object SecureStorage {
    private val sessionStorage: Storage = js("sessionStorage")
    
    actual suspend fun store(key: String, value: String) {
        // sessionToken → sessionStorage (in-memory, cleared on tab close)
        // NO refreshToken storage on web (re-auth on expiry)
        if (key == "sessionToken") {
            sessionStorage.setItem(key, value)
        }
    }
    
    actual suspend fun retrieve(key: String): String? {
        return if (key == "sessionToken") {
            sessionStorage.getItem(key)
        } else {
            null  // No persistent storage on web
        }
    }
    
    actual suspend fun delete(key: String) {
        sessionStorage.removeItem(key)
    }
}
```

**Web Push Notifications**:
```kotlin
// jsMain/kotlin/platform/PushNotificationManager.kt
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationPermission
import org.w3c.notifications.NotificationOptions

actual object PushNotificationManager {
    actual suspend fun registerForPushNotifications(): Resource<String> {
        return try {
            // Request permission
            val permission = suspendCancellableCoroutine<NotificationPermission> { continuation ->
                Notification.requestPermission {
                    continuation.resume(it)
                }
            }
            
            if (permission == NotificationPermission.GRANTED) {
                // Browser would generate unique ID
                Resource.Success("web-push-subscription-id")
            } else {
                Resource.Error(AppError.Unauthorized("Web Push permission denied"))
            }
        } catch (e: Exception) {
            Resource.Error(AppError.Unexpected(e))
        }
    }
}
```

## Platform Dependencies (build.gradle.kts)

```kotlin
// Shared common dependencies
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:2.3.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
                implementation("io.insert-koin:koin-core:3.4.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("com.google.firebase:firebase-messaging:23.2.1")
            }
        }
        
        val iosMain by getting {
            // CocoaPods handled in separate podspec
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-java:2.3.0")
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:2.3.0")
            }
        }
    }
}
```

## Platform Lifecycle Rules

### Android
- ✅ Store sessionToken in EncryptedSharedPreferences
- ✅ Never store refreshToken in plaintext
- ✅ Register FCM token on app launch
- ✅ Handle Firebase config in google-services.json

### iOS
- ✅ Store sessionToken in Keychain
- ✅ Never store refreshToken in plaintext
- ✅ Handle APNs token in Swift AppDelegate, pass to Kotlin
- ✅ Use CocoaPods for Firebase dependencies

### Desktop
- ✅ Encrypt storage file with user's system key
- ✅ Store in ~/.letapay/ (user home directory)
- ✅ Never hardcode master key (use system keystore in production)

### Web
- ✅ sessionToken only in sessionStorage (cleared on tab close)
- ❌ NO refreshToken storage (re-auth on expiry)
- ✅ Use Web Push API for notifications
- ✅ Handle CORS for backend requests

## Common Pitfalls

1. **Forgetting expect/actual** - Declare in commonMain, implement in platform-specific sources
2. **Storing refreshToken on web** - Violates security model; use session-only
3. **Not handling lifecycle** - Listen to app pause/resume for token refresh
4. **Mixing platform code in commonMain** - Always use expect/actual
5. **Not validating platform permissions** - Check FCM/APNs granted before use

## References
- Execution steps: `resources/execution-protocol.md`
- Code examples: `resources/examples.md`
- Checklist: `resources/checklist.md`
- Error recovery: `resources/error-playbook.md`
- Platform rules: `resources/platform-rules.md`
- Lessons learned: `.agent/.shared/lessons-learned.md`

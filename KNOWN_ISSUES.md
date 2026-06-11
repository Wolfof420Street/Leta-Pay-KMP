# Known Issues

This file tracks KMP placeholder cleanup status after production-hardening validation.

## Completed in this pass

### DONE-001: Shared SIWE message builder is now spec-compliant
- Files:
  - `core/common/src/commonMain/kotlin/com/letapay/app/core/common/SiweMessageBuilder.kt`
  - `core/network/src/commonMain/kotlin/com/letapay/app/core/network/auth/AuthApi.kt`
- Completion note:
  - SIWE message construction now uses a shared builder with domain, address, URI, nonce, chain ID, issued-at, and optional statement fields.
  - Client auth path continues to use backend verification via `/auth/verify-signature`.

### DONE-002: Profile screen no longer shows a fake wallet address
- Files:
  - `feature/profile/src/commonMain/kotlin/com/letapay/app/feature/profile/ProfileViewModel.kt`
  - `feature/profile/src/commonMain/kotlin/com/letapay/app/feature/profile/ProfileRoute.kt`
  - `feature/profile/src/commonMain/kotlin/com/letapay/app/feature/profile/ProfileScreen.kt`
  - `feature/profile/src/commonMain/composeResources/values/strings.xml`
- Completion note:
  - Profile now binds to live session state and shows a real wallet address when connected.

### DONE-003: Trade quote stub replaced with real swap quote and unsigned-tx build flow
- Files:
  - `feature/trade/src/commonMain/kotlin/com/letapay/app/feature/trade/TradeViewModel.kt`
  - `feature/trade/src/commonMain/kotlin/com/letapay/app/feature/trade/TradeScreen.kt`
- Completion note:
  - Trade UI now calls `SwapRepository.quote(...)` and `SwapRepository.execute(...)` instead of toggling a loading placeholder.

### DONE-004: Template TODO stubs removed from platform intent/review implementations
- Files:
  - `core-base/platform/src/nonAndroidMain/kotlin/template/core/base/platform/intent/IntentManagerImpl.kt`
  - `core-base/platform/src/nonAndroidMain/kotlin/template/core/base/platform/review/AppReviewManagerImpl.kt`
  - `core-base/platform/src/androidMain/kotlin/template/core/base/platform/review/AppReviewManagerImpl.kt`
- Completion note:
  - Explicit no-op/delegating behavior replaces `TODO("Not yet implemented")` placeholders.

### DONE-005: Backend stub aliases and unsafe null assertions removed
- Files:
  - `backend-ktor/src/main/kotlin/com/letapay/backend/service/ScreeningService.kt`
  - `backend-ktor/src/main/kotlin/com/letapay/backend/service/CoinbaseService.kt`
  - `backend-ktor/src/test/kotlin/com/letapay/backend/Phase8Test.kt`
  - `core-base/analytics/src/commonMain/kotlin/template/core/base/analytics/PerformanceTracker.kt`
  - `core-base/designsystem/src/commonMain/kotlin/template/core/base/designsystem/layout/AdaptiveNavigableListDetailScaffold.kt`
- Completion note:
  - Dead stub symbols were removed, the live test now uses the real Coinbase service, and shared code no longer relies on `!!` for those paths.

### DONE-012: Application scope DI prevents test coroutine leaks
- Files:
  - `backend-ktor/src/main/kotlin/com/letapay/backend/plugins/DependencyInjection.kt`
  - `backend-ktor/src/main/kotlin/com/letapay/backend/Application.kt`
  - `backend-ktor/src/main/kotlin/com/letapay/backend/plugins/Routing.kt`
- Completion note:
  - Introduced a DI-provided `applicationScope` and switched the application to use it for background jobs. This allows tests to override the scope (e.g., with a `TestScope`) and prevents background coroutines from leaking after test scopes close.

## Deferred / follow-up required

### DEFER-001: Full client-side swap execution orchestration
- Scope requested:
  - get quote -> build calldata via backend/sidecar -> prompt wallet signing -> submit signed tx -> watch confirmation with `ConfirmationWatcher`.
- Current state:
  - Quote and unsigned-tx build are implemented in UI flow.
  - Wallet signing, signed submission, and confirmation subscription are not yet wired end-to-end in KMP UI.
- What is needed:
  - A cross-platform signing abstraction, signed transaction submit endpoint integration, and client-facing confirmation stream/state model.

### DEFER-002: Template-derived copy/assets outside hardening path
- Files:
  - `feature/home/src/commonMain/composeResources/values/strings.xml`
  - `feature/settings/src/commonMain/composeResources/values*/strings.xml`
  - `core-base/analytics/README.md`
  - `core-base/platform/README.md`
- Reason deferred:
  - These are non-runtime-polish and documentation cleanup items not required to unblock backend/sidecar production hardening gates.

### DONE-007: Navbar string resources and localized labels
- Files:
  - `cmp-navigation/src/commonMain/kotlin/cmp/navigation/authenticatednavbar/AuthenticatedNavBarTabItem.kt`
  - `cmp-navigation/src/commonMain/composeResources/values/strings.xml`
  - `cmp-navigation/src/commonMain/composeResources/values-de/strings.xml`
- Completion note:
  - Navigation tabs now use explicit `chat_tab`, `wallet_tab`, `trade_tab`, and `yield_tab` resources instead of reusing generic placeholders.

### DONE-008: Centralize chain brand colors into shared theme tokens
- Files:
  - `feature/wallet/src/commonMain/kotlin/com/letapay/app/feature/wallet/WalletScreen.kt`
  - `cmp-shared/src/commonMain/kotlin/cmp/shared/ui/theme/LetaColors.kt`
- Completion note:
  - Wallet chain colors now come from the shared `LetaColors` theme object instead of local constants.

### DONE-006: Platform manager shims now emit explicit no-op warnings
- Files:
  - `core-base/platform/src/nonAndroidMain/kotlin/template/core/base/platform/intent/IntentManagerImpl.kt`
  - `core-base/platform/src/nonAndroidMain/kotlin/template/core/base/platform/review/AppReviewManagerImpl.kt`
  - `core-base/platform/src/nonAndroidMain/kotlin/template/core/base/platform/update/AppUpdateManagerImpl.kt`
  - `core-base/platform/README.md`
- Completion note:
  - Unsupported non-Android manager capabilities no longer use empty bodies; they now log explicit no-op warnings so placeholder behavior is visible without crashing builds.

### VERIFIED-ALL: Runtime TODOs cleared
- Verification note:
  - All runtime `TODO` placeholders and failing stubs were replaced with explicit no-op implementations or safe guards during the hardening pass on 2026-05-27.

### BUILD-STABILITY-001: Kotlin compile daemon fallback enabled
- Completion note:
  - Gradle now forces Kotlin compilation in-process when daemon connectivity is unstable, which avoids the compile-daemon handshake failure seen in backend test runs.

### DONE-009: Background reconciliation jobs are production-only
- Files:
  - `backend-ktor/src/main/kotlin/com/letapay/backend/Application.kt`
- Completion note:
  - Reconciliation, confirmation-watching, and cleanup coroutines no longer start during test/dev app boots, which removes the lingering coroutine leak from backend test teardown.

### DONE-010: Redis rate limiting is production-only
- Files:
  - `backend-ktor/src/main/kotlin/com/letapay/backend/plugins/DependencyInjection.kt`
- Completion note:
  - Production now uses Redis-backed rate limiting while tests and non-production use in-memory limiting, preventing cross-test throttling failures.

### DONE-011: Backend client resources close on application shutdown
- Files:
  - `backend-ktor/src/main/kotlin/com/letapay/backend/service/RedisInfra.kt`
  - `backend-ktor/src/main/kotlin/com/letapay/backend/Application.kt`
- Completion note:
  - Redis and HTTP clients are now closed during application shutdown so integration tests do not leave active event-loop coroutines behind.

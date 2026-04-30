# KMP Skills Routing Guide

This guide helps you choose the right skill for your task in the Leta-Pay-KMP project.

## Quick Decision Tree

```
START: What are you working on?
  │
  ├─→ Business logic, models, error handling, typed values?
  │   └─→ **shared-kmp-core** ✨
  │
  ├─→ Expect/actual, platform-specific code (Keychain, FCM, IndexedDB)?
  │   └─→ **kmp-platform-specifics** ✨
  │
  ├─→ UI screens, Composables, navigation, Material Design?
  │   └─→ **kmp-ui-compose** ✨
  │
  ├─→ Firebase Realtime, backend HTTP client, custom auth, device tokens?
  │   └─→ **firebase-and-realtime** ✨
  │
  ├─→ Structure, module layout, dependencies, where to place code?
  │   └─→ **kmp-module-architecture** ✨
  │
  ├─→ Phase 1-9 execution, deliverables, gate criteria?
  │   └─→ **phase-orchestrator** ✨
  │
  └─→ Unit tests, integration tests, E2E tests, mocks?
      └─→ **kmp-testing** ✨
```

## Skill Descriptions & Use Cases

### 1. `shared-kmp-core` 
**Core Domain Logic & Shared Business Logic**

**Use when:**
- Creating value classes (ChainId, WalletAddress, TxHash, AssetId)
- Defining AppError sealed hierarchy
- Building Resource<T> wrappers for async states
- Writing repository interfaces (contracts)
- Implementing use cases
- Setting up Koin DI modules
- Creating domain models (no UI)

**Examples:**
- "Create a typed value class for transaction amounts"
- "Add a new error type for staking failures"
- "Implement getBalances() repository method"
- "Set up Koin module for auth layer"

**Output:** Reusable code that compiles in commonMain

---

### 2. `kmp-platform-specifics`
**Platform-Specific Code & Native APIs**

**Use when:**
- Implementing SecureStorage for Android/iOS/Desktop/Web
- Handling FCM (Android) or APNs (iOS) push tokens
- Setting up platform lifecycle management
- Debugging platform-specific crashes
- Creating platform-specific storage (Keychain, EncryptedSharedPreferences, IndexedDB)
- Handling platform permissions

**Examples:**
- "Add APNs token registration for iOS"
- "Implement EncryptedSharedPreferences on Android"
- "Create IndexedDB storage for web"
- "Debug why sessionToken isn't persisting on iOS"

**Output:** Working expect/actual implementations for all 4 platforms

---

### 3. `kmp-ui-compose`
**Compose Multiplatform User Interface**

**Use when:**
- Building Composable screens (ChatScreen, WalletScreen, etc.)
- Creating reusable components (TokenCard, TransactionItem, etc.)
- Implementing Material 3 theme
- Setting up navigation
- Handling loading/error UI states
- Testing UI across platforms

**Examples:**
- "Build a wallet balance screen with loading skeleton"
- "Create a transaction card component with click handler"
- "Implement bottom navigation between 5 screens"
- "Add Material 3 theme for dark mode support"

**Output:** UI code that renders on Android, iOS, Desktop, Web

---

### 4. `firebase-and-realtime`
**Backend Integration & Real-Time Sync**

**Use when:**
- Setting up Firebase Realtime Database for chat
- Configuring Ktor HTTP client with interceptors
- Implementing custom Firebase authentication (wallet-backed)
- Building device token registration
- Handling offline/online sync
- Debugging Firebase/backend sync issues

**Examples:**
- "Connect Firebase Realtime DB for chat messages"
- "Implement auth token refresh on 401 Unauthorized"
- "Build offline message queue with retry logic"
- "Register device tokens for push notifications from backend"

**Output:** Backend integration code + local sync logic

---

### 5. `kmp-module-architecture`
**Module Structure & Dependency Management**

**Use when:**
- Designing where new code should live (core vs feature vs data)
- Resolving circular dependency issues
- Organizing gradle build files
- Creating new feature modules
- Setting up gradle conventions

**Examples:**
- "Where should I put the balance repository?"
- "How do I add a new feature module without circular deps?"
- "Fix circular dependency between auth and wallet modules"
- "Set up gradle convention plugins"

**Output:** Clear module organization + dependency diagram

---

### 6. `phase-orchestrator`
**Phase-Based Development & Execution**

**Use when:**
- Planning Phase 1, 2, 3, etc. work
- Understanding deliverables for current phase
- Verifying gate criteria before moving to next phase
- Breaking phase work into sprint tasks
- Tracking phase progress

**Examples:**
- "What are Phase 1 deliverables?"
- "Create checklist for Phase 3 completion"
- "Verify Phase 2 gate criteria are met"
- "Plan Phase 4 work (send payment feature)"

**Output:** Phase-level roadmap + task breakdown

---

### 7. `kmp-testing`
**Multiplatform Testing Strategy**

**Use when:**
- Writing unit tests for domain logic
- Creating integration tests for repositories
- Building E2E tests (chat→payment flow)
- Setting up test mocks (MockHttpClient, MockDatabase)
- Debugging test failures
- Achieving test coverage targets

**Examples:**
- "Write unit tests for WalletAddress validation"
- "Create integration test for chat message sync"
- "Build E2E test for full payment flow"
- "Set up mock Firebase for testing offline queue"

**Output:** Tested code with mock infrastructure

---

## Real-World Scenarios

### Scenario 1: "Implement wallet balance display"

```
1. Create Balance model (core/model) → shared-kmp-core
2. Create WalletRepository interface → shared-kmp-core
3. Implement HTTP client call in repo → firebase-and-realtime
4. Add SQLDelight schema for caching → kmp-module-architecture (where?) + shared-kmp-core (model)
5. Create WalletScreen Composable → kmp-ui-compose
6. Set up WalletViewModel → shared-kmp-core (state holder) + kmp-ui-compose (binding)
7. Write tests for repository → kmp-testing
```

### Scenario 2: "Add FCM push notifications"

```
1. Create PushNotificationManager expect interface → shared-kmp-core
2. Implement Android FCM → kmp-platform-specifics
3. Implement iOS APNs stubs → kmp-platform-specifics
4. Implement Web Push API stubs → kmp-platform-specifics
5. Create device token registration repository → firebase-and-realtime (HTTP call)
6. Call from auth flow → shared-kmp-core (AuthRepository)
7. Write E2E test → kmp-testing
```

### Scenario 3: "Debug 'session not persisting after app restart'"

```
1. Check where session is stored → kmp-platform-specifics (SecureStorage)
2. Verify session is being retrieved on app launch → shared-kmp-core (AppStateHolder)
3. Check if token is expired → firebase-and-realtime (token refresh logic)
4. Add logging to diagnose flow → shared-kmp-core (add logging)
5. Test on actual device → kmp-testing (platform-specific test)
```

---

## Skill Combinations (Multi-Skill Tasks)

### Full Feature Implementation (Example: Chat)

```
┌─ Phase 1-3 Setup (phase-orchestrator)
│
├─ Domain models (shared-kmp-core)
│  └─ ChatMessage, ChatRoom, ChatMessage.Type
│
├─ Module organization (kmp-module-architecture)
│  └─ Where to place chat code?
│
├─ Firebase integration (firebase-and-realtime)
│  └─ Listen to Realtime DB, sync to local
│
├─ Local persistence (shared-kmp-core + kmp-module-architecture)
│  └─ SQLDelight schema + queries
│
├─ Offline queue (shared-kmp-core)
│  └─ PendingMessage state + retry logic
│
├─ UI screens (kmp-ui-compose)
│  └─ ChatScreen, ChatBubble, MessageInput
│
├─ State management (shared-kmp-core)
│  └─ ChatViewModel, chatRepository.messages() StateFlow
│
├─ Platform push notifications (kmp-platform-specifics)
│  └─ FCM/APNs token registration
│
└─ Testing (kmp-testing)
   └─ Repository tests + E2E message flow
```

### Bug Fix (Example: "Messages not syncing offline")

```
1. Understand problem (shared-kmp-core + firebase-and-realtime)
   └─ Is offline queue enabled?

2. Check platform code (kmp-platform-specifics)
   └─ Is network status detection working?

3. Verify Firebase rules (firebase-and-realtime)
   └─ Are read permissions correct?

4. Add logging (shared-kmp-core)
   └─ Log when queue stores/retrieves message

5. Create test (kmp-testing)
   └─ Mock offline scenario and verify retry

6. Deploy fix (phase-orchestrator)
   └─ Which phase does this impact?
```

---

## When to Combine Skills

**Use MULTIPLE skills when:**
- Feature touches multiple layers (UI + backend + platform)
- Involves both code implementation AND testing
- Requires architecture review + actual coding
- Phase involves multiple components

**Use SINGLE skill when:**
- Task is isolated to one domain
- It's a bug fix in one layer
- It's a small refactor

---

## Skill Switching Guide

| Current Task | Next Task | New Skill |
|---|---|---|
| Building ChatScreen | Need to sync with Firebase | firebase-and-realtime |
| Implementing chat repo | Need platform-specific storage | kmp-platform-specifics |
| Debugging sync issues | Need to write tests | kmp-testing |
| Testing complete | Planning next phase | phase-orchestrator |
| Planning phase work | Start implementation | shared-kmp-core / kmp-ui-compose |

---

## Skill Selection Checklist

Before asking for help, ask yourself:

- [ ] Is this **business logic**? (models, errors, repos) → `shared-kmp-core`
- [ ] Is this **platform-specific**? (Keychain, FCM) → `kmp-platform-specifics`
- [ ] Is this **UI**? (screens, components) → `kmp-ui-compose`
- [ ] Is this **backend/Firebase**? (HTTP, realtime) → `firebase-and-realtime`
- [ ] Is this **architecture/structure**? (where to put code) → `kmp-module-architecture`
- [ ] Is this **phase/planning**? (deliverables, gates) → `phase-orchestrator`
- [ ] Is this **testing**? (tests, mocks) → `kmp-testing`
- [ ] Is this **multiple of the above**? → Use multiple skills in sequence

---

## Quick Links to Skills

1. [Shared KMP Core](/shared-kmp-core) - Domain logic, models, errors
2. [KMP Platform Specifics](/kmp-platform-specifics) - Android/iOS/Desktop/Web native code
3. [KMP UI Compose](/kmp-ui-compose) - Screens, components, theming
4. [Firebase & Realtime](/firebase-and-realtime) - Backend, Ktor, Firebase
5. [KMP Module Architecture](/kmp-module-architecture) - Structure, dependencies
6. [Phase Orchestrator](/phase-orchestrator) - Phase planning, gates, roadmap
7. [KMP Testing](/kmp-testing) - Unit/integration/E2E tests, mocks

---

## Tips for Best Results

1. **Use one skill at a time** - Let the skill guide you start-to-finish
2. **Be specific in your request** - "Implement FCM token registration" not "add notifications"
3. **Link to plan.md** - Skills reference your architecture from plan.md
4. **Combine skills when needed** - Full features often need 2-3 skills in sequence
5. **Follow skill checklists** - They ensure you don't miss steps
6. **Reference gate criteria** - Use phase-orchestrator to know when to stop

---

## Feedback

If a skill isn't helping or seems incomplete, Copilot can suggest edits or refinements. The skills are living documents tied to your project's evolution.

---

**Last Updated:** April 2026
**Project:** Leta-Pay-KMP (Kotlin Multiplatform)
**Platforms:** Android, iOS, Desktop (JVM), Web (JS)

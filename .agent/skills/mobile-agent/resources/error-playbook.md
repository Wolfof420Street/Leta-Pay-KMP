# Mobile Agent - Error Recovery Playbook

When you encounter a failure, find the matching scenario and follow the recovery steps.
Do NOT stop or ask for help until you have exhausted the playbook.

---

## Kotlin Analysis Error

**Symptoms**: compiler or Detekt issues, type mismatch, nullability mismatch

1. Read the exact file and symbol from the failure output.
2. Fix nullability at the type boundary, not by force-casting.
3. Ensure serialization models match payload shape.
4. Remove dead code and unused declarations flagged by Detekt.
5. Never suppress warnings unless policy explicitly allows it.

---

## Build Failure

**Symptoms**: Gradle build fails, Android/iOS packaging errors

1. Resolve dependency conflicts in Gradle module files.
2. Verify Android and iOS minimum versions and signing prerequisites.
3. Re-run only the failing task with stacktrace for targeted diagnostics.
4. If environment-related, capture command output and mark as blocked with evidence.

---

## Test Failure

**Symptoms**: commonTest or platform tests fail

1. Identify whether failure is shared logic, Android, or iOS specific.
2. Validate fake/mocked dependencies at repository and platform boundaries.
3. Re-run focused tests first, then full test task for confirmation.
4. After 3 failed attempts, switch approach and document prior attempts.

---

## State Management Issue

**Symptoms**: stale UI, missing updates, race conditions

1. Verify StateFlow/SharedFlow collection lifecycle.
2. Ensure state is immutable and replaced, not mutated in place.
3. Confirm coroutine scope and dispatcher usage is correct.
4. Trace state transitions with logs around reducers/use cases.
5. Check for premature cancellation or lifecycle disposal.

---

## Platform-Specific Crash

**Symptoms**: Works on one platform, crashes on another

1. Check for `Platform.isIOS` / `Platform.isAndroid` guards
2. Check permissions: camera, location, storage — different per platform
3. Check native plugin compatibility — some plugins don't support both platforms
4. If plugin issue: note in result with platform and version info
5. Test on emulator for the failing platform

---

## Memory Leak

**Symptoms**: app slows over time, increasing memory usage

1. Check listener, subscription, and coroutine cancellation hygiene.
2. Verify references are not retained by long-lived singleton objects.
3. Inspect navigation stack for uncollected screen states.
4. Use platform profiling tools to confirm allocation hotspots.
5. Fix and re-measure before closing issue.

---

## API Integration Error

**Symptoms**: Ktor client exceptions, parse errors, timeout spikes

1. **Connection refused**: backend running? correct URL/port?
2. **401**: auth interceptor sending token? token expired?
3. **Parse error**: payload does not match serializer model.
4. **Timeout**: increase Ktor Client timeout or check network conditions
5. If backend issue: document expected contract in result

---

## Rate Limit / Quota / Memory Fallback

Same as backend-agent playbook: See "Rate Limit" and "Serena Memory" sections.

---

## General Principles

- **After 3 failures**: If same approach fails 3 times, must try a different method
- **Blocked**: If no progress after 5 turns, save current state, `Status: blocked`
- **Out of scope**: Backend/frontend issues — only record in result

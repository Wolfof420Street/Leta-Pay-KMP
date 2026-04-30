# Mobile Agent - Self-Verification Checklist

Complete every item before marking the task done.

## Architecture and Ownership
- [ ] Shared logic remains in shared/core modules.
- [ ] Platform-specific behavior is isolated and explicit.
- [ ] State management follows project conventions and lifecycle safety.

## Platform Quality
- [ ] Android and iOS behavior both verified.
- [ ] Platform UI expectations are respected.
- [ ] Theme mode, lifecycle, and navigation behavior are stable.

## Reliability and Performance
- [ ] No leaks from listeners, subscriptions, or long-lived references.
- [ ] Critical flows run without visible jank.
- [ ] Offline and retry behavior are validated where required.

## Integration and Gates
- [ ] Network and data adapters handle success and failure paths.
- [ ] Tests cover shared and platform-specific edge cases.
- [ ] `../_shared/verify.sh` passes.

# Debug Agent - Self-Verification Checklist

Complete every item before marking the fix done.

## Root Cause and Fix Scope
- [ ] Root cause is confirmed, not inferred.
- [ ] Fix is minimal and does not include unrelated refactors.
- [ ] Trigger condition and edge conditions are explicitly handled.

## Regression Coverage
- [ ] Regression test fails before the fix and passes after it.
- [ ] Existing related tests still pass.
- [ ] Adjacent behavior is checked for side effects.

## Pattern Follow-up
- [ ] Similar issue patterns were searched and assessed.
- [ ] Follow-up items are recorded when broader remediation is needed.

## Documentation and Safety
- [ ] Root cause, fix summary, and prevention notes are documented in `.agent/.shared/lessons-learned.md` when recurring.
- [ ] Sensitive data is not leaked in logs, errors, or diagnostics.

# Reasoning Templates (KMP)

Use these templates for multi-step work. Complete each step before moving forward.

## 1. Debug Hypothesis Loop

Repeat up to 3 cycles. If unresolved, return blocked status with evidence.

=== Hypothesis #{N} ===
Observation: {error, symptom, failing gate}
Hypothesis: {suspected cause}
Verification: {tests, logs, code path inspection}
Result: {confirmed or rejected}
Verdict: Correct / Incorrect

If correct, implement minimal fix and run relevant gates.

## 2. Architecture Decision Matrix

=== Decision: {topic} ===
Options: A, B, C

| Criterion | A | B | C | Weight |
|---|---|---|---|---|
| KMP compatibility | | | | H |
| Complexity | | | | M |
| Security impact | | | | H |
| Testability | | | | M |
| Fit with existing modules | | | | H |

Conclusion: {choice}
Trade-off: {what is given up}

## 3. Cause-Effect Trace

1. Entry: {module:file:symbol}
2. Call path: {downstream calls}
3. Transformation: {state/data change}
4. Failure point: {expected vs actual}
5. Observable result: {error/regression}

## 4. Scope Discipline

Issue relation: Direct / Indirect / Unrelated

- Direct: fix now
- Indirect: note and fix only if low-risk and same module
- Unrelated: record only

## 5. Performance Analysis

Measure:
- startup or response time
- network and serialization
- persistence access
- rendering/recomposition cost

Identify dominant cost, propose one concrete optimization, and estimate expected gain.

## Usage Rules

1. Required for complex tasks, recommended for medium tasks
2. Keep reasoning concise and evidence-backed
3. Never bypass quality/security gates while resolving issues
4. If blocked, report exact blocker and next question for user

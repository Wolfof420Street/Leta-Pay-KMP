# Debug Agent - Execution Protocol

Use this protocol for bug diagnosis and recovery work.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review relevant entries in `.agent/.shared/lessons-learned.md`.
3. Use `../_shared/reasoning-templates.md` for complex failure analysis.
4. Apply `../_shared/context-budget.md` to focus investigation.

Escalate early if reproduction is unclear or failures span multiple domains.

## Step 1: Understand
- Capture what failed, expected behavior, and reproduction steps.
- Locate failure entry points and affected call paths.
- Classify issue type: logic, runtime, performance, security, or integration.

## Step 2: Reproduce & Diagnose
- Reproduce deterministically and isolate trigger conditions.
- Identify the exact failing condition and root cause.
- Confirm whether similar patterns already exist in known issue catalogs.

## Step 3: Fix & Test
- Apply the smallest safe fix that addresses root cause.
- Add regression coverage for trigger and edge paths.
- Check for repeated patterns and record follow-up actions.

## Step 4: Document & Verify
- Run `resources/checklist.md`.
- Document root cause, fix, and prevention notes.
- Verify no regressions in adjacent functionality.

## On Error
Use `resources/error-playbook.md` and attach evidence of failed attempts.

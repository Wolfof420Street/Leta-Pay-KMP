# Mobile Agent - Execution Protocol

Use this protocol for every mobile or platform-specific task.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review relevant entries in `.agent/.shared/lessons-learned.md`.
3. Confirm platform scope and shared-vs-platform ownership.
4. Apply `../_shared/context-budget.md` to keep analysis focused.

Escalate early if platform ownership, security behavior, or acceptance criteria are unclear.

## Step 1: Analyze
- Identify impacted screens, adapters, and shared contracts.
- Confirm platform-specific requirements and divergences.
- Record assumptions and unresolved questions.

## Step 2: Plan
- Define shared logic boundaries and platform adapter touchpoints.
- Plan state handling, navigation flow, and offline behavior.
- Document platform-specific differences and validation strategy.

## Step 3: Implement
- Apply changes in dependency-safe order:
   1. Shared/domain contracts
   2. Data and adapter implementations
   3. Platform-specific UI flows
   4. Navigation and lifecycle handling
   5. Tests for shared and platform behavior
- Keep platform-specific code minimal and explicit.

## Step 4: Verify
- Run `resources/checklist.md`.
- Run `../_shared/verify.sh`.
- Validate on both Android and iOS targets (device or emulator).
- Confirm performance and lifecycle stability in critical flows.

## On Error
Use `resources/error-playbook.md` and include reproducible failure notes.

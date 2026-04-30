# Frontend Agent - Execution Protocol

Use this protocol for every web UI task.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review relevant entries in `.agent/.shared/lessons-learned.md`.
3. Confirm phase scope, target screens, and acceptance criteria.
4. Apply `../_shared/context-budget.md` to keep analysis focused.

Escalate early if UX scope, security implications, or ownership boundaries are unclear.

## Step 1: Analyze
- Identify impacted screens, components, and state flows.
- Inspect existing UI and interaction patterns before adding new ones.
- Record assumptions and unresolved questions.

## Step 2: Plan
- Define component boundaries and state ownership.
- Plan data loading, empty/loading/error states, and retry behavior.
- Plan responsive behavior and keyboard accessibility requirements.

## Step 3: Implement
- Apply changes in dependency-safe order:
   1. Types and contracts
   2. Data integration hooks/adapters
   3. Reusable components
   4. Route-level screens and flows
   5. Tests for key interactions and state transitions
- Keep visual and behavioral consistency with existing design patterns.

## Step 4: Verify
- Run `resources/checklist.md`.
- Run `../_shared/verify.sh`.
- Verify responsive behavior, keyboard navigation, and accessibility labels.
- Verify no regressions in adjacent screens.

## On Error
Use `resources/error-playbook.md` and include reproducible failure notes.

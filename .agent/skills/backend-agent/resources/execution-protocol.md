# Backend Agent - Execution Protocol

Use this protocol for every backend-facing task.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review relevant entries in `.agent/.shared/lessons-learned.md`.
3. Confirm phase scope and acceptance criteria before implementation.
4. Apply `../_shared/context-budget.md` to keep context focused.

Escalate early if security, architecture ownership, or phase scope is unclear.

## Step 1: Analyze
- Identify impacted modules, repositories, data sources, and use cases.
- Inspect existing patterns before adding new abstractions.
- Record assumptions and unresolved questions.

## Step 2: Plan
- Define request/response contracts and error mapping.
- Plan schema, migration, and indexing impact where relevant.
- Define security requirements: auth, validation, idempotency, and rate limits.

## Step 3: Implement
- Apply changes in dependency-safe order:
   1. Data models and contracts
   2. Repository and use-case logic
   3. Transport or adapter layer integration
   4. Tests for happy, failure, and edge paths
- Keep business logic out of transport adapters.

## Step 4: Verify
- Run `resources/checklist.md`.
- Run `../_shared/verify.sh`.
- Verify tests pass and regressions are not introduced.

## On Error
Use `resources/error-playbook.md` and include evidence with root cause notes.

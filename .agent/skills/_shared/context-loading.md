# Dynamic Context Loading Guide (KMP)

Do not load everything. Load only resources needed for the current phase and module scope.

## Baseline Loading Order

1. Agent SKILL.md
2. plan.md
3. .agent/rules/code-quality-gates.yaml
4. .agent/rules/security-policy.yaml (if auth, signing, secrets, or networking is involved)
5. Target module build files and changed source files

## Load by Task Type

### Architecture or planning

- plan.md
- CLAUDE.md
- module-level docs for impacted areas

### Shared/domain implementation

- cmp-shared and/or core module source
- dependency injection setup (if touched)
- tests in commonTest and platform tests when impacted

### Platform-specific implementation

- Relevant platform module(s): cmp-android, cmp-ios, cmp-desktop, cmp-web
- expect/actual boundaries and adapters
- platform-specific tests and launch wiring

### Debugging

- Failing test/log output
- recently changed files
- matching gate config when failure is Spotless/Detekt/tests/dependencyGuard

### QA/review

- Diff under review
- plan phase acceptance criteria
- code-quality-gates.yaml and security-policy.yaml

## Orchestrator Prompt Composition

1. Skill core rules
2. phase scope from plan.md
3. only task-relevant resource paths
4. expected verification commands

Prefer minimal but sufficient context so the subagent can execute deterministically.

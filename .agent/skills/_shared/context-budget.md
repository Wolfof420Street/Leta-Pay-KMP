# Context Budget Management (KMP)

Use context intentionally. Prioritize files that determine architecture, phase gates, and changed modules.

## Priority Order

1. Current request and target files
2. plan.md phase boundaries and acceptance criteria
3. .agent/rules/code-quality-gates.yaml and security-policy.yaml
4. Recent diffs in impacted modules
5. Supporting docs (CLAUDE.md, module docs)

## Suggested Budget by Task

| Task | Budget |
|---|---|
| Planning and decomposition | 12k-30k |
| Single-module implementation | 8k-20k |
| Cross-platform KMP feature | 20k-45k |
| Debug/root-cause analysis | 20k-60k |
| QA/review pass | 10k-30k |

## Load Discipline

- Load SKILL.md first, then only task-relevant shared resources
- Read changed files in larger chunks instead of many tiny reads
- Summarize completed work and avoid reloading unchanged history
- Keep one explicit list of assumptions and unresolved questions

## Escalation Rule

If context pressure is high, preserve only:
- Current phase requirements
- Quality/security gate rules
- Files being actively edited

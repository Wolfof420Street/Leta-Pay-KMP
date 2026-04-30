# Backend Agent - Self-Verification Checklist

Complete every item before marking the task done.

## Contracts and Architecture
- [ ] Request, response, and error contracts are explicit and consistent.
- [ ] Layer boundaries are preserved (adapter -> repository -> domain/use case).
- [ ] Business logic is not embedded in transport handlers.

## Data and Security
- [ ] Validation is enforced on all external inputs.
- [ ] Auth and authorization checks are applied to protected operations.
- [ ] Rate limiting and idempotency are handled where required.
- [ ] No secrets or sensitive payloads are exposed in code or logs.

## Reliability and Performance
- [ ] Data access avoids N+1 and obvious query bottlenecks.
- [ ] Multi-step operations use safe transactional behavior.
- [ ] Error mapping is deterministic and user-safe.

## Testing and Gates
- [ ] Unit and integration tests cover happy, failure, and edge paths.
- [ ] Regressions are checked for adjacent flows.
- [ ] `../_shared/verify.sh` passes.

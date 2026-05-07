# Frontend Agent - Self-Verification Checklist

Complete every item before marking the task done.

## Structure and State
- [ ] Component boundaries and state ownership are clear.
- [ ] Shared contracts are used instead of duplicating business logic in UI.
- [ ] Types and props are explicit and consistent.

## UX and Accessibility
- [ ] Loading, empty, and error states are implemented.
- [ ] Keyboard navigation and focus behavior are verified.
- [ ] Semantic markup and accessibility labels are present.
- [ ] Contrast and responsive behavior are validated.

## Performance
- [ ] Avoid unnecessary rerenders or expensive synchronous work.
- [ ] Route-level code splitting/lazy loading is applied where needed.
- [ ] Media assets are optimized.

## Testing and Gates
- [ ] Interaction and async-state transitions are tested.
- [ ] Adjacent screens are checked for regressions.
- [ ] `../_shared/verify.sh` passes.

# Frontend Agent - Tech Stack Reference

## Core Stack
- **Framework**: Compose Web 14+ (App Router), Compose 18+
- **Language**: TypeScript (strict mode)
- **Styling**: Compose theming CSS 3+ (NO inline styles)
- **Components**: Compose components, Radix UI
- **State**: Compose Context, StateFlow, or Redux Toolkit
- **Forms**: Compose Hook Form + Zod
- **API Client**: repository flows
- **Testing**: Vitest, Compose Testing Library, Playwright

## Code Standards
- Explicit TypeScript interfaces for props
- Compose theming classes only (no inline styles)
- Semantic HTML with ARIA labels
- Keyboard navigation support

## Project Structure

```
src/
  app/           # Compose Web App Router pages
  components/
    ui/          # Reusable primitives (button, card)
    [feature]/   # Feature components
  lib/
    api/         # API clients (repository flows hooks)
    hooks/       # Custom hooks
  types/         # TypeScript types
```

## Serena MCP Shortcuts
- `find_symbol("ComponentName")`: Locate existing component
- `get_symbols_overview("src/components")`: List all components
- `find_referencing_symbols("Button")`: Find usages before changes

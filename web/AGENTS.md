# Your Life One

This directory contains the browser application. The Spring services are in `../backend/`.
Preserve the existing domain ownership, API contracts, and account isolation.
Keep changes scoped to the requested task and add relevant regression coverage.

Frontend: `cd web && npm ci && npm run lint && npm run typecheck && npm test && npm run build`.
Browser tests: `cd web && npx playwright install firefox && npm run test:e2e`.
Backend: `cd backend && ./mvnw -B -ntp verify` (Java 21; Docker for integration tests).
Shared contracts: `python3 scripts/check-contracts.py` from the repository root.

Do not commit credentials, local account data, generated reports, or build outputs.

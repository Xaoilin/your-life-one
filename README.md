# Your Life One

A personal life-management application with a React browser interface and nine Spring Boot services. It includes prayer tracking, calendars, tasks and habits, projects and knowledge, inventory, employment, trips, health logs, finance and loan planning.

## Stack

- **Browser:** React 19, TypeScript 5, Vite 8, CSS, Vitest and Playwright (Firefox).
- **Services:** Java 21, Spring Boot 4.1.1, Spring Security, Spring AI MCP, MapStruct and Lombok.
- **Data and messaging:** PostgreSQL, Flyway and RabbitMQ.
- **Integrations:** Google sign-in and Calendar, prayer-time providers, OAuth-authorized domain MCP tools and server-sent events.
- **Legacy compatibility:** Supabase migrations and Edge Functions remain in `web/supabase/`; native identity and MCP are implemented in Spring.

## Repository layout

| Path | Contents |
| --- | --- |
| [`web/`](web/) | Browser application, frontend tests and API contract fixtures |
| [`backend/`](backend/) | Spring services, backend tests and matching API contract fixtures |
| [`scripts/`](scripts/) | Repository-wide contract and public-source checks |
| [`.github/workflows/`](.github/workflows/) | Active validation for both components |

The application code and dependency versions are preserved. Branding, example identities and deployment addresses are generic. The repository starts with fresh history and contains no production accounts, private screenshots, deployment receipts or credentials. The included tests use synthetic accounts and service fixtures.

## Quick start

Requirements: Node.js 22+, Java 21, and Docker for backend integration tests.

```sh
git clone https://github.com/Xaoilin/your-life-one.git
cd your-life-one/web
npm ci
npm run dev
```

Open `http://localhost:5173/yourlifeone/`. For authenticated operation, copy `web/.env.example` to `web/.env.local` and configure your own service endpoints, Google OAuth application, PostgreSQL database and RabbitMQ broker. Runtime configuration is defined by each service's `application.yml`; account data and credentials are supplied by your deployment. Browser tests run against local synthetic service responses and do not need a live account.

## Validation

```sh
cd web
npm run lint
npm run typecheck
npm test
npm run build
npx playwright install firefox
npm run test:e2e
```

```sh
cd backend
./mvnw -B -ntp verify
```

From the repository root:

```sh
python3 scripts/check-contracts.py
python3 scripts/check-public-source.py
```

CI validates both components. Runtime credentials and production deployment are deliberately operator-configured; this repository does not deploy to an existing account.

## License

Licensed under the [MIT License](LICENSE). Anyone may use, copy, modify, distribute, sublicense and sell the software, including for commercial purposes, provided the copyright and license notice are included. The software is provided without warranty.

Dependencies and separately licensed content retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

# Your Life One — browser application

React 19, TypeScript 5, Vite 8 and CSS. Account data is held by the Spring services.
See the [project README](../README.md) for installation, service ownership and licensing.

```sh
npm ci
npm run dev
```

The app uses the `/yourlifeone/` asset base. Copy `.env.example` to `.env.local` and set your own service endpoints for authenticated use. Browser E2E tests use synthetic local service responses instead of a production account.

```sh
npm run lint
npm run typecheck
npm test
npm run build
npx playwright install firefox
npm run test:e2e
```

The workflows inside this directory document the application's component-level validation; the active monorepo CI is in the root `.github/workflows/` directory. Deployment requires an operator's own configuration and credentials.

License: [MIT](../LICENSE). See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for separately licensed content.

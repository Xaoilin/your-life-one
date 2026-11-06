# Your Life One — Spring services

Java 21 and Spring Boot 4.1.1, with PostgreSQL, Flyway, RabbitMQ, Spring Security, Spring AI MCP, MapStruct and Lombok.

| Service | Responsibility | Data schemas |
| --- | --- | --- |
| Access | Google sign-in, browser sessions, OAuth and domain MCP endpoints | `identity`, shared public approval tables |
| Profile | Account settings, integrations, product usage | `profile`, `activity` |
| Prayer | Timetables, recorded outcomes, reminders and competition summaries | `prayer` |
| Calendar | Calendar accounts, sources, events and encrypted provider credentials | `calendar` |
| Life admin | Inventory, trips, health and employment | `inventory`, `trips`, `health`, `jobs`, `life` |
| Knowledge | Knowledge base, lifestyle, projects and wiki | `knowledge`, `lifestyle`, `projects`, `knowledge_service` |
| Planner | Tasks, habits, goals, XP, streaks, daily momentum and clock | `tasks`, `gamification`, `clock`, `planner_service` |
| Finance | Accounts, transactions, budgets, banking review, loans and equity | `finance`, `equity`, `finance_service` |
| Live | Account-scoped server-sent events | None |

`common-service` is a shared library. Planner reads Prayer outcomes and Knowledge progress through their authenticated APIs. Access forwards approved MCP operations to Life admin, Finance and Knowledge. Domain writes use per-service RabbitMQ command queues and publish change events after commit; Live forwards these events to open browser tabs.

```sh
./mvnw -B -ntp verify
```

Java 21 and Docker are required for the full integration suite. Tests provision synthetic PostgreSQL/RabbitMQ containers. Service `src/main/resources/application.yml` files define runtime configuration; credentials and signing keys must be supplied by the operator. No production account, deployment credentials or hosted infrastructure is included.

The `contracts/` fixtures are shared with `../web/contracts/` and checked by the root CI. The nested `.github/` workflow is retained as component source; the root workflow controls this repository's CI.

License: [MIT](../LICENSE).

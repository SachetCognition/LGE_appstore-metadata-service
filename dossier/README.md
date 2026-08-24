# LGE AppStore Platform — Reverse-Engineering & Consolidation Dossier

This dossier documents the LGE AppStore platform as reverse-engineered from source, for use by the
forward-engineering team as the base to rationalise and consolidate the services.

## Scope

In-scope repositories (source inspected directly):

| Repo | Branch | Role |
|---|---|---|
| `SachetCognition/LGE_appstore-metadata-service` | `master` | ASMS — application-metadata source of truth (Spring Boot, jOOQ, Flyway, PostgreSQL) |
| `SachetCognition/LGE_appstore-bundle-service` | `main` | ASBS — bundle generation/encryption orchestration (Spring Boot, jOOQ, Flyway, PostgreSQL, RabbitMQ, Resilience4j) |
| `SachetCognition/LGE_appstore-caching-service` | `main` | Nginx edge cache / reverse proxy for bundle delivery |

Out of scope but referenced: **`asbm-backend`** ("App Store Bundle Manager", name inferred) — an external
service referenced only by the caching service's Nginx template (`upstream asbm-backend`, routes
`/platforms` and `/bundles`). It has no repository in this set; see
[10-open-questions.md](10-open-questions.md).

## Legend

- **[VERIFIED]** — confirmed directly against source/config/tests in the repos above.
- **[INFERRED]** — reasoned from code or architecture, not directly confirmed in these repos.
- Citations use the form `repo path:lines`.

## Index

| Doc | Contents |
|---|---|
| [00-overview-and-scope.md](00-overview-and-scope.md) | Scope, in/out-of-scope services, known unknowns, legend |
| [01-hld.md](01-hld.md) | High-Level Design: context diagram, tech stack per service, cross-service data flows |
| [02-lld.md](02-lld.md) | Low-Level Design per service: REST surfaces, ERDs, state machine, business rules, Nginx analysis |
| [03-processes-L1-L4.md](03-processes-L1-L4.md) | Process decomposition L1 (value chain) → L4 (step/branch/error flows) |
| [04-business-journeys.md](04-business-journeys.md) | End-to-end journeys: publish/update/delete, discover, download (hit/miss/generate/encrypt), failure/retry |
| [05-urs.md](05-urs.md) | User Requirements Specification (URS-M/S/B/C/X) traced to code |
| [06-test-cases.md](06-test-cases.md) | Reverse-derived test catalogue reconciled against existing suites; coverage gaps |
| [07-capability-matrix.md](07-capability-matrix.md) | Cross-repo capability overlap matrix (Full/Partial/Absent) |
| [08-fit-gap.md](08-fit-gap.md) | Overlaps/dedupe candidates, divergent rules, unique capabilities, uncovered gaps |
| [09-consolidation-recommendation.md](09-consolidation-recommendation.md) | Anchor-repo recommendation, target shape, migration backlog |
| [10-open-questions.md](10-open-questions.md) | Items for the forward-engineering team to confirm |

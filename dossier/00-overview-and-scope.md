# 00 — Overview and Scope

Dossier index: [README.md](README.md) · Siblings: [01-hld.md](01-hld.md) · [02-lld.md](02-lld.md) · [03-api-contracts.md](03-api-contracts.md) · [04-data-model.md](04-data-model.md) · [05-messaging-and-async.md](05-messaging-and-async.md) · [06-deployment-and-ops.md](06-deployment-and-ops.md) · [07-testing.md](07-testing.md) · [08-consolidation-analysis.md](08-consolidation-analysis.md) · [09-risks-and-gaps.md](09-risks-and-gaps.md) · [10-open-questions.md](10-open-questions.md)

## Purpose

This dossier is a reverse-engineering record of the LGE AppStore platform, produced by reading the source of the three available repositories. Its goals are:

1. Describe what each service actually does today (not what documentation or naming suggests it does).
2. Make the cross-service contracts explicit: REST paths, HTTP semantics, database schemas, RabbitMQ queues and message payloads, and the filesystem hand-off for generated bundles.
3. Provide a basis for consolidation analysis (see [08-consolidation-analysis.md](08-consolidation-analysis.md)) by highlighting duplicated concerns, behavioural quirks, and gaps in configuration/coverage.

Everything is derived from code in the repositories listed below. Where behaviour depends on components that are *not* in these repositories (bundle generator, bundle cryptor, API gateway, `asbm-backend`), that is stated explicitly rather than assumed.

## In-scope repositories

| Repo short name (used in citations) | GitHub repository | Branch read |
| --- | --- | --- |
| `appstore-metadata-service` | `SachetCognition/LGE_appstore-metadata-service` | `master` |
| `appstore-bundle-service` | `SachetCognition/LGE_appstore-bundle-service` | `main` |
| `appstore-caching-service` | `SachetCognition/LGE_appstore-caching-service` | `main` |

**appstore-metadata-service (ASMS)** — Spring Boot service that is the source of truth for application metadata. It exposes a read-only STB perspective (`GET /apps`, `GET /apps/{appId}`) and a maintainer perspective (CRUD on `/maintainers` and `/maintainers/{maintainerCode}/apps`), persists to PostgreSQL through jOOQ with Flyway-managed schema, and computes derived facts such as which application version is "latest" for STBs versus for maintainers, and which version is "preferred". It also synthesises the download URL clients use for native (DAC) applications. [VERIFIED] `appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/stb/StbAppsController.java:42-96`, `appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/MaintainerAppsController.java:46-150`, `appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/V00__Initial_schema.sql:20-62`.

**appstore-bundle-service (ASBS)** — Spring Boot service that orchestrates bundle generation and encryption. It exposes exactly one REST endpoint, `GET /applications/{appId}/{appVersion}/{platformName}/{firmwareVersion}/{appBundleName}`, which validates the application against ASMS, records a bundle row in its own PostgreSQL schema, publishes a generation request to RabbitMQ, and answers `202 Accepted` with a `Retry-After` header. It consumes status feedback from RabbitMQ and, when generation completes and encryption is enabled, publishes an encryption request. Calls to ASMS are wrapped in a Resilience4j circuit breaker and bulkhead. [VERIFIED] `appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/resources/AppStoreBundleController.java:58-115`, `appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/configuration/RabbitMQConsumersConfiguration.java:68-122`.

**appstore-caching-service** — Nginx edge/reverse proxy packaged as a container plus Helm chart. It serves already-generated (encrypted) bundle files straight from a read-only NFS volume shared with the bundle generator, falling back to ASBS when the file is not on disk yet, and proxies `/platforms` and `/bundles` to the `asbm-backend` upstream. It also injects/propagates `x-request-id`, normalises 5xx responses to a JSON `ErrorResponse`, and exposes health endpoints on a separate port. [VERIFIED] `appstore-caching-service appstore-caching-service-nginx/default.conf.template:20-151`, `appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:68-105`.

## Out of scope: the `asbm-backend` dependency

The caching service declares a second upstream, `asbm-backend`, fed by the environment variable `${ASBM_SERVICE}`, and routes every request matching `^/(platforms|bundles)` to it with permissive CORS headers. [VERIFIED] `appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26,50-87`.

No repository for this service is in scope, and nothing in the three repositories implements `/platforms` or `/bundles`. The name is commonly read as "App Store Bundle Manager". [INFERRED] — the expansion is not written anywhere in the three repositories; the only in-repo gloss is the caching-service knowledge index, not source.

It is flagged rather than documented because:

- Its API, data model, and authentication are unknown, so the platform's external surface cannot be fully described from these repositories.
- The caching service will fail to start (or fail to resolve the upstream) if `ASBM_SERVICE` is unset — and the Helm chart's `configMap` defines `ASBS_SERVICE` but **not** `ASBM_SERVICE`. [VERIFIED] `appstore-caching-service helm/appstore-caching-service/values.yaml:30-34` vs `appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26`. See [09-risks-and-gaps.md](09-risks-and-gaps.md).
- Any consolidation plan that touches the caching service's routing has to account for a consumer whose contract cannot be verified here.

## Known unknowns (summary)

The full list, with the evidence that raised each one, lives in [10-open-questions.md](10-open-questions.md). The main categories:

- **Bundle generation and encryption workers.** ASBS publishes to `bundlegen-service-requests` / `bundlecrypt-service-requests` and consumes `bundlegen-service-status` / `bundlecrypt-service-status`, but no component in these repositories consumes the request queues or produces the status messages. [VERIFIED] `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:29-32`.
- **Authentication and authorization.** ASMS never reads the `x-maintainer-id` header in code; the OpenAPI spec documents it as `required: false` and set "by intermediate proxies/api gateways". The only in-repo example of such a proxy is the local docker-compose `as3proxy`, which hardcodes the value behind HTTP basic auth. [VERIFIED] `appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:194-200`, `appstore-metadata-service appstore-metadata-service/docker-compose/as3proxy/nginx.conf:57-64`.
- **`asbm-backend`** — see above.
- **Bundle storage topology.** ASMS builds native application URLs from `BUNDLES_STORAGE_PROTOCOL`/`BUNDLES_STORAGE_HOST`, but nothing in these repositories ties that host to the caching service or to the NFS volume the caching service reads. [VERIFIED] `appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/config/BeanConfiguration.java:36-48`.

## Legend and conventions

**Finding labels**

- `[VERIFIED]` — the statement was confirmed by reading the cited source in the checked-out branch. Line numbers refer to that revision.
- `[INFERRED]` — a reasoned conclusion that is *not* directly stated by any source read here (for example, deductions about components outside these repositories, or about operational intent). Treat as a hypothesis.

Statements with no label are structural/navigational text, not findings.

**Citation format**

`repo-short-name path/inside/repo:lines`, for example:

```
appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/stb/StbAppsController.java:53-76
```

The first token is one of `appstore-metadata-service`, `appstore-bundle-service`, `appstore-caching-service`; the rest is the path relative to that repository's root. Line ranges are inclusive and may be a comma-separated list when a claim rests on several regions of one file.

**Diagrams** are GitHub-flavoured Mermaid with no colours or styling; all node and edge labels are quoted.

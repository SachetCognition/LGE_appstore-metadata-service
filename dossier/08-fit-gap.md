# 08 — Fit-Gap Report

Part of the [LGE AppStore consolidation dossier](README.md). Read with
[07-capability-matrix.md](07-capability-matrix.md) (what exists where) and
[09-consolidation-recommendation.md](09-consolidation-recommendation.md) (what to do about it).
Unresolved items are carried into [10-open-questions.md](10-open-questions.md).

Labels: **[VERIFIED]** = read in source; **[INFERRED]** = reasoned, not directly confirmed. Citations use
the form `repo path:lines`.

---

## (a) Overlapping functionality / dedupe candidates

### A1. Correlation-ID filter duplicated verbatim — **strong dedupe candidate** [VERIFIED]

`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/filter/CorrelationIdFilter.java:33-61`
and
`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/filters/CorrelationIdFilter.java:34-59`
are the same class in two packages: same header name `x-request-id`, same MDC key `correlationId`, same
"generate a UUID when blank", same response echo, same `finally { MDC.remove(...) }`. The only difference is
visibility of the header constant (`public` in ASBS because the exception handler imports it —
`appstore-bundle-service .../error/handler/GlobalExceptionHandler.java:56`). A third implementation exists in
Nginx (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:28-31,128`), which is
appropriate at the edge and should stay.

### A2. Error-response normalisation implemented three times, in **two incompatible shapes** [VERIFIED]

| Where | Shape |
|---|---|
| ASMS | flat `{"message": "..."}` — `appstore-metadata-service .../api/error/GlobalExceptionHandler.java:43-153`, schema `appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:1052-1058`; plus `CustomErrorController` for container-level errors (`.../api/error/CustomErrorController.java:35-53`) |
| ASBS | nested `{"error":{"httpStatusCode":…,"message":…,"details":…,"correlationId":…}}` — `appstore-bundle-service .../error/handler/GlobalExceptionHandler.java:39-93`, schema `appstore-bundle-service appstore-bundle-service-application/src/main/resources/static/appstore-bundle-service.yaml:80-86` |
| caching | hardcoded string literal duplicating the ASBS shape for 5xx only — `appstore-caching-service appstore-caching-service-nginx/default.conf.template:131-134` |

Two dedupe actions, not one: (i) collapse the two Java handlers onto one shared advice class, and (ii) pick a
single wire shape. Note the Nginx literal will silently drift from the Java model, since nothing tests it
against the ASBS schema [INFERRED].

### A3. Bundle path/name derivation implemented independently in ASMS and ASBS [VERIFIED]

ASMS builds the download URL as
`%s://%s/%s/%s/%s/%s/%s-%s-%s-%s.tar.gz`
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/util/ApplicationUrlCreator.java:26-27,48-60`),
i.e. `{appId}/{version}/{platform}/{firmware}/{appId}-{version}-{platform}-{firmware}.tar.gz`, from
`BUNDLES_STORAGE_PROTOCOL`/`BUNDLES_STORAGE_HOST`
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/config/BeanConfiguration.java:36-48`).

ASBS re-derives the *same* layout for the encryption message:
`String.format("/%s/%s/%s/%s/%s", appId, version, platform, firmware, buildBundleName(...))` with
`buildBundleName` = `%s-%s-%s-%s.%s`
(`appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/model/EncryptionMessageFactory.java:45-56`).
The extension is a *separate* configuration key `bundle.extension` (default `tar.gz`)
(`appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:4`),
while ASMS hardcodes `.tar.gz` in the format string. Changing `BUNDLE_EXTENSION` therefore breaks the ASMS-published
URL with no compile-time or test-time signal [VERIFIED — the divergence is structural; the runtime break is
[INFERRED]]. The caching service is the third consumer of the same layout, via
`rewrite ^\/(.+)$ /applications/$1`
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:126`).

### A4. Duplicated ASMS domain models (no shared library) [VERIFIED]

ASBS hand-maintains its own copies of the ASMS contract (`ApplicationMetadata`, `ApplicationMetadataForMaintainer`,
`Header`, `HeaderForMaintainer`, `Maintainer` in
`appstore-bundle-service appstore-bundle-service-external/appstore-metadata-service-client/src/main/java/com/lgi/appstorebundle/external/asms/model/`),
and hardcodes ASMS URL templates as string constants
(`.../external/asms/AppstoreMetadataServiceClient.java:56-58`). ASMS generates its models from its own OpenAPI
spec. Nothing verifies the two stay compatible.

### A5. Two PostgreSQL schemas both keyed on the same application identity [VERIFIED]

ASMS `application` (schema `appstore_metadata_service`,
`appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/V00__Initial_schema.sql:1-62`,
plus `size`, OCI image url, `encryption`, `preferred` in V02–V05) and ASBS `bundle` (schema
`appstore_bundle_service`,
`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/resources/migration/V1__Add_schema.sql:22-33`)
share the compound business key `(application_id, application_version, platform_name, firmware_version)` and
both carry an `encryption boolean` column
(`appstore-metadata-service .../db/migration/V04__Add_encryption_column.sql:20`;
`appstore-bundle-service .../migration/V2__Add_encryption_column.sql:20`). There is no FK and no referential
integrity across them — deleting an app in ASMS leaves orphan bundle rows [VERIFIED: no cross-schema
constraint exists; the orphan consequence is [INFERRED]].

### A6. Duplicated Helm and CI scaffolding [VERIFIED]

All three repos carry the same six GitHub Actions workflows (`build`, `test`, `release`, `set-version`,
`helm-release`, `static`) and near-identical Helm template sets (configmap/deployment/service/ingress; caching
adds `pvc.yaml`, ASMS/ASBS add `sealedsecret.yaml`). The `test.yaml` workflows are `workflow_call` wrappers
around `mvn verify` (`appstore-caching-service .github/workflows/test.yaml:1-22`). This is copy-paste
infrastructure, extractable to a shared reusable-workflow repo and a Helm library chart.

### A7. Two Prometheus/actuator configuration blocks with different exposure sets [VERIFIED]

`appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:16-23`
vs `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:68-74`.
Same intent, and the second is a superset (see D2 below).

---

## (b) Divergent business rules

### B1. Encryption decision uses two different sources at two points in the same flow [VERIFIED]

- **Request time:** `encrypt && applicationMetadataForMaintainer.getHeader().getEncryption()` — the global
  toggle ANDed with the flag *just fetched from ASMS*
  (`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/util/EncryptionHelper.java:43-45`),
  persisted onto the bundle row
  (`appstore-bundle-service .../resources/AppStoreBundleController.java:103-108,117-123`;
  `appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:85,96`).
- **Feedback time:** `encrypt && bundleService.isEncryptionEnabled(feedbackMessage.getId())` — the global
  toggle ANDed with the *persisted* column
  (`appstore-bundle-service .../util/EncryptionHelper.java:47-49`;
  `.../service/BundleService.java:91-95`;
  `appstore-bundle-service .../JooqBundleDao.java:128-133`), evaluated when `GENERATION_COMPLETED` arrives
  (`appstore-bundle-service .../configuration/RabbitMQConsumersConfiguration.java:98-106`).

The persisted-flag read makes the feedback path immune to an ASMS flag flip mid-generation, which is arguably
correct, but it also means the global `bundle.encryption.enabled` toggle
(`appstore-bundle-service .../config/application.properties:3`) is re-evaluated at feedback time while the
per-app flag is not. A restart with the global toggle flipped between request and feedback yields a bundle
whose persisted intent and actual treatment disagree [VERIFIED for the code paths; the drift scenario is
[INFERRED]]. Also note `isEncryptionEnabled(UUID)` returns `false` when the bundle row is missing
(`.../service/BundleService.java:91-95`) — a lookup failure is indistinguishable from "encryption off".

### B2. `latest` means two different things depending on the caller [VERIFIED]

The `latest` JSONB column (`appstore-metadata-service .../db/migration/V00__Initial_schema.sql:46`) holds
`{"stb": bool, "maintainer": bool}` and the two perspectives query different keys:

- STB reads: `latest -> 'stb' = 'true' OR preferred = true`
  (`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/stb/PersistentAppsService.java:152-153,351-352`).
- Maintainer reads: `latest -> 'maintainer' = 'true' OR preferred = true`
  (`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/PersistentAppsService.java:131-132,311-312`); the
  write paths gate on `latest -> 'maintainer' = 'true'` alone, without the `preferred` disjunct
  (`.../api/maintainer/PersistentAppsService.java:417,516,612`).

Both flags are recomputed transactionally on write, and the code explicitly handles the case where the
maintainer-latest and stb-latest rows differ
(`appstore-metadata-service .../api/maintainer/PersistentAppsService.java:540-589`), writing
`{"stb": false, "maintainer": true}` / `{"stb": true, "maintainer": false}` accordingly. The consequence:
a maintainer's `GET` and an STB's `GET` for the same appId can legitimately return different versions. Any
consolidation that "simplifies" `latest` to a single boolean would be a behavioural regression. One further
inconsistency: the predicate is hand-written SQL text in every case, and line 311 uses the `->>` (text)
operator where every sibling predicate uses `->` (json)
(`appstore-metadata-service .../api/maintainer/PersistentAppsService.java:311` vs `:131,417,516,612` and
`.../api/stb/PersistentAppsService.java:152,351`) [VERIFIED]. Both happen to compare against the literal
`'true'`, so they agree today, but the asymmetry is unintended [INFERRED].

### B3. `getLatestBundle` returns the **oldest** matching row [VERIFIED]

```java
.orderBy(coalesce(BUNDLE.UPDATED_AT, BUNDLE.CREATED_AT))   // ASC — no .desc()
.limit(1)
```
(`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:54-63`).
jOOQ's default ordering is ascending, so for an app/version/platform/firmware with several bundle rows the
method named `getLatestBundle` returns the *first ever* row. The caller uses it to decide whether to start a
new generation
(`appstore-bundle-service .../resources/AppStoreBundleController.java:99-110` — `maybeBundle.filter(IS_NOT_BUNDLE_ERROR).isEmpty()`),
so a stale non-error row suppresses regeneration and a stale `BUNDLE_ERROR` row triggers an unnecessary one
[VERIFIED for the ordering; the behavioural consequence is [INFERRED]]. See
[10-open-questions.md](10-open-questions.md) Q7 — bug vs. deliberate is unresolved.

Note this is *reachable*: `x_request_id` is `UNIQUE` but the business key is not
(`appstore-bundle-service .../migration/V1__Add_schema.sql:22-33`), so multiple rows per business key are
expected by design.

### B4. Feedback messages are silently dropped, not dead-lettered [VERIFIED]

A feedback message without `messageTimestamp`, or with an unparseable `phaseCode`, is logged at WARN and
acknowledged
(`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/configuration/RabbitMQConsumersConfiguration.java:92-96,113-124`
— the ack decorator runs unconditionally after the handler). Combined with `updateBundleStatusIfNewer`'s
`message_timestamp < newTimestamp` guard
(`appstore-bundle-service .../JooqBundleDao.java:113-125`), out-of-order and malformed messages both end as
no-ops with no operator-visible artefact beyond a log line. The rest of the platform has no equivalent
silent-drop rule.

### B5. Correlation ID: optional in ASMS, mandatory in ASBS [VERIFIED]

ASMS generates one when absent (`appstore-metadata-service .../api/filter/CorrelationIdFilter.java:45-48`).
ASBS's controller declares `@RequestHeader(CORRELATION_ID) String xRequestId` with no `required = false` and
no default (`appstore-bundle-service .../resources/AppStoreBundleController.java:90`), so a direct call
without `x-request-id` fails before reaching the handler — the ASBS filter's generated ID does not satisfy
the binding [VERIFIED for the declaration; the failure mode is [INFERRED]]. In production this is masked
because Nginx always sets the header
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:28-31,128`), which makes ASBS
effectively un-callable except through the edge.

---

## (c) Unique capabilities to preserve per repo

### ASMS — preserve

1. The whole application/maintainer domain model and its 6-step migration history, including `latest` JSONB,
   `preferred`, `size`, OCI image url and `encryption`
   (`appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/`) [VERIFIED].
2. Dual-perspective query semantics (STB vs maintainer) and `ApplicationPreferredHelper` tie-breaking
   (`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/util/ApplicationPreferredHelper.java:34-78`) [VERIFIED].
3. JSONB containment filtering for platform/category and the type converters that make query params
   type-safe (`appstore-metadata-service .../api/converter/`) [VERIFIED].
4. `PlatformAndVersionOptionalForWebValidator` — platformName/firmwareVer mandatory for native apps only
   (`appstore-metadata-service .../config/BeanConfiguration.java:60-63`) [VERIFIED].
5. The complete 1082-line OpenAPI contract — the only full API specification in the platform
   (`appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml`) [VERIFIED].
6. The Spock functional-test module, incl. real-environment sanity/smoke tiers
   (`appstore-metadata-service appstore-metadata-service-tests/src/test/groovy/com/lgi/appstore/metadata/test/cases/`) [VERIFIED].

### ASBS — preserve

1. The bundle state machine and its DB-level `CHECK` constraint
   (`appstore-bundle-service appstore-bundle-service-api/src/main/java/com/lgi/appstorebundle/api/model/BundleStatus.java:23-31`;
   `.../migration/V1__Add_schema.sql:28`) [VERIFIED].
2. Idempotent-ish `202 + Retry-After` polling contract
   (`appstore-bundle-service .../resources/AppStoreBundleController.java:99-115`; `http.retry.after` default `30s`
   at `.../config/application.properties:1`) [VERIFIED].
3. Resilience4j circuit-breaker + bulkhead decoration of every ASMS call, with `RecoverableException` excluded
   from the breaker
   (`appstore-bundle-service appstore-bundle-service-external/client-common/src/main/java/com/lgi/appstorebundle/common/r4j/AsmsClientInvoker.java:34-86`;
   config keys `asms.r4j.*` at `.../config/application.properties:14-27`) [VERIFIED].
4. Split read/write Hikari pools (`.../config/application.properties:37-59`) [VERIFIED].
5. Monotonic status updates via `updateBundleStatusIfNewer`
   (`appstore-bundle-service .../JooqBundleDao.java:113-125`) [VERIFIED].
6. Testcontainers ITs covering the RabbitMQ round trip
   (`appstore-bundle-service appstore-bundle-service-test/src/test/java/com/lgi/appstorebundle/test/tests/ConsumeFeedbackMessageMockedIT.java`) [VERIFIED].

### caching-service — preserve

1. File-first delivery with backend fallback — the only place bundle bytes are served
   (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:35-36,126-128`) [VERIFIED].
2. Edge correlation-ID minting (`:28-31,128`) [VERIFIED].
3. The only CORS policy in the platform (`:50-125`) [VERIFIED].
4. Separated admin port 8081 with `/healthcheck`, `/ping`, `deny all` on `/` (`:137-151`) [VERIFIED].
5. Read-only NFS PVC mount of the bundle-generator volume
   (`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:77-78,98-102`;
   `appstore-caching-service helm/appstore-caching-service/values.yaml:41-45`) [VERIFIED].
6. The Nginx behaviour ITs — the only executable spec of the edge
   (`appstore-caching-service appstore-caching-service-test/src/test/java/com/lgi/appstore/cache/test/mocked/NginxITCase.java:27-98`) [VERIFIED].

---

## (d) Gaps no repo covers

### D1. No authN/authZ anywhere in code [VERIFIED]

`x-maintainer-id` is declared `required: false` on every maintainer-perspective operation and documented as
"Value should be set by intermediate proxies/api gateways" — on one operation extended with "Used for
authentication/authorization purposes"
(`appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:194-200,377-383,466-472`).
No controller reads it and no filter validates it: the maintainer CRUD endpoints authorise on the path
variable `maintainerCode` alone
(`appstore-metadata-service .../api/maintainer/MaintainerAppsController.java:47-150`). The only enforcement
in any repo is the dev docker-compose proxy, which hardcodes the value behind basic auth
(`appstore-metadata-service appstore-metadata-service/docker-compose/as3proxy/nginx.conf:57-64`); the sibling
`asmsproxy` adds basic auth but no maintainer header at all
(`appstore-metadata-service appstore-metadata-service/docker-compose/asmsproxy/nginx.conf:57-63`). ASBS's
bundle endpoint has no authentication at all
(`appstore-bundle-service .../resources/AppStoreBundleController.java:84-115`), and the caching service's data
plane is open (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:33-135`).
**Consequence:** anyone with network reach to ASMS can mutate any maintainer's applications. The production
gateway that is supposed to close this gap is not in any in-scope repo [VERIFIED that it is absent here].

### D2. No health endpoint in ASMS, and no Kubernetes probes anywhere [VERIFIED]

ASMS sets `management.endpoints.enabled-by-default=false` and exposes only `info,prometheus`
(`appstore-metadata-service .../config/application.properties:16-22`) — there is no `health` endpoint at all,
whereas ASBS exposes `health,info,prometheus`
(`appstore-bundle-service .../config/application.properties:71`). Independently, **none** of the three Helm
charts define a `livenessProbe` or `readinessProbe`, so the caching service's purpose-built 8081
`/healthcheck` and `/ping` endpoints
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:137-151`) are never consulted
by the platform
(`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:68-97`).

### D3. No cache invalidation on bundle regeneration or re-encryption [VERIFIED]

The edge serves whatever file is on the PVC via `try_files`, with no `proxy_cache` zone, no TTL, and no purge
location (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:35-36`); the PVC is
mounted `readOnly: true`
(`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:98-102`). ASBS never
touches the filesystem — it only publishes messages
(`appstore-bundle-service .../service/BundleService.java:60-89`) — and there is no eviction/purge code path in
any repo. **Consequence:** once a bundle file exists at its deterministic path, the edge will keep serving it
and will never reach ASBS again for that path, so a regenerated or re-encrypted bundle is invisible to clients
until something outside these repos deletes the file [VERIFIED that no eviction path exists; the staleness
consequence is [INFERRED]]. Compounding it, `getLatestBundle`'s ascending order (B3) means ASBS may also
decline to regenerate.

### D4. No shared domain library [VERIFIED]

See A4. Three components agree on a wire contract (bundle path layout, error shape, `x-request-id`, the
application/maintainer DTOs) with zero shared, versioned artefact and zero contract test between them. The
only cross-repo verification that exists is WireMock-stubbed ASMS responses inside ASBS's ITs
(`appstore-bundle-service appstore-bundle-service-test/src/test/java/com/lgi/appstorebundle/test/utils/ASMSMockSteps.java`)
— stubs written by hand in the consumer, i.e. not a contract test [VERIFIED].

### D5. `asbm-backend` is unowned and undocumented [VERIFIED]

It appears only as `upstream asbm-backend { server ${ASBM_SERVICE}; }` and as the target of
`location ~ ^/(platforms|bundles)`
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26,50-87`). No repo, no
OpenAPI spec — the caching service's own spec documents only the bundle GET
(`appstore-caching-service appstore-caching-service-nginx/appstore-caching-service.yaml:26-27`), so
`/platforms` and `/bundles` are undocumented routes. No test in `NginxITCase` exercises them
(`appstore-caching-service appstore-caching-service-test/src/test/java/com/lgi/appstore/cache/test/mocked/NginxITCase.java:27-98`).

### D6. `ASBM_SERVICE` is missing from the caching-service Helm values [VERIFIED]

The configMap block defines `ASBS_SERVICE`, `ENCRYPTED_BUNDLES_PATH`, `API_URL` and
`DNS_RESOLVER_CONFIGURATION` — but **not** `ASBM_SERVICE`
(`appstore-caching-service helm/appstore-caching-service/values.yaml:30-34`), while the container consumes the
configMap wholesale via `envFrom`
(`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:86-88`). Since the
template substitutes `${ASBM_SERVICE}` into an `upstream` block, a default install cannot produce a valid
`upstream asbm-backend` stanza unless the value is supplied out-of-chart [VERIFIED that the key is absent;
the startup consequence is [INFERRED]].

### D7. Nobody declares the RabbitMQ topology [VERIFIED]

ASBS publishes to the **default exchange** using the queue name as routing key
(`appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/external/ManagedRabbitMQ.java:103`)
and consumes two `-status` queues by name
(`appstore-bundle-service .../configuration/RabbitMQConsumersConfiguration.java:68-90`;
names at `.../config/application.properties:29-32` and `appstore-bundle-service helm/appstore-bundle-service/values.yaml:46-49`).
No code calls `queueDeclare`/`exchangeDeclare`; the only declaration in the repo is four manual
`rabbitmqadmin declare queue` commands in the README
(`appstore-bundle-service README.md:16-19`). **Consequence:** `basicConsume` on a non-existent queue fails, so
production start-up depends on an unowned, undocumented provisioning step [VERIFIED for the code; the
start-up failure is [INFERRED]].

### D8. The generation and encryption workers are outside the platform [VERIFIED]

`bundlegen-service-*` and `bundlecrypt-service-*` queues have no consumer/producer counterpart in any
in-scope repo (`appstore-bundle-service .../config/application.properties:29-32`). The entire artefact-producing
half of the value chain — what actually writes files to the PVC the caching service serves — is invisible here.

### D9. No bundle retention or cleanup on the shared PVC [VERIFIED]

Nothing in the three repos deletes bundle files or `bundle` rows: no scheduler, no TTL, no cleanup migration
(`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:53-133` has
no delete method; the caching PVC is read-only). Growth of both the `bundle` table and the NFS volume is
unbounded from the platform's point of view [VERIFIED].

### D10. No cross-service referential integrity or deletion propagation [VERIFIED]

Deleting an application in ASMS
(`appstore-metadata-service .../api/maintainer/MaintainerAppsController.java:129-149`) emits no event and has
no path to ASBS or to the PVC (ASMS has no messaging — see
[07-capability-matrix.md](07-capability-matrix.md) row 12). Bundle rows and bundle files for a deleted
application persist indefinitely.

---

## Priority reading

The items that most constrain a consolidation are **B2** (dual `latest` semantics — a real domain rule that
must survive), **A3 + D3** (the bundle path is a de-facto shared contract with no owner and no invalidation),
**D5/D6** (`asbm-backend` must be characterised before any target architecture is fixed) and **D1** (a security
gap that consolidation should close rather than inherit). These drive the ordering in
[09-consolidation-recommendation.md](09-consolidation-recommendation.md).

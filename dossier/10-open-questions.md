# 10 — Open Questions

Part of the [LGE AppStore consolidation dossier](README.md). These are the questions the in-scope source code
cannot answer. They are the direct blockers and risks behind
[09-consolidation-recommendation.md](09-consolidation-recommendation.md), and most of them originate from a gap
recorded in [08-fit-gap.md](08-fit-gap.md); coverage per repo is in
[07-capability-matrix.md](07-capability-matrix.md).

Labels: **[VERIFIED]** = read in source; **[INFERRED]** = reasoned, not directly confirmed. Citations use the
form `repo path:lines`. "Suggested owner" names a role, never a person.

---

## Q1. Who owns `asbm-backend`, and what is its API and data model?

**Why it matters.** It is a production dependency of the edge with no repository, no specification, and no
test. Any target architecture, capacity plan or incident runbook that omits it is wrong, and the consolidation
in [09-consolidation-recommendation.md](09-consolidation-recommendation.md) is explicitly gated on it.

**Evidence.** `upstream asbm-backend { server ${ASBM_SERVICE}; }` and `location ~ ^/(platforms|bundles)`
proxying to it (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26,50-87`)
[VERIFIED]. The CORS policy on that location advertises `GET, POST, OPTIONS, DELETE`
(`:53,63,70,78`), so it is a read/write API, not a read-only one [VERIFIED]. It is absent from the caching
service's own OpenAPI document, which describes only the bundle GET
(`appstore-caching-service appstore-caching-service-nginx/appstore-caching-service.yaml:26-27`) [VERIFIED], and
from `NginxITCase`
(`appstore-caching-service appstore-caching-service-test/src/test/java/com/lgi/appstore/cache/test/mocked/NginxITCase.java:27-98`)
[VERIFIED]. The name "App Store Bundle Manager" and the `/platforms` semantics are [INFERRED] from the route
names only.

**Suggested owner / next step.** Platform architecture owner: identify the deployed service behind
`ASBM_SERVICE` in each environment, locate its repository, and publish an OpenAPI spec plus its relationship to
the shared bundle volume. Until then treat every `asbm-backend` statement in this dossier as unverified.

## Q2. Who declares the RabbitMQ queues and exchanges in production?

**Why it matters.** `basicConsume` against a non-existent queue fails, so start-up of ASBS depends on a
provisioning step that exists in no repository. The merged deployable would inherit the same dependency.

**Evidence.** Publication uses the **default exchange** with the queue name as routing key —
`channel.basicPublish("", queueName, ...)`
(`appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/external/ManagedRabbitMQ.java:103`)
[VERIFIED]. No `queueDeclare` or `exchangeDeclare` call exists in the repository [VERIFIED]. Consumers attach
by name to the two `-status` queues
(`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/configuration/RabbitMQConsumersConfiguration.java:68-90`)
[VERIFIED]. Queue names come from configuration
(`appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:29-32`;
`appstore-bundle-service helm/appstore-bundle-service/values.yaml:46-49`) [VERIFIED]. The only declarations
anywhere are four manual `rabbitmqadmin declare queue` commands in the README, i.e. a developer instruction, not
a deployment artefact (`appstore-bundle-service README.md:16-19`) [VERIFIED].

**Suggested owner / next step.** Platform/infrastructure owner: state where the topology is declared (broker
config management, a Helm hook, an operator, or manual runbook), then either commit it as code or reference it
from the chart. Confirm whether durability, DLX and TTL are set anywhere — none are set in code [VERIFIED].

## Q3. Who owns the bundle generation and encryption workers?

**Why it matters.** They are the half of the value chain that actually produces the artifacts the edge serves.
Nothing in this dossier can describe the failure modes, retry behaviour, or file-writing conventions of the
component that determines whether a download succeeds.

**Evidence.** `bundlegen-service-requests` / `bundlecrypt-service-requests` have a producer but no consumer in
scope, and `-status` queues have a consumer but no producer
(`appstore-bundle-service .../config/application.properties:29-32`;
`appstore-bundle-service .../configuration/RabbitMQConsumersConfiguration.java:68-90`) [VERIFIED]. ASBS never
touches the filesystem (`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/service/BundleService.java:60-89`)
[VERIFIED], yet the edge serves files from a volume named after a *bundle-generator* claim
(`appstore-caching-service helm/appstore-caching-service/values.yaml:41-45`) [VERIFIED] — so the writer is that
service [INFERRED].

**Suggested owner / next step.** Platform architecture owner: name the two services, confirm the message
contracts against `GenerationMessage` / `EncryptionMessage`
(`appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/model/`),
and record which of them writes to the shared volume and under what path convention.

## Q4. What invalidates a cached bundle after regeneration or re-encryption?

**Why it matters.** If the answer is "nothing", then a rebuilt bundle never reaches clients — the most
user-visible correctness risk found in this review.

**Evidence.** The edge resolves requests with `try_files $uri $uri/ @backend` off `${ENCRYPTED_BUNDLES_PATH}`
with no `proxy_cache` zone, no TTL and no purge location
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:35-36`) [VERIFIED]; the volume
is mounted `readOnly: true`
(`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:98-102`) [VERIFIED]; no repo
contains eviction or delete code [VERIFIED]. Because the bundle path is deterministic
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/util/ApplicationUrlCreator.java:26-27`),
an existing file permanently shadows the ASBS fallback for that path [INFERRED].

**Suggested owner / next step.** Edge/caching owner together with the worker owner from Q3: define whether the
worker overwrites in place (in which case explain client-side caching and `Last-Modified` behaviour) or writes a
new path, and whether any operational procedure deletes files today. See
[09-consolidation-recommendation.md](09-consolidation-recommendation.md) backlog item 11.

## Q5. Is the two-source encryption decision intentional?

**Why it matters.** A bundle can be generated with one encryption intent and evaluated for encryption with
another, which silently produces an unencrypted artifact at a path clients treat as encrypted
(`ENCRYPTED_BUNDLES_PATH`).

**Evidence.** Request time ANDs the global toggle with the flag fetched live from ASMS
(`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/util/EncryptionHelper.java:43-45`),
persisting it on the bundle row
(`appstore-bundle-service .../resources/AppStoreBundleController.java:103-108`;
`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:85,96`).
Feedback time ANDs the same global toggle with the *persisted* column
(`.../util/EncryptionHelper.java:47-49`; `.../service/BundleService.java:91-95`; `.../JooqBundleDao.java:128-133`),
evaluated on `GENERATION_COMPLETED`
(`.../configuration/RabbitMQConsumersConfiguration.java:98-106`) [all VERIFIED]. Two consequences follow
[INFERRED]: (a) flipping `bundle.encryption.enabled`
(`.../config/application.properties:3`) between request and feedback changes the outcome for in-flight bundles;
(b) a missing bundle row makes `isEncryptionEnabled` return `false`, so a lookup failure is indistinguishable
from "encryption disabled" (`.../service/BundleService.java:91-95`) [VERIFIED].

**Suggested owner / next step.** ASBS service owner: confirm the intended semantics, then either read the
persisted flag at both points or re-read ASMS at both points, and make the row-missing case an explicit error
rather than `false`.

## Q6. Does the ASMS OpenAPI document match the controllers, and is it generated or hand-maintained?

**Why it matters.** It is the platform's only complete contract and the basis for consolidating the two error
shapes. If it has drifted, every downstream artefact derived from it — including this dossier — inherits the
drift.

**Evidence.** The document describes six path templates and a flat `ErrorResponse` requiring only `message`
(`appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:1052-1058`)
[VERIFIED]; the controllers are `StbAppsController` (`/apps`, `/apps/{appId:.+}`),
`MaintainerAppsController` (`/maintainers/{maintainerCode}/apps...`) and `MaintainersController` (`/maintainers`)
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/`) [VERIFIED].
The `{appId:.+}` regex in the STB path
(`appstore-metadata-service .../api/stb/StbAppsController.java`) has no counterpart in the spec's path template,
so path-parameter escaping behaviour is specified only in code [VERIFIED]. Whether models are generated from the
spec at build time or hand-written is not confirmed here [INFERRED that a schema-first setup exists, from the
static document being served via `springdoc.swagger-ui.url`
(`appstore-metadata-service .../config/application.properties:14-15`)].

**Suggested owner / next step.** ASMS service owner: run a spec-vs-implementation diff in CI (the `test.yaml`
workflow is the natural home) and record whether the spec is the source of truth or a published copy.

## Q7. Is `getLatestBundle`'s ascending order a bug?

**Why it matters.** It decides whether a new generation is triggered. If it is a bug, the platform both
suppresses needed regenerations and triggers unnecessary ones; if it is deliberate, the reason must be recorded
before anyone "fixes" it.

**Evidence.** `orderBy(coalesce(BUNDLE.UPDATED_AT, BUNDLE.CREATED_AT)).limit(1)` with no `.desc()` — jOOQ
defaults to ascending, so the method named `getLatestBundle` returns the oldest matching row
(`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:54-63`)
[VERIFIED]. Multiple rows per business key are possible because only `x_request_id` is unique
(`appstore-bundle-service .../migration/V1__Add_schema.sql:22-33`) [VERIFIED]. The caller regenerates when the
row is absent or in `BUNDLE_ERROR`
(`appstore-bundle-service .../resources/AppStoreBundleController.java:99-110`) [VERIFIED], so a stale row
governs the decision [INFERRED].

**Suggested owner / next step.** ASBS service owner: decide, add a unit test that inserts two rows and asserts
which is returned, and quantify how many business keys currently have more than one row before changing
behaviour. See [08-fit-gap.md](08-fit-gap.md) B3.

## Q8. How is `x-maintainer-id` authenticated and enforced in production?

**Why it matters.** Without an enforcing gateway, any caller reaching ASMS directly can create, update or delete
any maintainer's applications.

**Evidence.** The header is `required: false` on all maintainer operations and documented as gateway-set — on
one operation with "Used for authentication/authorization purposes"
(`appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:194-200,377-383,466-472`)
[VERIFIED]. No controller reads it; authorisation is by path variable only
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/MaintainerAppsController.java:47-150`)
[VERIFIED]. The only enforcement in scope is the dev-only compose proxy, which hardcodes a value behind basic
auth (`appstore-metadata-service appstore-metadata-service/docker-compose/as3proxy/nginx.conf:57-64`)
[VERIFIED]. ASBS's endpoint and the caching data plane are unauthenticated
(`appstore-bundle-service .../resources/AppStoreBundleController.java:84-115`;
`appstore-caching-service appstore-caching-service-nginx/default.conf.template:33-135`) [VERIFIED].

**Suggested owner / next step.** Security owner with platform architecture: identify the production gateway,
document what it validates, and confirm whether ASMS's network position prevents direct access. Then make the
header mandatory in the spec.

## Q9. Why is `ASBM_SERVICE` absent from the caching-service Helm values?

**Why it matters.** The variable is substituted into an `upstream` block, so a chart install that does not
supply it from elsewhere cannot render a valid Nginx configuration — which suggests either dead configuration or
an undocumented out-of-chart value.

**Evidence.** The configMap defines `ASBS_SERVICE`, `ENCRYPTED_BUNDLES_PATH`, `API_URL` and
`DNS_RESOLVER_CONFIGURATION` but not `ASBM_SERVICE`
(`appstore-caching-service helm/appstore-caching-service/values.yaml:30-34`) [VERIFIED], and the container takes
its environment from that configMap via `envFrom`
(`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:86-88`) [VERIFIED], while the
template references `${ASBM_SERVICE}`
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26`) [VERIFIED]. Whether
deployments override it via a values file outside the repo, or whether the `/platforms` and `/bundles` routes are
simply dead, cannot be determined here [INFERRED].

**Suggested owner / next step.** Edge/caching owner: check the deployed configMap in each environment. If the
routes are live, add the key to `values.yaml`; if they are dead, delete the upstream and the location together
with the CORS block that serves them.

## Q10. What is the retention and cleanup policy for bundles?

**Why it matters.** Both the `bundle` table and the NFS volume grow without bound as far as the in-scope code is
concerned, and neither has an owner for deletion.

**Evidence.** `JooqBundleDao` exposes no delete operation
(`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:53-133`)
[VERIFIED]; there is no scheduler or TTL migration in ASBS [VERIFIED]; the caching service mounts the volume
read-only (`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:98-102`) [VERIFIED];
and deleting an application in ASMS emits no event and touches nothing outside its own schema
(`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/MaintainerAppsController.java:129-149`)
[VERIFIED]. Orphaned bundle rows and files after an application delete are therefore expected [INFERRED].

**Suggested owner / next step.** Platform/storage owner: state the current volume capacity trend and whether any
external job prunes it; then define retention (by age, by superseded version, or on application delete) and where
it is enforced. See [09-consolidation-recommendation.md](09-consolidation-recommendation.md) backlog item 14.

---

## Q11. Secondary questions

These are smaller but still unanswerable from the code.

11.1 **Why does ASMS expose no `health` actuator endpoint, and why does no chart define probes?** ASMS exposes
only `info,prometheus` with endpoints disabled by default
(`appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:16-22`)
while ASBS exposes `health,info,prometheus`
(`appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:71`),
and the caching service's dedicated 8081 health server
(`appstore-caching-service appstore-caching-service-nginx/default.conf.template:137-151`) is referenced by no
probe (`appstore-caching-service helm/appstore-caching-service/templates/deployment.yaml:68-97`) [all VERIFIED].
Is liveness handled by an external monitor? *Owner:* platform/operations.

11.2 **Is silently acknowledging malformed feedback messages intended?** Messages lacking a timestamp or with an
unparseable phase code are logged and acked with no dead-letter
(`appstore-bundle-service .../configuration/RabbitMQConsumersConfiguration.java:92-96,113-124`) [VERIFIED]. *Owner:*
ASBS service owner.

11.3 **Is ASBS callable without going through the edge?** Its controller declares `x-request-id` as a required
`@RequestHeader` with no default
(`appstore-bundle-service .../resources/AppStoreBundleController.java:90`) [VERIFIED], while Nginx always sets
it (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:28-31,128`) [VERIFIED] — so
direct callers must supply it [INFERRED]. Is that contract deliberate and documented for clients? *Owner:* ASBS
service owner.

11.4 **Is `BUNDLE_EXTENSION` ever changed from `tar.gz`?** ASBS makes it configurable
(`appstore-bundle-service .../config/application.properties:4`) while ASMS hardcodes `.tar.gz` in its URL
pattern (`appstore-metadata-service .../util/ApplicationUrlCreator.java:27`) [VERIFIED], so the two disagree if
it is ever overridden [INFERRED]. *Owner:* whoever owns the bundle path contract after
[09-consolidation-recommendation.md](09-consolidation-recommendation.md) backlog item 7.

11.5 **Are the two PostgreSQL schemas on the same cluster, and is the `encryption` column duplication
intentional?** Both define one
(`appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/V04__Add_encryption_column.sql:20`;
`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/resources/migration/V2__Add_encryption_column.sql:20`)
with no constraint between them [VERIFIED]; ASBS additionally splits read and write connection pools by host
(`appstore-bundle-service .../config/application.properties:37-59`) whereas ASMS uses a single datasource
(`appstore-metadata-service .../config/application.properties:3-10`) [VERIFIED], which matters for any single
deployable. *Owner:* data/platform owner.

11.6 **Does anything consume ASMS's `preferred` flag outside ASMS?** It participates in both perspectives'
latest-resolution
(`appstore-metadata-service .../api/stb/PersistentAppsService.java:152-153`;
`.../api/maintainer/PersistentAppsService.java:131-132`) [VERIFIED] but ASBS's client models do not carry it
(`appstore-bundle-service appstore-bundle-service-external/appstore-metadata-service-client/src/main/java/com/lgi/appstorebundle/external/asms/model/`)
[VERIFIED]. Confirm no external consumer depends on it before any change to the semantics. *Owner:* ASMS service
owner.

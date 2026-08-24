# 04 — End-to-end business journeys

Part of the LGE AppStore reverse-engineering dossier. See [README](../README.md) and sibling docs:
[00-overview](00-overview.md) · [01-hld](01-hld.md) · [02-lld](02-lld.md) · [03-processes-L1-L4](03-processes-L1-L4.md) · [05-urs](05-urs.md) · [06-test-cases](06-test-cases.md)

Every non-trivial statement carries a `repo path:lines` citation and a `[VERIFIED]` / `[INFERRED]` label.
Repo short names: `appstore-metadata-service` (ASMS), `appstore-bundle-service` (ASBS), `appstore-caching-service` (cache/edge).

## Actors and systems

| Actor / system | Role | Status |
| --- | --- | --- |
| Maintainer | Publishes and maintains application metadata | `[VERIFIED]` via maintainer endpoints (`appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/MaintainerAppsController.java:47-151`) |
| Gateway / AS3 proxy | Terminates auth and injects `x-maintainer-id` | `[INFERRED]` — only a demo Nginx proxy exists (`appstore-metadata-service appstore-metadata-service/docker-compose/as3proxy/nginx.conf:56-63`) |
| STB | Reads metadata, downloads bundles | `[VERIFIED]` (`.../api/stb/StbAppsController.java:43-101`) |
| Cache (Nginx) | Serves bundles from `/data`, proxies misses to ASBS | `[VERIFIED]` (`appstore-caching-service appstore-caching-service-nginx/default.conf.template:33-135`) |
| ASBS | Orchestrates generation and encryption | `[VERIFIED]` (`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/resources/AppStoreBundleController.java:84-114`) |
| bundlegen / bundlecrypt workers | Build and encrypt bundles, publish feedback | `[INFERRED]` — not present in any repo |
| `asbm-backend` | Serves `/platforms` and `/bundles` | `[INFERRED]` — referenced only by the cache config; no repo in scope |

---

## Journey A — Maintainer publishes, updates and deletes an application

Flow `[VERIFIED]` from `MaintainerAppsController.java:98-151` and `appstore-metadata-service appstore-metadata-service/src/main/java/com/lgi/appstore/metadata/api/maintainer/PersistentAppsService.java:323-600`; the gateway hop is `[INFERRED]`.

```mermaid
sequenceDiagram
    actor M as "Maintainer"
    participant GW as "Gateway / AS3 proxy [INFERRED]"
    participant C as "MaintainerAppsController"
    participant S as "maintainer.PersistentAppsService"
    participant DB as "PostgreSQL (application, maintainer)"

    M->>GW: "POST /as3/maintainers/{code}/apps (basic auth)"
    GW->>C: "POST /maintainers/{code}/apps + x-maintainer-id"
    C->>S: "addApplication(code, application)"
    S->>DB: "SELECT maintainer id by code"
    alt "maintainer unknown"
        S-->>C: "MaintainerNotFoundException"
        C-->>GW: "404 ErrorResponse"
    else "maintainer known"
        S->>DB: "INSERT application row (metadata + JSONB blocks)"
        alt "duplicate app id and version"
            DB-->>S: "duplicate key"
            S-->>C: "ApplicationAlreadyExistsException"
            C-->>GW: "409 ErrorResponse"
        else "inserted"
            S->>DB: "updateApplicationsLatestField: reset latest for app id"
            S->>DB: "set latest.maintainer on highest version"
            S->>DB: "set latest.stb on highest visible version"
            S-->>C: "committed"
            C-->>GW: "201 Created"
        end
    end
    GW-->>M: "response"

    M->>GW: "PUT /maintainers/{code}/apps/{appId}:{version}"
    GW->>C: "PUT /maintainers/{code}/apps/{appId}:{version}"
    C->>S: "updateApplication(code, appId, version, body)"
    S->>DB: "UPDATE application WHERE version = :version"
    opt "body sets preferred = true"
        S->>DB: "clear preferred on other versions of the app"
        S->>DB: "set preferred = true on target version"
    end
    S->>DB: "recalculate latest.maintainer and latest.stb"
    S-->>C: "true when a row was updated"
    C-->>GW: "204 No Content, or 404 when nothing matched"
    GW-->>M: "response"

    M->>GW: "DELETE /maintainers/{code}/apps/{appId}:all"
    GW->>C: "DELETE /maintainers/{code}/apps/{appId}:all"
    C->>S: "deleteAllApplicationVersions(code, appId)"
    S->>DB: "DELETE all rows for app id"
    S-->>C: "true when rows were removed"
    C-->>GW: "204 No Content, or 404"
    GW-->>M: "response"
```

Notes:
- `PUT` with a bare id or `:latest` targets only the row flagged `latest -> 'maintainer' = 'true'`, and a non-blank `version` in the body rewrites the version of the targeted row (`PersistentAppsService.java:387-472`) `[VERIFIED]`.
- Deleting an explicit version or the latest version recalculates the latest flags; `:all` removes every row so no recalculation is needed (`PersistentAppsService.java:473-537`) `[VERIFIED]`.
- A maintainer cannot be deleted while it still owns applications (`.../api/maintainer/PersistentMaintainersService.java:133-158`) `[VERIFIED]`.
- Nothing in ASMS checks that the caller owns `{maintainerCode}`; isolation depends entirely on the gateway `[VERIFIED]` (absence of checks) / `[INFERRED]` (gateway behaviour).

---

## Journey B — STB discovers and inspects applications

```mermaid
sequenceDiagram
    actor STB as "STB"
    participant C as "StbAppsController"
    participant V as "PlatformAndVersionOptionalForWebValidator"
    participant S as "stb.PersistentAppsService"
    participant H as "ApplicationPreferredHelper"
    participant U as "ApplicationUrlService / ApplicationUrlCreator"
    participant DB as "PostgreSQL"

    STB->>C: "GET /apps?name=&category=&platform=&offset=0&limit=10"
    C->>S: "listApplications(filters, offset, limit)"
    S->>DB: "SELECT visible apps WHERE latest.stb = true OR preferred = true, filters applied"
    DB-->>S: "rows + total count"
    S->>H: "matchByPreferredVersionForListStb(rows)"
    H-->>S: "rows with non-preferred duplicates removed"
    S-->>C: "ApplicationsList + meta(offset, limit, count, total)"
    C-->>STB: "200 with list"

    STB->>C: "GET /apps/{appId}?platformName=&firmwareVer="
    C->>S: "getApplicationType(appId) or (appId, version)"
    S->>DB: "SELECT type"
    S-->>C: "AppIdWithType or empty"
    C->>V: "validate(params, appIdWithType)"
    alt "type unsupported"
        V-->>C: "UnsupportedApplicationTypeException"
        C-->>STB: "400 ErrorResponse"
    else "native app missing platformName or firmwareVer"
        V-->>C: "MandatoryFieldForNativeAppNotFound"
        C-->>STB: "400 ErrorResponse"
    else "web or android app"
        V-->>C: "platform and firmware must be ignored"
    end
    C->>S: "getApplicationDetails(appId[, version], platformName, firmwareVer)"
    S->>DB: "SELECT details WHERE version = :version OR latest.stb = true OR preferred = true"
    alt "no row"
        S-->>C: "Optional.empty"
        C-->>STB: "404 Not Found"
    else "row found"
        S->>U: "build download url"
        U-->>S: "native - generated tar.gz url from bundle storage host, web or android - stored source url"
        S-->>C: "details + versions + requirements + maintainer"
        C-->>STB: "200 with details"
    end
```

Notes `[VERIFIED]`:
- Only visible applications are ever returned to STBs, and with no `version` filter the latest-or-preferred rule applies (`.../api/stb/PersistentAppsService.java:111-220`).
- `offset` defaults to 0 and `limit` defaults to 0, where 0 means "no SQL limit", i.e. return everything (`.../api/stb/PersistentAppsService.java:111-220`).
- Native URLs are assembled from `BUNDLES_STORAGE_PROTOCOL`/`BUNDLES_STORAGE_HOST` and the pattern `"%s://%s/%s/%s/%s/%s/%s-%s-%s-%s.tar.gz"` (`.../util/ApplicationUrlCreator.java:27-60`, `.../config/BeanConfiguration.java`); web types are configuration-driven (`HTML5,LIGHTNING`) and Android also returns its source URL (`.../util/ApplicationUrlService.java`, `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:25`).

---

## Journey C — STB downloads a bundle (cache hit and cache miss)

```mermaid
sequenceDiagram
    actor STB as "STB"
    participant NX as "Nginx cache :8080"
    participant FS as "Shared volume /data/nginx (read-only)"
    participant BS as "ASBS AppStoreBundleController"
    participant ASMS as "ASMS"
    participant DB as "ASBS PostgreSQL (bundle)"
    participant MQ as "RabbitMQ"
    participant W as "bundlegen worker [INFERRED]"
    participant CW as "bundlecrypt worker [INFERRED]"

    STB->>NX: "GET /{appId}/{version}/{platform}/{firmware}/{bundleName}"
    NX->>FS: "try_files $uri $uri/"
    alt "cache hit"
        FS-->>NX: "file bytes"
        NX-->>STB: "200 bundle from volume"
    else "cache miss"
        NX->>NX: "@backend: rewrite to /applications/$1, x-request-id = client value or $request_id"
        NX->>BS: "GET /applications/{appId}/{version}/{platform}/{firmware}/{bundleName}"
        BS->>ASMS: "GET /apps/{appId}:{version}?platformName=&firmwareVer="
        ASMS-->>BS: "200 metadata (maintainer code)"
        BS->>ASMS: "GET /maintainers/{code}/apps/{appId}:{version}"
        ASMS-->>BS: "200 metadata (encryption flag, ociImageUrl)"
        BS->>DB: "getLatestBundle(appId, version, platform, firmware)"
        alt "no bundle row, or row status = BUNDLE_ERROR"
            BS->>DB: "INSERT bundle row status GENERATION_REQUESTED (new UUID, encryption flag)"
            BS->>MQ: "publish GenerationMessage to bundlegen-service-requests (x-request-id header)"
        else "bundle already in progress"
            BS->>BS: "skip generation"
        end
        BS-->>NX: "202 Accepted, Retry-After 30, x-request-id"
        NX-->>STB: "202 Accepted, Retry-After 30"

        MQ->>W: "GenerationMessage"
        W->>MQ: "feedback GENERATION_LAUNCHED then GENERATION_COMPLETED on bundlegen-service-status"
        MQ->>BS: "feedback message"
        BS->>DB: "updateBundleStatusIfNewer(id, status, messageTimestamp)"
        opt "GENERATION_COMPLETED and encryption enabled (global AND persisted column)"
            BS->>DB: "updateStatusForBundle(ENCRYPTION_REQUESTED)"
            BS->>MQ: "publish EncryptionMessage to bundlecrypt-service-requests"
            MQ->>CW: "EncryptionMessage"
            CW->>FS: "write encrypted bundle to shared volume [INFERRED]"
            CW->>MQ: "feedback ENCRYPTION_LAUNCHED then ENCRYPTION_COMPLETED"
            MQ->>BS: "feedback message"
            BS->>DB: "updateBundleStatusIfNewer"
        end

        STB->>NX: "retry GET /{appId}/{version}/{platform}/{firmware}/{bundleName} after Retry-After"
        NX->>FS: "try_files $uri $uri/"
        FS-->>NX: "file bytes"
        NX-->>STB: "200 bundle from volume"
    end
```

Notes:
- The `202` is returned unconditionally on the miss path, whether generation was newly triggered or already in flight (`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/resources/AppStoreBundleController.java:101-114`) `[VERIFIED]`.
- `Retry-After` is `http.retry.after`, default `30s`, rendered in seconds (`AppStoreBundleController.java:111-113`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:1`) `[VERIFIED]`.
- The volume is mounted read-only at `/data` with `ENCRYPTED_BUNDLES_PATH=/data/nginx` (`appstore-caching-service helm/appstore-caching-service/values.yaml:31-34`) `[VERIFIED]`; the cache therefore never writes bundles — the producing worker does `[INFERRED]`.
- Nothing in these repos maps a `bundle` DB row to a file path on the volume; the naming contract between the ASMS-generated URL and the worker output is an open question `[INFERRED]`.

---

## Journey D — Failure and retry journeys

```mermaid
sequenceDiagram
    actor STB as "STB"
    participant NX as "Nginx cache :8080"
    participant BS as "ASBS"
    participant ASMS as "ASMS"
    participant CB as "Resilience4j circuit breaker and bulkhead"
    participant DB as "ASBS PostgreSQL"
    participant MQ as "RabbitMQ"
    participant W as "bundlegen worker [INFERRED]"

    Note over STB,ASMS: "D1 - application unknown in ASMS"
    STB->>NX: "GET /{appId}/{version}/{platform}/{firmware}/{bundleName}"
    NX->>BS: "@backend GET /applications/..."
    BS->>ASMS: "GET /apps/{appId}:{version}"
    ASMS-->>BS: "404"
    BS-->>NX: "404 ErrorResponse (message, details, httpStatusCode, correlation id)"
    NX-->>STB: "404"

    Note over BS,W: "D2 - generation fails"
    BS->>MQ: "publish GenerationMessage"
    MQ->>W: "GenerationMessage"
    W->>MQ: "feedback phaseCode BUNDLE_ERROR"
    MQ->>BS: "feedback"
    BS->>DB: "updateBundleStatusIfNewer -> BUNDLE_ERROR"
    STB->>NX: "retry after Retry-After"
    NX->>BS: "@backend GET /applications/..."
    BS->>DB: "getLatestBundle -> row with BUNDLE_ERROR"
    BS->>DB: "INSERT new bundle row (new UUID) status GENERATION_REQUESTED"
    BS->>MQ: "publish GenerationMessage again"
    BS-->>NX: "202 Retry-After 30"
    NX-->>STB: "202"

    Note over BS,MQ: "D3 - RabbitMQ publish fails"
    BS->>MQ: "publish GenerationMessage"
    MQ-->>BS: "OptionalException with error"
    BS->>DB: "updateStatusForBundle -> BUNDLE_ERROR"
    BS-->>NX: "500 ErrorResponse (RabbitMQException)"
    NX-->>STB: "500"

    Note over BS,CB: "D4 - ASMS outage"
    BS->>CB: "invoke ASMS call"
    CB->>ASMS: "GET /apps/{appId}:{version}"
    ASMS-->>CB: "connection error or 5xx"
    CB->>CB: "record failure, RecoverableException is ignored by the breaker"
    CB-->>BS: "AppstoreMetadataServiceClientException"
    BS-->>NX: "500 ErrorResponse"
    Note over CB: "failure_rate_threshold = 100 percent, closed ring buffer = 5 -> breaker opens"
    STB->>NX: "further requests"
    NX->>BS: "@backend"
    BS->>CB: "invoke"
    CB-->>BS: "CallNotPermittedException while open (wait_duration_in_open_state = 5s, then half-open with ring buffer 1)"
    BS-->>NX: "500 ErrorResponse"
    Note over CB: "bulkhead maxConcurrentCalls = 100, maxWaitDuration = 500ms -> BulkheadFullException under saturation"

    Note over MQ,BS: "D5 - malformed feedback"
    MQ->>BS: "feedback without x-request-id header"
    BS->>BS: "log warn, drop, no DB write"
    MQ->>BS: "feedback without messageTimestamp"
    BS->>BS: "log warn, drop, no DB write"
    MQ->>BS: "feedback with unknown phaseCode"
    BS->>BS: "log warn, drop, no DB write"

    Note over STB,NX: "D6 - ASBS unreachable"
    STB->>NX: "GET /{appId}/... (cache miss)"
    NX->>BS: "@backend proxy_pass"
    BS-->>NX: "connection refused or 5xx"
    NX->>NX: "proxy_intercept_errors on, error_page 500 501 502 503 504 @50x"
    NX-->>STB: "upstream status (e.g. 502/504) with application/json {error: {httpStatusCode: 500, message: Internal Server Error, details: }}"
```

Evidence for the failure paths:

| Path | Evidence | Label |
| --- | --- | --- |
| D1 404 | `appstore-bundle-service appstore-bundle-service-external/appstore-metadata-service-client/src/main/java/com/lgi/appstorebundle/external/asms/AppstoreMetadataServiceClient.java:68-140`, `.../error/handler/GlobalExceptionHandler.java:49-53` | `[VERIFIED]` |
| D2 retry creates a new row | `appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/resources/AppStoreBundleController.java:99-110` (new `randomUUID` per trigger) | `[VERIFIED]` |
| D2 caveat | `getLatestBundle` orders `coalesce(updated_at, created_at)` ascending, so the "latest" row is actually the oldest (`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:54-64`); after one failure the error row keeps being re-selected and generation is re-triggered on every request | `[VERIFIED]` code, `[INFERRED]` impact |
| D3 | `.../service/BundleService.java:60-76`, `.../error/handler/GlobalExceptionHandler.java:54-58` | `[VERIFIED]` |
| D4 | `appstore-bundle-service appstore-bundle-service-external/client-common/src/main/java/com/lgi/appstorebundle/common/r4j/ClientInvokerFactory.java:34-64`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:14-27` | `[VERIFIED]` |
| D5 | `.../util/ConsumerFactory.java:48-90`, `.../configuration/RabbitMQConsumersConfiguration.java:114-130` | `[VERIFIED]` |
| D6 | `appstore-caching-service appstore-caching-service-nginx/default.conf.template:37-38,131-134` | `[VERIFIED]` |

### Encryption drift variant of D2/D3

If a maintainer flips the ASMS `encryption` flag between the accepted request and the generation feedback, the request-time decision (ASMS flag, `EncryptionHelper.java:43-46`) and the feedback-time decision (persisted `bundle.encryption` column, `EncryptionHelper.java:47-50`) diverge and the persisted value wins, so a bundle can be delivered unencrypted (or encrypted) contrary to the current metadata `[VERIFIED]` code, `[INFERRED]` operational impact.

## Open questions

1. How does an STB learn the cache hostname? ASMS builds URLs from `BUNDLES_STORAGE_HOST`, but nothing links that value to the caching service deployment `[INFERRED]`.
2. Is there any client-side or edge-level retry budget for the `202 Retry-After` loop, or does the STB retry indefinitely?
3. Are `BUNDLE_ERROR` rows ever cleaned up, and is there an alert on repeated generation retriggering?
4. What resolves `${ASBM_SERVICE}` in production, given the caching chart does not define it (`appstore-caching-service helm/appstore-caching-service/values.yaml:31-34`) `[VERIFIED]`?

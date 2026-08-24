# 05 — User Requirements Specification (reverse-derived)

Part of the LGE AppStore reverse-engineering dossier. See [README](../README.md) and sibling docs:
[00-overview](00-overview.md) · [01-hld](01-hld.md) · [02-lld](02-lld.md) · [03-processes-L1-L4](03-processes-L1-L4.md) · [04-business-journeys](04-business-journeys.md) · [06-test-cases](06-test-cases.md)

These requirements are **reverse-derived from the implementation**: they describe what the code does today, not an approved specification. Each requirement is one "shall" sentence, labelled `[VERIFIED]` (read in code) or `[INFERRED]`, with `repo path:lines` citations. Test-case traceability lives in [06-test-cases](06-test-cases.md).

Groups: `URS-M*` maintainer perspective (ASMS) · `URS-S*` STB discovery (ASMS) · `URS-B*` bundle service (ASBS) · `URS-C*` caching/delivery · `URS-X*` cross-cutting.

Short paths below are relative to the cited repo; `asms-src` = `appstore-metadata-service/src/main/java/com/lgi/appstore/metadata`, `asbs-app` = `appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle`.

---

## URS-M — Maintainer perspective (ASMS)

| ID | Requirement | Label | Evidence |
| --- | --- | --- | --- |
| URS-M01 | The system shall let a maintainer be created via `POST /maintainers` and return HTTP 201 on success. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/maintainer/MaintainersController.java:63-70` |
| URS-M02 | The system shall reject creation of a maintainer whose code already exists with HTTP 409. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/maintainer/PersistentMaintainersService.java:77-107`, `asms-src/api/error/GlobalExceptionHandler.java:60-63` |
| URS-M03 | The system shall return a single maintainer by code via `GET /maintainers/{maintainerCode}`, or HTTP 404 when the code is unknown. | `[VERIFIED]` | `MaintainersController.java:51-61`, `PersistentMaintainersService.java:61-75`, `GlobalExceptionHandler.java:55-58` |
| URS-M04 | The system shall list maintainers via `GET /maintainers` with optional case-insensitive prefix filtering by `name`. | `[VERIFIED]` | `MaintainersController.java:99-107`, `PersistentMaintainersService.java:160-200` |
| URS-M05 | The system shall apply `limit` and `offset` to maintainer listings and return offset, limit, count and total metadata with the results. | `[VERIFIED]` | `PersistentMaintainersService.java:160-200` |
| URS-M06 | The system shall update a maintainer's address, email, homepage and name via `PUT /maintainers/{maintainerCode}` and return HTTP 204, or HTTP 404 when no maintainer matched. | `[VERIFIED]` | `MaintainersController.java:72-85`, `PersistentMaintainersService.java:109-131` |
| URS-M07 | The system shall refuse to delete a maintainer that still owns at least one application, returning HTTP 409. | `[VERIFIED]` | `PersistentMaintainersService.java:133-158`, `GlobalExceptionHandler.java:60-63` |
| URS-M08 | The system shall delete a maintainer with no applications via `DELETE /maintainers/{maintainerCode}` and return HTTP 204. | `[VERIFIED]` | `MaintainersController.java:87-97`, `PersistentMaintainersService.java:133-158` |
| URS-M09 | The system shall accept a new application version via `POST /maintainers/{maintainerCode}/apps` and return HTTP 201. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/maintainer/MaintainerAppsController.java:98-108` |
| URS-M10 | The system shall persist, for each application version, the reverse-domain application id, version, visibility, encryption flag, preferred flag, OCI image URL, name, description, icon, type, size, category and the platform, hardware, features, dependencies and localization JSONB blocks. | `[VERIFIED]` | `asms-src/api/maintainer/PersistentAppsService.java:323-386`, `appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/V00__Initial_schema.sql` |
| URS-M11 | The system shall reject an application create request for an unknown maintainer code with HTTP 404. | `[VERIFIED]` | `PersistentAppsService.java:323-340`, `GlobalExceptionHandler.java:55-58` |
| URS-M12 | The system shall reject a duplicate application id and version for the same maintainer with HTTP 409. | `[VERIFIED]` | `PersistentAppsService.java:370-386`, `GlobalExceptionHandler.java:60-63` |
| URS-M13 | The system shall reject application payloads missing mandatory fields (application id, name, version, OCI image URL) with HTTP 400. | `[VERIFIED]` | `MaintainerAppsController.java:98-108` (`@Valid`), `GlobalExceptionHandler.java:92-130` |
| URS-M14 | The system shall return maintainer-scoped application details via `GET /maintainers/{maintainerCode}/apps/{appId}` accepting a bare id, `:latest` or `:{version}` suffix. | `[VERIFIED]` | `MaintainerAppsController.java:55-74`, `asms-src/model/AppIdWithVersion.java` |
| URS-M15 | The system shall, when no explicit version is requested by a maintainer, select the version flagged `latest.maintainer` or flagged `preferred`. | `[VERIFIED]` | `PersistentAppsService.java:253-322` |
| URS-M16 | The system shall list a maintainer's applications via `GET /maintainers/{maintainerCode}/apps` filtered by any of name, description, version, type, platform and category. | `[VERIFIED]` | `MaintainerAppsController.java:76-96`, `PersistentAppsService.java:90-183` |
| URS-M17 | The system shall include invisible application versions in maintainer-perspective results. | `[VERIFIED]` | `PersistentAppsService.java:90-183` (no visibility predicate) |
| URS-M18 | The system shall update the version flagged `latest.maintainer` when `PUT /maintainers/{maintainerCode}/apps/{appId}` is called without an explicit version or with `:latest`. | `[VERIFIED]` | `PersistentAppsService.java:387-429`, `MaintainerAppsController.java:110-127` |
| URS-M19 | The system shall update the addressed version when `PUT` is called with an explicit `:{version}` suffix, and shall replace that row's version when the request body supplies a non-blank `version`. | `[VERIFIED]` | `PersistentAppsService.java:430-472` |
| URS-M20 | The system shall clear the `preferred` flag on all other versions of the same application before setting `preferred` on the requested version. | `[VERIFIED]` | `PersistentAppsService.java:430-472` |
| URS-M21 | The system shall recalculate the `latest` flags inside the same transaction as any create, update or single-version delete. | `[VERIFIED]` | `PersistentAppsService.java:323-537` |
| URS-M22 | The system shall track `latest.maintainer` and `latest.stb` independently, choosing `latest.maintainer` as the highest version overall and `latest.stb` as the highest visible version. | `[VERIFIED]` | `PersistentAppsService.java:538-600` |
| URS-M23 | The system shall order versions for latest selection by their dot-separated numeric components rather than lexically. | `[VERIFIED]` | `PersistentAppsService.java:538-600` |
| URS-M24 | The system shall delete a single application version, the `latest.maintainer` version, or all versions when the `appId` path carries `:{version}`, no suffix/`:latest`, or `:all` respectively. | `[VERIFIED]` | `MaintainerAppsController.java:129-151`, `PersistentAppsService.java:473-537` |
| URS-M25 | The system shall return HTTP 204 for a successful application update or delete and HTTP 404 when no row matched. | `[VERIFIED]` | `MaintainerAppsController.java:110-151` |
| URS-M26 | The system shall reject an `appId` path value containing more than one `:` separator or a blank version. | `[VERIFIED]` | `asms-src/model/AppIdWithVersion.java` |

## URS-S — STB discovery (ASMS)

| ID | Requirement | Label | Evidence |
| --- | --- | --- | --- |
| URS-S01 | The system shall expose an unauthenticated read-only STB application list at `GET /apps`. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/stb/StbAppsController.java:43-101` |
| URS-S02 | The system shall return only visible application versions to STB clients. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S03 | The system shall, when no `version` filter is supplied, return the version flagged `latest.stb` or flagged `preferred`. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S04 | The system shall suppress non-preferred duplicate versions of an application when a preferred version is present in the result set. | `[VERIFIED]` | `asms-src/util/ApplicationPreferredHelper.java:40-58` |
| URS-S05 | The system shall filter the STB list by `name` and `description` case-insensitively. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S06 | The system shall filter the STB list by exact `version`, by `type`, by `category` and by `maintainerName`. | `[VERIFIED]` | `StbAppsController.java:78-101`, `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S07 | The system shall filter the STB list by platform architecture, variant and operating system using JSONB containment on the stored platform block. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S08 | The system shall reject an unparseable `platform` or `category` query value with HTTP 400. | `[VERIFIED]` | `asms-src/api/converter/StringToPlatformConverter.java`, `asms-src/api/converter/StringToCategoryConverter.java`, `asms-src/api/error/GlobalExceptionHandler.java:70-80` |
| URS-S09 | The system shall default `offset` to 0 and `limit` to 0, where a `limit` of 0 imposes no SQL limit. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S10 | The system shall return offset, limit, count and total metadata alongside every STB application list. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:111-220` |
| URS-S11 | The system shall return application details via `GET /apps/{appId}` for a bare id, `:latest` or `:{version}`. | `[VERIFIED]` | `StbAppsController.java:53-76` |
| URS-S12 | The system shall determine the application type before validating request parameters for a details request. | `[VERIFIED]` | `StbAppsController.java:55-70`, `asms-src/api/stb/PersistentAppsService.java:82-110` |
| URS-S13 | The system shall require `platformName` and `firmwareVer` for native applications and reject their absence with HTTP 400. | `[VERIFIED]` | `asms-src/api/stb/input/validator/PlatformAndVersionOptionalForWebValidator.java:43-90`, `GlobalExceptionHandler.java:65-68` |
| URS-S14 | The system shall ignore `platformName` and `firmwareVer` for web and Android applications. | `[VERIFIED]` | `PlatformAndVersionOptionalForWebValidator.java:43-90`, `asms-src/util/ApplicationUrlService.java` |
| URS-S15 | The system shall reject requests for applications of an unsupported type with HTTP 400. | `[VERIFIED]` | `PlatformAndVersionOptionalForWebValidator.java:43-110`, `GlobalExceptionHandler.java:65-68` |
| URS-S16 | The system shall treat the application types listed in `webApplications.list` (default `HTML5,LIGHTNING`) as web applications. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:25`, `asms-src/config/BeanConfiguration.java` |
| URS-S17 | The system shall generate the native application download URL as `{protocol}://{host}/{appId}/{version}/{platformName}/{firmwareVer}/{appId}-{version}-{platformName}-{firmwareVer}.tar.gz` from the configured bundle storage protocol and host. | `[VERIFIED]` | `asms-src/util/ApplicationUrlCreator.java:27-60`, `asms-src/config/BeanConfiguration.java` |
| URS-S18 | The system shall return the stored source or OCI URL as the download URL for web and Android applications. | `[VERIFIED]` | `asms-src/util/ApplicationUrlService.java`, `asms-src/util/ApplicationUrlCreator.java:44-48` |
| URS-S19 | The system shall include in application details the application header, its maintainer, all visible versions, and the platform/hardware/feature/dependency requirements. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:221-396` |
| URS-S20 | The system shall return the version list in application details sorted by parsed numeric version. | `[VERIFIED]` | `asms-src/api/stb/PersistentAppsService.java:221-396` |
| URS-S21 | The system shall return HTTP 404 when the requested application or version is not visible or does not exist. | `[VERIFIED]` | `StbAppsController.java:70-76` |

## URS-B — Bundle service (ASBS)

| ID | Requirement | Label | Evidence |
| --- | --- | --- | --- |
| URS-B01 | The service shall expose exactly one bundle endpoint, `GET /applications/{appId}/{appVersion}/{platformName}/{firmwareVersion}/{appBundleName}`. | `[VERIFIED]` | `appstore-bundle-service asbs-app/resources/AppStoreBundleController.java:84-95` |
| URS-B02 | The service shall require an `x-request-id` request header on the bundle endpoint. | `[VERIFIED]` | `AppStoreBundleController.java:84-95` |
| URS-B03 | The service shall resolve STB-perspective application metadata from ASMS, passing `platformName` and `firmwareVer` as query parameters. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-external/appstore-metadata-service-client/src/main/java/com/lgi/appstorebundle/external/asms/AppstoreMetadataServiceClient.java:68-97` |
| URS-B04 | The service shall then resolve maintainer-perspective metadata from ASMS using the maintainer code returned by the STB lookup. | `[VERIFIED]` | `AppStoreBundleController.java:96-100`, `AppstoreMetadataServiceClient.java:99-131` |
| URS-B05 | The service shall return HTTP 404 with a normalized error body when either ASMS lookup yields no application. | `[VERIFIED]` | `AppstoreMetadataServiceClient.java:133-140`, `appstore-bundle-service asbs-app/error/handler/GlobalExceptionHandler.java:49-53` |
| URS-B06 | The service shall treat any non-404 ASMS HTTP failure as an `AppstoreMetadataServiceClientException` and answer HTTP 500. | `[VERIFIED]` | `AppstoreMetadataServiceClient.java:133-140`, `GlobalExceptionHandler.java:44-48` |
| URS-B07 | The service shall look up an existing bundle by application id, application version, platform name and firmware version before triggering generation. | `[VERIFIED]` | `asbs-app/service/BundleService.java:56-58`, `appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:54-64` |
| URS-B08 | The service shall trigger a new bundle generation when no bundle row exists for the requested coordinates or when the retrieved row has status `BUNDLE_ERROR`. | `[VERIFIED]` | `AppStoreBundleController.java:99-110` |
| URS-B09 | The service shall skip generation when a non-error bundle row already exists for the requested coordinates. | `[VERIFIED]` | `AppStoreBundleController.java:99-110` |
| URS-B10 | The service shall assign a fresh UUID to every generation attempt, so a retry after `BUNDLE_ERROR` creates an additional bundle row. | `[VERIFIED]` | `AppStoreBundleController.java:99-110`, `JooqBundleDao.java:73-101` |
| URS-B11 | The service shall persist a bundle row with status `GENERATION_REQUESTED`, the request's correlation id, timestamps and the effective encryption flag before publishing the generation message. | `[VERIFIED]` | `asbs-app/service/BundleService.java:60-69`, `JooqBundleDao.java:73-101` |
| URS-B12 | The service shall publish a `GenerationMessage` carrying the bundle id, application coordinates, OCI image URL and encryption flag to the generation request queue. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/external/RabbitMQService.java:39-53` |
| URS-B13 | The service shall attach the correlation id as an `x-request-id` message header on every published RabbitMQ message. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-external/rabbitmq-client/src/main/java/com/lgi/appstorebundle/external/ManagedRabbitMQ.java:91-112` |
| URS-B14 | The service shall publish to the RabbitMQ default exchange using the queue name as routing key and shall not declare exchanges or queues itself. | `[VERIFIED]` | `ManagedRabbitMQ.java:99-112` |
| URS-B15 | The service shall set the bundle status to `BUNDLE_ERROR` and propagate the failure as HTTP 500 when publishing the generation message fails. | `[VERIFIED]` | `BundleService.java:60-76`, `GlobalExceptionHandler.java:54-58` |
| URS-B16 | The service shall answer HTTP 202 with a `Retry-After` header and the echoed `x-request-id` whenever the application exists, whether or not generation was triggered. | `[VERIFIED]` | `AppStoreBundleController.java:111-114` |
| URS-B17 | The `Retry-After` value shall be the configured `http.retry.after` duration expressed in seconds, defaulting to 30. | `[VERIFIED]` | `AppStoreBundleController.java:111-113`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:1` |
| URS-B18 | The service shall model bundle lifecycle with exactly the statuses `GENERATION_REQUESTED`, `GENERATION_LAUNCHED`, `GENERATION_COMPLETED`, `ENCRYPTION_REQUESTED`, `ENCRYPTION_LAUNCHED`, `ENCRYPTION_COMPLETED` and `BUNDLE_ERROR`. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-api/src/main/java/com/lgi/appstorebundle/api/model/BundleStatus.java:25-45` |
| URS-B19 | The service shall consume feedback from the generation status and encryption status queues with manual acknowledgement. | `[VERIFIED]` | `asbs-app/configuration/RabbitMQConsumersConfiguration.java:69-98` |
| URS-B20 | The service shall discard, with a warning and without persisting anything, any feedback delivery lacking a non-empty `x-request-id` header. | `[VERIFIED]` | `asbs-app/util/ConsumerFactory.java:48-90` |
| URS-B21 | The service shall discard any feedback message lacking a `messageTimestamp`. | `[VERIFIED]` | `RabbitMQConsumersConfiguration.java:114-130` |
| URS-B22 | The service shall discard any feedback message whose `phaseCode` does not map to a known bundle status. | `[VERIFIED]` | `RabbitMQConsumersConfiguration.java:114-130`, `BundleStatus.java:33-45` |
| URS-B23 | The service shall discard any feedback message whose payload cannot be deserialized into a feedback message. | `[VERIFIED]` | `ConsumerFactory.java:48-90` |
| URS-B24 | The service shall apply a feedback status only when the incoming message timestamp is strictly newer than the persisted timestamp for that bundle. | `[VERIFIED]` | `BundleService.java:70-76`, `JooqBundleDao.java:114-127` |
| URS-B25 | The service shall trigger encryption when generation feedback reports `GENERATION_COMPLETED` and encryption is effectively enabled. | `[VERIFIED]` | `RabbitMQConsumersConfiguration.java:99-108` |
| URS-B26 | Effective encryption for a new bundle request shall be the conjunction of the global `bundle.encryption.enabled` switch and the per-application `encryption` flag from ASMS maintainer metadata. | `[VERIFIED]` | `asbs-app/util/EncryptionHelper.java:43-46`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:3` |
| URS-B27 | Effective encryption during feedback processing shall be the conjunction of the global switch and the `encryption` column persisted on the bundle row, defaulting to false when the row is absent. | `[VERIFIED]` | `EncryptionHelper.java:47-50`, `BundleService.java:91-96`, `JooqBundleDao.java:128-137` |
| URS-B28 | The service shall set status `ENCRYPTION_REQUESTED` and publish an `EncryptionMessage` when encryption is triggered, and shall set `BUNDLE_ERROR` if that publish fails. | `[VERIFIED]` | `BundleService.java:77-90` |
| URS-B29 | The service shall log and skip encryption when the bundle id referenced by feedback does not exist. | `[VERIFIED]` | `BundleService.java:77-90` |
| URS-B30 | The service shall advance encryption statuses only through the same newer-timestamp rule used for generation feedback. | `[VERIFIED]` | `RabbitMQConsumersConfiguration.java:109-130` |
| URS-B31 | The service shall read all four queue names from configuration, defaulting to `bundlegen-service-requests`, `bundlegen-service-status`, `bundlecrypt-service-requests` and `bundlecrypt-service-status`. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:29-32` |
| URS-B32 | The service shall use separate read-only and write datasources for bundle persistence. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:43-59` |
| URS-B33 | The service shall version its bundle schema with Flyway migrations, the second of which adds the `encryption` column. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/resources/migration/V1__Add_schema.sql`, `.../V2__Add_encryption_column.sql` |

## URS-C — Caching and delivery (Nginx)

| ID | Requirement | Label | Evidence |
| --- | --- | --- | --- |
| URS-C01 | The edge shall serve bundle requests on port 8080 with the document root set to `${ENCRYPTED_BUNDLES_PATH}`. | `[VERIFIED]` | `appstore-caching-service appstore-caching-service-nginx/default.conf.template:33-36` |
| URS-C02 | The edge shall serve a requested bundle directly from the mounted volume when the corresponding file exists. | `[VERIFIED]` | `default.conf.template:36` |
| URS-C03 | The edge shall fall back to the named location `@backend` when the requested file is absent. | `[VERIFIED]` | `default.conf.template:36,89` |
| URS-C04 | The edge shall rewrite a cache-miss URI to `/applications/{original path}` before proxying it to the bundle service. | `[VERIFIED]` | `default.conf.template:126-127` |
| URS-C05 | The edge shall forward the client's `x-request-id` to the bundle service, generating Nginx's `$request_id` when the client did not supply one. | `[VERIFIED]` | `default.conf.template:28-31,128` |
| URS-C06 | The edge shall proxy `/platforms` and `/bundles` requests to the `asbm-backend` upstream. | `[VERIFIED]` | `default.conf.template:24-26,50-87` |
| URS-C07 | The edge shall answer `OPTIONS` preflight requests on the bundle and `asbm-backend` routes with HTTP 204 and permissive CORS headers. | `[VERIFIED]` | `default.conf.template:51-60,90-99` |
| URS-C08 | The edge shall add `Access-Control-Allow-Origin: *` and related CORS headers to `GET`, `POST` and `DELETE` responses on those routes. | `[VERIFIED]` | `default.conf.template:61-85,100-124` |
| URS-C09 | The edge shall intercept upstream 500, 501, 502, 503 and 504 responses and replace their bodies with a fixed JSON error body (which always says `httpStatusCode: 500`); the original HTTP status code is preserved. | `[VERIFIED]` | `default.conf.template:37-38,131-134` |
| URS-C10 | The edge shall expose `/healthcheck` and `/ping` on port 8081, each returning HTTP 204. | `[VERIFIED]` | `default.conf.template:137-150` |
| URS-C11 | The edge shall deny requests to `/` on the management port. | `[VERIFIED]` | `default.conf.template:140-142` |
| URS-C12 | The edge shall serve the caching service OpenAPI document and Swagger UI under `/swagger/`. | `[VERIFIED]` | `default.conf.template:42-48` |
| URS-C13 | The edge shall mount the bundle storage volume read-only, so bundles are produced exclusively by external components. | `[VERIFIED]` | `appstore-caching-service helm/appstore-caching-service/values.yaml:31-34` and deployment template |
| URS-C14 | The edge shall resolve upstream service names through the configured cluster DNS resolver. | `[VERIFIED]` | `default.conf.template:39`, `appstore-caching-service helm/appstore-caching-service/values.yaml:34` |
| URS-C15 | The `asbm-backend` upstream shall be configured through `ASBM_SERVICE`, which the shipped chart does not define — an unresolved configuration gap. | `[VERIFIED]` (gap) | `default.conf.template:24-26`, `appstore-caching-service helm/appstore-caching-service/values.yaml:31-34` |

## URS-X — Cross-cutting

| ID | Requirement | Label | Evidence |
| --- | --- | --- | --- |
| URS-X01 | Both services shall accept an `x-request-id` request header as the correlation id. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/filter/CorrelationIdFilter.java:34-61`, `appstore-bundle-service asbs-app/filters/CorrelationIdFilter.java:34-70` |
| URS-X02 | Both services shall generate a UUID correlation id when the header is absent or blank. | `[VERIFIED]` | same as URS-X01 |
| URS-X03 | Both services shall place the correlation id into the logging context for the duration of the request and remove it afterwards. | `[VERIFIED]` | same as URS-X01 |
| URS-X04 | Both services shall echo the correlation id in the `x-request-id` response header. | `[VERIFIED]` | same as URS-X01 |
| URS-X05 | The bundle service shall propagate the correlation id across RabbitMQ in both directions. | `[VERIFIED]` | `ManagedRabbitMQ.java:91-112`, `ConsumerFactory.java:48-90` |
| URS-X06 | ASMS shall return a normalized `ErrorResponse` body for handled failures rather than a servlet whitelabel page. | `[VERIFIED]` | `appstore-metadata-service asms-src/api/error/GlobalExceptionHandler.java:44-130`, `asms-src/api/error/CustomErrorController.java:36-70`, `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:12-13` |
| URS-X07 | ASMS shall map unexpected exceptions to HTTP 500, maintainer-not-found to 404, duplicate resources to 409, and validation, type-conversion and missing-parameter problems to 400. | `[VERIFIED]` | `GlobalExceptionHandler.java:50-130` |
| URS-X08 | ASBS shall return a normalized error body containing message, details, HTTP status code and correlation id. | `[VERIFIED]` | `appstore-bundle-service asbs-app/error/handler/GlobalExceptionHandler.java:39-60` |
| URS-X09 | Neither service shall include stack traces in error responses. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:12`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:76` |
| URS-X10 | Authentication and authorization shall be delegated to an intermediate proxy or API gateway, with the optional `x-maintainer-id` header conveying maintainer identity. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/static/appstore-metadata-service.yaml:194-200` |
| URS-X11 | The reference proxy shall apply HTTP basic authentication and inject a fixed `x-maintainer-id` when forwarding to ASMS under `/as3`. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/docker-compose/as3proxy/nginx.conf:56-63` |
| URS-X12 | No service shall itself verify that the caller is entitled to act on the addressed `{maintainerCode}`. | `[VERIFIED]` (absence) | `appstore-metadata-service asms-src/api/maintainer/MaintainerAppsController.java:47-151` |
| URS-X13 | Every ASMS call made by the bundle service shall be executed through a Resilience4j circuit breaker. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-external/client-common/src/main/java/com/lgi/appstorebundle/common/r4j/ClientInvokerFactory.java:34-64` |
| URS-X14 | The circuit breaker shall ignore `RecoverableException` when computing its failure rate. | `[VERIFIED]` | `ClientInvokerFactory.java:54-64` |
| URS-X15 | The circuit breaker shall open at a 100 percent failure rate over a closed-state ring buffer of 5 calls, wait 5 seconds in the open state, use a half-open ring buffer of 1 and transition automatically from open to half-open. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:14-19` |
| URS-X16 | A bulkhead shall limit concurrent ASMS calls to 100 with a maximum wait of 500 ms when configured. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:21-24`, `ClientInvokerFactory.java:40-52` |
| URS-X17 | Circuit breaker and bulkhead state transitions and rejections shall be logged and exported as Prometheus metrics. | `[VERIFIED]` | `ClientInvokerFactory.java:47-64`, `appstore-bundle-service appstore-bundle-service-external/client-common/src/main/java/com/lgi/appstorebundle/common/r4j/CircuitBreakerFactory.java`, `.../BulkheadFactory.java` |
| URS-X18 | ASMS calls shall use a 1000 ms request timeout and a 60 s idle timeout. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:26-27` |
| URS-X19 | ASMS shall expose only the `info` and `prometheus` actuator endpoints, at the root management base path, with all endpoints disabled by default. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:16-23` |
| URS-X20 | ASBS shall expose the `health`, `info` and `prometheus` actuator endpoints. | `[VERIFIED]` | `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:68-72` |
| URS-X21 | Prometheus scraping shall be switchable at deploy time through the `PROMETHEUS_METRICS` environment variable in both services. | `[VERIFIED]` | both `application.properties` files (`appstore-metadata-service ...:22`, `appstore-bundle-service ...:72`) |
| URS-X22 | Both services shall publish their OpenAPI document and Swagger UI at `/swagger`. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:14-15`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:8-9` |
| URS-X23 | Both services shall omit null properties from JSON responses. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/config/application.properties:2`, `appstore-bundle-service appstore-bundle-service-application/src/main/resources/config/application.properties:61` |
| URS-X24 | Database schemas shall be managed by Flyway migrations in both services. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/` (V00–V05), `appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/resources/migration/` (V1–V2) |
| URS-X25 | Application metadata shall be the single source of truth for the per-application encryption flag, and the bundle service shall snapshot it on the bundle row at request time. | `[VERIFIED]` | `appstore-metadata-service appstore-metadata-service/src/main/resources/db/migration/V04__Add_encryption_column.sql`, `appstore-bundle-service .../JooqBundleDao.java:73-101` |
| URS-X26 | The generation and encryption workers shall be external components that communicate with the bundle service only through the four RabbitMQ queues. | `[INFERRED]` | no worker source in any repo; `RabbitMQConsumersConfiguration.java:69-98` |
| URS-X27 | The `asbm-backend` service behind `/platforms` and `/bundles` shall be an external component outside the scope of these three repositories. | `[INFERRED]` | `appstore-caching-service appstore-caching-service-nginx/default.conf.template:24-26,50-87` |

## Open questions

1. Is `limit=0` meaning "unlimited" an intentional API contract, or should a maximum page size be enforced? Both ASMS perspectives behave this way `[VERIFIED]`.
2. Which component enforces maintainer tenancy in production (URS-X10/URS-X12)?
3. Should the feedback path re-read the ASMS encryption flag rather than the snapshot (URS-B26 vs URS-B27)?
4. Is the ascending ordering behind URS-B07 intentional? See the defect noted in [03-processes-L1-L4](03-processes-L1-L4.md).
5. Who provisions the RabbitMQ topology implied by URS-B14?

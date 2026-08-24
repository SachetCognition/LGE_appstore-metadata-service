# 06 — Reverse-derived test cases and coverage reconciliation

Part of the LGE AppStore reverse-engineering dossier. See [README](../README.md) and sibling docs:
[00-overview](00-overview.md) · [01-hld](01-hld.md) · [02-lld](02-lld.md) · [03-processes-L1-L4](03-processes-L1-L4.md) · [04-business-journeys](04-business-journeys.md) · [05-urs](05-urs.md)

Test cases below are derived from the requirements in [05-urs](05-urs.md) and use the same IDs. Section 2 lists the **actual** existing tests found in the three repositories; section 3 reconciles derived cases against them and names the gaps.

---

## 1. Derived test cases (Given / When / Then)

### 1.1 Maintainer perspective (ASMS)

| Case | Given / When / Then | URS |
| --- | --- | --- |
| TC-M01 | Given no maintainer with code `X`, when `POST /maintainers` is called with code `X`, then HTTP 201 is returned and the maintainer is readable. | URS-M01 |
| TC-M02 | Given a maintainer with code `X`, when `POST /maintainers` repeats code `X`, then HTTP 409 is returned. | URS-M02 |
| TC-M03 | Given an unknown code, when `GET /maintainers/{code}` is called, then HTTP 404 with a normalized error body is returned. | URS-M03, URS-X07 |
| TC-M04 | Given several maintainers, when `GET /maintainers?name=abc` is called with different letter casing, then only matching maintainers are returned. | URS-M04 |
| TC-M05 | Given more maintainers than the page size, when `GET /maintainers?limit=2&offset=1` is called, then two maintainers are returned and the metadata reports offset 1, limit 2, count 2 and the full total. | URS-M05 |
| TC-M06 | Given an existing maintainer, when `PUT /maintainers/{code}` changes email, address, homepage and name, then HTTP 204 is returned and a subsequent read reflects all four fields. | URS-M06 |
| TC-M07 | Given a maintainer owning one application, when `DELETE /maintainers/{code}` is called, then HTTP 409 is returned and the maintainer still exists. | URS-M07 |
| TC-M08 | Given a maintainer with no applications, when `DELETE /maintainers/{code}` is called, then HTTP 204 is returned and a subsequent read returns 404. | URS-M08 |
| TC-M09 | Given a valid application payload, when `POST /maintainers/{code}/apps` is called, then HTTP 201 is returned and every persisted field (including JSONB blocks, size, OCI URL, encryption, preferred) round-trips on read. | URS-M09, URS-M10 |
| TC-M10 | Given an unknown maintainer code, when an application is posted to it, then HTTP 404 is returned. | URS-M11 |
| TC-M11 | Given an existing application id and version, when the same id and version is posted again, then HTTP 409 is returned. | URS-M12 |
| TC-M12 | Given payloads missing application id, name, version or OCI image URL (absent or blank), when posted, then HTTP 400 is returned for each. | URS-M13 |
| TC-M13 | Given an application with versions 1.0.0, 1.1.0 and 2.0.0, when `GET /maintainers/{code}/apps/{appId}` is called without a version suffix, then version 2.0.0 is returned. | URS-M14, URS-M15, URS-M22 |
| TC-M14 | Given the same application, when `:latest` and `:1.1.0` suffixes are used, then the latest and the explicitly requested version are returned respectively. | URS-M14 |
| TC-M15 | Given version 1.1.0 marked preferred and 2.0.0 not, when the maintainer reads the application without a version, then the preferred version is returned. | URS-M15, URS-M20 |
| TC-M16 | Given applications differing in name, description, version, type, platform and category, when the maintainer list is filtered by each of those in turn, then only matching applications are returned. | URS-M16 |
| TC-M17 | Given an invisible newest version, when the maintainer lists applications, then the invisible version is present in the maintainer view. | URS-M17 |
| TC-M18 | Given versions 1.0.0 and 2.0.0, when `PUT /maintainers/{code}/apps/{appId}` is called without a version suffix, then only version 2.0.0 is modified. | URS-M18 |
| TC-M19 | Given version 1.0.0, when `PUT .../{appId}:1.0.0` sends a body containing `version: 1.0.1`, then the stored row's version becomes 1.0.1. | URS-M19 |
| TC-M20 | Given version 1.0.0 already preferred, when version 2.0.0 is updated with `preferred: true`, then 2.0.0 is preferred and 1.0.0 is no longer preferred. | URS-M20 |
| TC-M21 | Given a create, update or single-version delete that fails midway, when the transaction rolls back, then neither the row nor the `latest` flags are changed. | URS-M21 |
| TC-M22 | Given versions 1.0.0 (visible) and 2.0.0 (invisible), when latest flags are recalculated, then `latest.maintainer` is on 2.0.0 and `latest.stb` is on 1.0.0. | URS-M22 |
| TC-M23 | Given versions 2.0.0 and 10.0.0, when latest flags are recalculated, then 10.0.0 is latest (numeric, not lexical, ordering). | URS-M23 |
| TC-M24 | Given three versions, when delete is called with `:1.0.0`, then only that version disappears; with no suffix, only the maintainer-latest disappears; with `:all`, all versions disappear. | URS-M24 |
| TC-M25 | Given a non-existent application id or version, when update or delete is called, then HTTP 404 is returned. | URS-M25 |
| TC-M26 | Given an `appId` path of `a:b:c` or `appId:` (blank version), when any application endpoint is called, then the request is rejected. | URS-M26 |

### 1.2 STB discovery (ASMS)

| Case | Given / When / Then | URS |
| --- | --- | --- |
| TC-S01 | Given applications exist, when `GET /apps` is called without credentials, then HTTP 200 and a list are returned. | URS-S01 |
| TC-S02 | Given one visible and one invisible application, when the STB lists applications, then only the visible one is returned. | URS-S02 |
| TC-S03 | Given versions 1.0.0 and 2.0.0 both visible, when the STB lists applications without a version filter, then only 2.0.0 appears. | URS-S03 |
| TC-S04 | Given versions 1.0.0 (preferred) and 2.0.0, when the STB lists applications, then only the preferred version appears once. | URS-S03, URS-S04 |
| TC-S05 | Given applications named `Foo` and `bar`, when `GET /apps?name=FO` is called, then `Foo` is returned (case-insensitive). | URS-S05 |
| TC-S06 | Given mixed applications, when the STB list is filtered by `version`, `type`, `category` and `maintainerName` in turn, then only matching applications are returned. | URS-S06 |
| TC-S07 | Given applications on different platforms, when `GET /apps?platform=arm:v7:linux` is called, then only applications matching architecture, variant and OS are returned. | URS-S07 |
| TC-S08 | Given a malformed `platform` (no architecture, or too many components) or an unknown `category`, when the STB list is requested, then HTTP 400 is returned. | URS-S08 |
| TC-S09 | Given 5 applications, when `limit` is omitted or set to 0, then all 5 are returned; when `limit=2&offset=3`, then 2 are returned starting from the fourth. | URS-S09 |
| TC-S10 | Given any STB list request, when it succeeds, then the response metadata contains offset, limit, count and total. | URS-S10 |
| TC-S11 | Given an application with three versions, when `GET /apps/{appId}`, `{appId}:latest` and `{appId}:1.0.0` are requested, then the latest, latest and explicit versions are returned respectively. | URS-S11 |
| TC-S12 | Given a native application, when details are requested without `platformName` or without `firmwareVer`, then HTTP 400 is returned in both cases. | URS-S13 |
| TC-S13 | Given a web (`HTML5`) or Android application, when details are requested with no `platformName` and no `firmwareVer`, then HTTP 200 is returned. | URS-S14, URS-S16 |
| TC-S14 | Given an application whose type is not supported, when details are requested, then HTTP 400 is returned. | URS-S15 |
| TC-S15 | Given a native application, when details are requested, then the URL equals `{protocol}://{host}/{appId}/{version}/{platformName}/{firmwareVer}/{appId}-{version}-{platformName}-{firmwareVer}.tar.gz`. | URS-S17 |
| TC-S16 | Given a web or Android application, when details are requested, then the returned URL is the stored source URL. | URS-S18 |
| TC-S17 | Given an application with requirements and several visible versions, when details are requested, then header, maintainer, versions and requirements are all present. | URS-S19 |
| TC-S18 | Given versions 1.0.0, 2.0.0 and 10.0.0, when details are requested, then the version list is ordered numerically. | URS-S20 |
| TC-S19 | Given an unknown application id, or a version that exists but is invisible, when details are requested, then HTTP 404 is returned. | URS-S02, URS-S21 |

### 1.3 Bundle service (ASBS)

| Case | Given / When / Then | URS |
| --- | --- | --- |
| TC-B01 | Given a valid bundle path, when the endpoint is called without `x-request-id`, then the request is rejected as a bad request. | URS-B02 |
| TC-B02 | Given ASMS returns 404 for the STB lookup, when a bundle is requested, then HTTP 404 with a normalized error body is returned and nothing is persisted. | URS-B03, URS-B05 |
| TC-B03 | Given the STB lookup succeeds but the maintainer lookup returns 404, when a bundle is requested, then HTTP 404 is returned. | URS-B04, URS-B05 |
| TC-B04 | Given ASMS answers HTTP 500, when a bundle is requested, then HTTP 500 is returned. | URS-B06 |
| TC-B05 | Given no bundle row for the coordinates, when a bundle is requested, then a row with status `GENERATION_REQUESTED` is stored and a generation message is published. | URS-B07, URS-B08, URS-B11, URS-B12 |
| TC-B06 | Given a bundle row with status `GENERATION_REQUESTED`, when the same bundle is requested again, then no new row is created and no message is published. | URS-B09 |
| TC-B07 | Given a bundle row with status `BUNDLE_ERROR`, when the bundle is requested again, then a new row with a new UUID and status `GENERATION_REQUESTED` is created and a message is published. | URS-B08, URS-B10 |
| TC-B08 | Given several bundle rows for the same coordinates with different timestamps, when the latest bundle is looked up, then the **most recent** row is evaluated. | URS-B07 (asserts intended behaviour; current code returns the oldest) |
| TC-B09 | Given any accepted request, when it completes, then HTTP 202 with `Retry-After: 30` and the echoed `x-request-id` is returned. | URS-B16, URS-B17 |
| TC-B10 | Given RabbitMQ publishing fails, when a bundle is requested, then the row is set to `BUNDLE_ERROR` and HTTP 500 is returned. | URS-B15 |
| TC-B11 | Given a published generation message, when it is inspected, then it carries the bundle id, application coordinates, OCI image URL, encryption flag and the `x-request-id` header, and was published to the default exchange with the queue name as routing key. | URS-B12, URS-B13, URS-B14 |
| TC-B12 | Given a feedback message with a newer timestamp, when it is consumed, then the bundle status is updated. | URS-B19, URS-B24 |
| TC-B13 | Given a feedback message with an older timestamp, when it is consumed, then the bundle status is unchanged. | URS-B24 |
| TC-B14 | Given a feedback delivery without an `x-request-id` header, when it is consumed, then nothing is persisted. | URS-B20 |
| TC-B15 | Given a feedback message without `messageTimestamp`, when it is consumed, then nothing is persisted. | URS-B21 |
| TC-B16 | Given a feedback message with an unknown `phaseCode`, when it is consumed, then nothing is persisted. | URS-B22 |
| TC-B17 | Given a malformed (non-deserializable) feedback payload, when it is consumed, then nothing is persisted and the consumer keeps running. | URS-B23 |
| TC-B18 | Given global encryption enabled and the application's ASMS `encryption` flag true, when a bundle is requested, then the stored row and the generation message both carry encryption true. | URS-B26 |
| TC-B19 | Given global encryption enabled and the application's ASMS `encryption` flag false (or absent), when a bundle is requested, then encryption is false. | URS-B26 |
| TC-B20 | Given global encryption disabled, when a bundle is requested for an application whose flag is true, then encryption is false. | URS-B26 |
| TC-B21 | Given `GENERATION_COMPLETED` feedback and the bundle row's `encryption` column true, when feedback is processed, then status becomes `ENCRYPTION_REQUESTED` and an encryption message is published. | URS-B25, URS-B27, URS-B28 |
| TC-B22 | Given `GENERATION_COMPLETED` feedback and the bundle row's `encryption` column false, when feedback is processed, then no encryption message is published. | URS-B27 |
| TC-B23 | Given the ASMS `encryption` flag is flipped after the bundle row was created, when generation feedback arrives, then the decision follows the persisted column and the divergence is detected. | URS-B26, URS-B27 |
| TC-B24 | Given a feedback message referencing an unknown bundle id, when encryption would be triggered, then it is skipped without error. | URS-B29 |
| TC-B25 | Given encryption message publishing fails, when encryption is triggered, then the bundle status becomes `BUNDLE_ERROR`. | URS-B28 |
| TC-B26 | Given encryption feedback with an older timestamp, when consumed, then the status is unchanged. | URS-B30 |
| TC-B27 | Given the service is running, when `/health`, `/info` and `/prometheus` are requested, then each responds according to configuration. | URS-B32, URS-X20, URS-X21 |
| TC-B28 | Given ASMS is unavailable and 5 consecutive calls fail, when the next bundle request arrives, then the circuit breaker is open and the call is not attempted. | URS-X13, URS-X15 |
| TC-B29 | Given a `RecoverableException` from an ASMS call, when the breaker evaluates it, then it does not count as a failure. | URS-X14 |
| TC-B30 | Given the bulkhead is saturated, when another ASMS call is attempted, then it is rejected after at most the configured wait duration. | URS-X16 |

### 1.4 Caching and delivery

| Case | Given / When / Then | URS |
| --- | --- | --- |
| TC-C01 | Given the requested bundle file exists under the document root, when the STB requests it, then the file bytes are served without contacting ASBS. | URS-C01, URS-C02 |
| TC-C02 | Given the file is absent, when the STB requests it, then the request is rewritten to `/applications/{path}` and proxied to the bundle service. | URS-C03, URS-C04 |
| TC-C03 | Given the client sends no `x-request-id`, when the request is proxied, then the upstream receives a generated id. | URS-C05 |
| TC-C04 | Given the client sends `x-request-id`, when the request is proxied, then the upstream receives that exact value. | URS-C05 |
| TC-C05 | Given the bundle service is unreachable or answers 5xx, when a cache miss occurs, then HTTP 500 with the fixed JSON error body and `Content-Type: application/json` is returned. | URS-C09 |
| TC-C06 | Given a `/platforms` or `/bundles` request, when it is handled, then it is proxied to `asbm-backend`. | URS-C06 |
| TC-C07 | Given an `OPTIONS` preflight to a bundle or `asbm-backend` route, when it is handled, then HTTP 204 with CORS allow-origin, allow-methods, allow-headers and max-age is returned. | URS-C07 |
| TC-C08 | Given a `GET` on those routes, when it is answered, then `Access-Control-Allow-Origin: *` is present. | URS-C08 |
| TC-C09 | Given the management port, when `/healthcheck` and `/ping` are requested, then HTTP 204 is returned, and `/` is denied. | URS-C10, URS-C11 |
| TC-C10 | Given the NFS-backed volume, when the container reads a bundle, then the read succeeds and writes are impossible. | URS-C13 |
| TC-C11 | Given `/swagger/`, when requested, then the Swagger UI and the aliased spec are served. | URS-C12 |
| TC-C12 | Given the shipped Helm chart, when the config map is rendered, then `ASBM_SERVICE` is undefined and the `asbm-backend` upstream cannot resolve. | URS-C15 |

### 1.5 Cross-cutting

| Case | Given / When / Then | URS |
| --- | --- | --- |
| TC-X01 | Given a request without `x-request-id` to either service, when it completes, then the response carries a generated `x-request-id`. | URS-X01, URS-X02, URS-X04 |
| TC-X02 | Given a request with `x-request-id`, when it completes, then the same value is echoed and appears in log lines for that request. | URS-X03, URS-X04 |
| TC-X03 | Given a bundle request, when generation and encryption messages are published, then both carry the request's correlation id, and consumed feedback restores it into the log context. | URS-X05 |
| TC-X04 | Given an unmapped exception in ASMS, when it surfaces, then HTTP 500 with a normalized `ErrorResponse` and no stack trace is returned. | URS-X06, URS-X07, URS-X09 |
| TC-X05 | Given a failing ASBS request, when the error body is inspected, then it contains message, details, HTTP status code and correlation id. | URS-X08 |
| TC-X06 | Given a request that omits `x-maintainer-id`, when a maintainer endpoint is called, then it still succeeds, demonstrating that authorization is delegated. | URS-X10, URS-X12 |
| TC-X07 | Given maintainer A's credentials at the proxy, when maintainer B's `{maintainerCode}` is addressed, then ASMS does not block the call. | URS-X12 |
| TC-X08 | Given a JSON payload with unknown properties, when it is deserialized, then no failure occurs. | URS-X23 |
| TC-X09 | Given a fresh database, when either service starts, then Flyway applies all migrations in order. | URS-X24 |

---

## 2. Existing tests found in the repositories

### 2.1 ASMS — Spock functional specs (`appstore-metadata-service appstore-metadata-service-tests/src/test/groovy/com/lgi/appstore/metadata/test/cases/functional/`) `[VERIFIED]`

`MaintainerApiFTSpec.groovy`:
- `create application very basic validation for #behavior`
- `second attempt to create same application should be rejected`
- `create non-existing app and view details for #behavior`
- `developer cannot access other developer application (GET/PUT/DELETE)`
- `details of each version contain separate information about app requirements, maintainer and all available versions of the application`
- `update application details for #field - PUT operation does complete overwrite of latest version (by ID alone)`
- `update application details value of #field for specific version (non-latest)`
- `update application details value of #field for specific version (non-latest) for param newVersion`
- `deletes application with #behavior`
- `delete by not existing version or id`
- `consecutive deletes of application versions`
- `query for applications list for #queryDevCode returns apps in latest versions in amount corresponding to given limit=#limit offset=#offset`
- `queries for applications list returns apps for #behavior`

`ManageMaintainerApiFTSpec.groovy`:
- `query for maintainers list returns amount corresponding to given limit=#limit offset=#offset`
- `query for maintainers list filtering by name #scenario`

`StbApiFTSpec.groovy`:
- `queries for applications list returns apps in latest versions in amount corresponding to given limit=#limit offset=#offset`
- `queries for applications list returns apps corresponding to given filter by #queryParam`
- `developer creates new app and stb view details for #behavior`
- `details of each version contain separate information about app requirements, maintainer and all visible versions of the application`

Additional real-environment specs under `cases/real/` `[VERIFIED]`: `DevApiFTSpecSanity` (`CRUD operations check (POST, GET, PUT, DELETE)`), `ManageMaintainerApiFTSpecSanity` (same name), `ConfigurationFTSpecSanity` (`Object mapper is configured not to fail on unknown properties`), `DevApiFTSpecSmoke` (`smoke check of example apps`), `ManageMaintainerApiFTSpecSmoke` (`smoke check of default maintainer`).

### 2.2 ASMS — JUnit unit/service tests (`appstore-metadata-service appstore-metadata-service/src/test/java/com/lgi/appstore/metadata/`) `[VERIFIED]`

- `api/stb/StbAppsControllerTest`: list when some/none present; list by name, description, version, type, platform, category, maintainerName, offset, limit; reject incorrect platform and category; details by id, id+version, id+`latest`; 404 for non-existing; 400 without `platformName` / `firmwareVer`; cannot list native app when firmware or platform absent; can list web application; cannot list application when type unsupported.
- `api/maintainer/MaintainerAppsControllerTest`: list variants and filters as above; details by id, id+version, id+`latest`, and with `encrypted = true`; 404 for non-existing; add application; reject add without version, without/blank OCI image URL, without/blank application name, without/blank application id; 400 without `platformName` / `firmwareVer`.
- `api/stb/PersistentAppsServiceTest`: details, details by version, list, version sorting (both details flavours), limit omitted / greater / smaller / equal / zero, application type by id, source URL for web and Android, generated URL for native.
- `api/maintainer/PersistentAppsServiceTest`: details, details by version, list, add for non-existent maintainer, maintainer-not-found, update latest, delete by version, delete latest, delete all versions, version sorting, limit variants including zero.
- `api/stb/input/validator/PlatformAndVersionOptionalForWebValidatorTest`: missing app id, native app without platform name, native app without firmware version, all fields set, web app without platform/firmware, unsupported application.
- `model/AppIdWithVersionTest`: explicit version, `latest`, no version, `all`, too many tokens, too few tokens. `model/AppIdWithTypeTest`: id mandatory, type optional.
- `util/`: `ApplicationUrlCreatorTest` (native URL, web URL, web URL absent), `ApplicationUrlServiceTest` (web/Android vs native), `ApplicationTypeHelperTest`, `JsonProcessorHelperTest` (unknown properties ignored).
- `api/converter/`: `StringToPlatformConverterTest`, `StringToCategoryConverterTest`, `StringToApplicationTypeConverterTest` (valid, invalid and missing values).

### 2.3 ASBS — unit tests `[VERIFIED]`

- `appstore-bundle-service-application/src/test/java/com/lgi/appstorebundle/resources/AppStoreBundleControllerTest`: `returnErrorWhenApplicationNotExistsInAppstoreMetadataService`, `returnErrorWhenApplicationForMaintainerNotExistsInAppstoreMetadataService`, `returnRetryAfterAndTriggerBundleGeneration`, `returnRetryAfterAndTriggerNewBundleGenerationBecauseOfBundleErrorStatus`, `returnRetryAfterWithoutStartingBundleGeneration`, `returnRetryAfterWithEncryptionEnabled`, `returnRetryAfterWithEncryptionDisabled`.
- `.../resources/AppStoreBundleControllerRealRequestsTest`: `givenValidRequestWhenApplicationDoesNotExistThenNotFound`, `givenValidRequestWhenApplicationForMaintainerDoesNotExistThenNotFound`, `givenValidRequestWhenGenerationNotStartedThenGenerationRequested`, `givenValidRequestWhenGenerationErrorThenTriggerNewGeneration`, `givenValidRequestWhenGenerationRequestedThenGenerationSkipped`, `givenValidRequestWhenTriggeringGenerationThrowsExceptionThenInternalServerError`.
- `.../service/BundleServiceTest`: `triggerBundleGeneration_SavingToDBAndSendingMessageExecuted`, `triggerBundleGenerationWithException_SaveAndUpdateRowAndSendingMessageExecuted`, `triggerBundleEncryptionWithNotExistingBundle_skipTriggeringEncryption`, `triggerBundleEncryptionWithExistingBundle_sendsMqMessage`, `triggerBundleEncryptionWithException_updatesBundleStatusToError`, `testEncryption`.
- `.../util/EncryptionHelperTest`: `testEncryptionEnabledForApplicationMetadata`, `testEncryptionEnabledForFeedbackMessage`, `testEncryptionDisabledForApplicationMetadata`, `testEncryptionDisabledForFeedbackMessage`.
- `appstore-bundle-service-external/appstore-metadata-service-client/.../AppstoreMetadataServiceClientTest`: `shouldExtractApplicationMetadataByAppIdOnSuccess`, `shouldReturnEmptyOptionalWhenApplicationWithAppIdNotFound`, `shouldCloseCircuitWhenFailureThresholdPassedOnGetApplicationByAppId`, and the three equivalents for `getApplicationByIdAndMaintainerCode`.
- `appstore-bundle-service-external/client-common/.../ClientInvokerTest`: `circuitOpensOnExceptionWhenInvoke`, `callRejectedIfBulkheadFullWhenInvoke`, `circuitOpensOnExceptionWhenInvokeWithFallback`, `fallbackAppliedIfBulkheadFullWhenInvoke`, `circuitOpensOnExceptionWhenInvokeAsync`, `callRejectedIfBulkheadFullWhenInvokeAsync`, `circuitOpensOnExceptionWhenRunAsync`, `circuitClosesOnSuccessfulCallsWhenRunAsync`, `callRejectedIfBulkheadFullWhenInvokeRunnable`, `runsCircuitBreakerOnlyIfNullBulkheadWhenInvoke`, `runsCircuitBreakerOnlyIfNullBulkheadWhenInvokeCompletionStage`, `runsCircuitBreakerOnlyIfNullBulkheadWhenInvokeRunnable`.
- `.../ClientInvokerFactoryTest`: `givenCircuitBreakerConfigWhenCreateInvokerThenCreated`, `givenCircuitBreakerConfigWhenRecoverableExceptionThenIgnored`, `givenCircuitBreakerConfigWithBulkheadWhenCreateInvokerThenCreated`.
- `appstore-bundle-service-external/rabbitmq-client/.../EncryptionMessageFactoryTest`: `doesNotAllowNullEnvironmentAndBundleExtension`, `whenCreateEncryptionMessageFromValidBundleThenCreatedWithValidOciBundleUrl`.
- `appstore-bundle-service-application/.../AppStoreBundleConfigurationTest`: `sanityCheck`.

### 2.4 ASBS — Testcontainers integration tests (`appstore-bundle-service appstore-bundle-service-test/src/test/java/com/lgi/appstorebundle/test/tests/`) `[VERIFIED]`

- `StartBundleGenerationMockedIT`: `callRequestBundleWhenAppNotExistsInASMS_verifyNotFoundResponse`, `callRequestBundleWhenAppForMaintainerNotExistsInASMS_verifyNotFoundResponse`, `callRequestBundleWhenGenerationNotTriggered_verifyStartingGenerationWithSendingMessageAndSavingToDB_maintainerSetsFieldEncryptionTrue`, `..._maintainerSetsFieldEncryptionFalse`, `callRequestBundleWhenGenerationHadError_verifyStartingNewGenerationWithSendingMessageAndSavingToDB`, `callRequestBundleWhenGenerationWasAlreadyTriggered_verifyNotSendingMessageAndNotSavingToDB`.
- `StartBundleGenerationEncryptionDisabledMockedIT`: `callRequestBundleWhenGenerationNotTriggered_verifyStartingGenerationWithSendingMessageAndSavingToDB` (parameterized, with `bundle.encryption.enabled = false`).
- `ConsumeFeedbackMessageMockedIT`: `receivedNewerFeedbackMessage_saveNewStatus`, `receivedNewerFeedbackMessage_whenEncryptionEnabledAndBundleStatusIsGenerationCompleted_publishEncryptionMessage`, `receivedOlderFeedbackMessage_doNotSaveNewStatus`, `receivedFeedbackMessageWithoutXRequestId_doNotSaveNewStatus`, `receivedFeedbackMessageWithoutMessageTimestamp_doNotSaveNewStatus`.
- `GetInfoIT`: `callServiceInfo_verifySuccessfulResponse`.
- Bases: `BaseContainersIT`, `MockedBaseIT`.

### 2.5 Caching service `[VERIFIED]`

- `appstore-caching-service appstore-caching-service-test/src/test/java/com/lgi/appstore/cache/test/mocked/NginxITCase`: `shouldForwardRequestToBackendWhenFileIsAbsent`, `shouldDownloadBundleIfPresent`, `shouldGenerateXRequestIdInNginx`, `shouldTakeXRequestIdFromClient`, `shouldReturnInternalServerErrorWhenASBSIsUnreachable`, `shouldHaveAccessToReadBundlesOnNFS`.
- `.../real/NginxTest`: `shouldDownloadJsonFromRealService`.
- Framework helpers: `AppStoreCachingContainer`, `AppStoreCachingStep`, `WiremockStep`.

---

## 3. Coverage reconciliation

Legend: **Covered** = an existing test asserts the behaviour; **Partial** = adjacent behaviour is asserted but not the specific branch; **GAP** = no existing test found.

| Derived case | URS | Existing coverage | Verdict |
| --- | --- | --- | --- |
| TC-M01, TC-M06, TC-M08 | URS-M01, M06, M08 | `ManageMaintainerApiFTSpecSanity` `CRUD operations check`, `ManageMaintainerApiFTSpecSmoke` | Covered (sanity/smoke only, needs a live environment) |
| TC-M02 | URS-M02 | none for maintainers (only the application analogue `second attempt to create same application should be rejected`) | GAP |
| TC-M03 | URS-M03 | `MaintainerAppsControllerTest.cannotGetDetailsOfANonExistingApplication` covers apps, not maintainers | GAP |
| TC-M04, TC-M05 | URS-M04, M05 | `ManageMaintainerApiFTSpec` `query for maintainers list filtering by name #scenario`, `... limit=#limit offset=#offset` | Covered |
| TC-M07 | URS-M07 | none | GAP (delete-with-applications 409) |
| TC-M09 | URS-M09, M10 | `MaintainerApiFTSpec` `create non-existing app and view details`, `MaintainerAppsControllerTest.canAddApplication`, `...WhenEncryptedTrue` | Covered |
| TC-M10 | URS-M11 | `maintainer.PersistentAppsServiceTest.cannotAddApplicationForNonExistentMaintainer`, `maintainerNotFoundExceptionIsThrown` | Covered |
| TC-M11 | URS-M12 | `MaintainerApiFTSpec` `second attempt to create same application should be rejected` | Covered |
| TC-M12 | URS-M13 | `MaintainerAppsControllerTest.cannotAddApplicationWithout*` and `...WithBlank*` | Covered |
| TC-M13, TC-M14 | URS-M14, M15 | `MaintainerAppsControllerTest.canGetDetailsByJustApplicationId*`, `...AndVersion*`, `...AndLatestVersion*` | Covered |
| TC-M15 | URS-M15, M20 | none — no test sets `preferred` and asserts selection | GAP (preferred-version selection) |
| TC-M16 | URS-M16 | `MaintainerAppsControllerTest.canListAppsBy*`, `MaintainerApiFTSpec` `queries for applications list returns apps for #behavior` | Covered |
| TC-M17 | URS-M17 | `MaintainerApiFTSpec` `details of each version ... all available versions` | Partial (visibility split not asserted directly) |
| TC-M18, TC-M19 | URS-M18, M19 | `MaintainerApiFTSpec` `update application details for #field - PUT ... (by ID alone)`, `... for specific version (non-latest)`, `... for param newVersion`; `maintainer.PersistentAppsServiceTest.canUpdateLatestApplication` | Covered |
| TC-M20 | URS-M20 | none | GAP (preferred exclusivity) |
| TC-M21 | URS-M21 | none (no rollback/failure-injection test) | GAP |
| TC-M22 | URS-M22 | `StbApiFTSpec` `details ... all visible versions` indirectly | Partial (`latest.maintainer` vs `latest.stb` divergence untested) |
| TC-M23 | URS-M23 | `PersistentAppsServiceTest.applicationDetailsVersionsAreSorted` (both perspectives) | Partial (sorting of the version list is tested; numeric latest selection is not) |
| TC-M24, TC-M25 | URS-M24, M25 | `MaintainerApiFTSpec` `deletes application with #behavior`, `delete by not existing version or id`, `consecutive deletes of application versions`; `maintainer.PersistentAppsServiceTest.canDelete*` | Covered |
| TC-M26 | URS-M26 | `AppIdWithVersionTest.willThrowAnExceptionIfMoreThanTwoTokensArePresent`, `...IfLessThanTwoTokensArePresent` | Covered |
| TC-S01, TC-S06, TC-S07, TC-S08 | URS-S01, S06, S07, S08 | `StbAppsControllerTest.canListAppsBy*`, `cannotListAppsByIncorrectPlatform*`, `cannotListAppsByIncorrectCategory*`, `StbApiFTSpec` `... filter by #queryParam` | Covered |
| TC-S02, TC-S19 | URS-S02, S21 | `StbApiFTSpec` `details ... all visible versions`, `StbAppsControllerTest.cannotGetDetailsOfANonExistingApplication` | Partial (invisible-version 404 not asserted explicitly) |
| TC-S03 | URS-S03 | `StbApiFTSpec` `... returns apps in latest versions in amount corresponding to given limit/offset` | Covered |
| TC-S04 | URS-S04 | none | GAP (preferred de-duplication in list results) |
| TC-S05 | URS-S05 | `StbAppsControllerTest.canListAppsByName*`, `...ByDescription*` | Partial (case-insensitivity not asserted) |
| TC-S09 | URS-S09 | `stb.PersistentAppsServiceTest.omittingLimitParamReturnsAllApplications`, `passingLimitParam{Greater,Less,Equal}...`, `passingZeroLimitReturnsAllApplications`, `StbAppsControllerTest.canListAppsByOffset*` | Covered for limit; GAP for offset beyond total and negative offset/limit |
| TC-S10 | URS-S10 | `StbApiFTSpec` limit/offset spec asserts metadata | Covered |
| TC-S11 | URS-S11 | `StbAppsControllerTest.canGetDetailsBy*` | Covered |
| TC-S12 | URS-S13 | `StbAppsControllerTest.cannotGetApplicationDetailsWithoutPlatformName`, `...WithoutFirmwareVer`, `cannotListNativeApplicationWhen*Absent`, `PlatformAndVersionOptionalForWebValidatorTest` | Covered |
| TC-S13 | URS-S14, S16 | `StbAppsControllerTest.canListWebApplication`, `PlatformAndVersionOptionalForWebValidatorTest.shouldReturnWebAppWithoutPlatformAndFirmware`, `stb.PersistentAppsServiceTest.shouldReturnSourceUrlForWebAndAndroidApplication` | Covered |
| TC-S14 | URS-S15 | `StbAppsControllerTest.cannotListApplicationWhenTypeIsUnsupported`, `PlatformAndVersionOptionalForWebValidatorTest.throwExceptionIfApplicationIsNotSupported` | Covered |
| TC-S15, TC-S16 | URS-S17, S18 | `ApplicationUrlCreatorTest`, `ApplicationUrlServiceTest`, `stb.PersistentAppsServiceTest.shouldReturnUrlForNativeApplication` | Covered |
| TC-S17, TC-S18 | URS-S19, S20 | `StbApiFTSpec` `details of each version ...`, `stb.PersistentAppsServiceTest.applicationDetailsVersionsAreSorted` | Covered |
| TC-B01 | URS-B02 | none (all tests send the header) | GAP (missing `x-request-id` on the REST endpoint) |
| TC-B02, TC-B03 | URS-B03, B04, B05 | `AppStoreBundleControllerTest.returnErrorWhenApplication*NotExists*`, `StartBundleGenerationMockedIT.callRequestBundleWhenApp*NotExistsInASMS_verifyNotFoundResponse` | Covered |
| TC-B04 | URS-B06 | `AppstoreMetadataServiceClientTest.shouldCloseCircuitWhenFailureThresholdPassed*` (exception path) | Partial (HTTP 500 propagation to the REST response untested) |
| TC-B05 | URS-B07, B08, B11, B12 | `BundleServiceTest.triggerBundleGeneration_*`, `StartBundleGenerationMockedIT.callRequestBundleWhenGenerationNotTriggered_*`, `AppStoreBundleControllerRealRequestsTest.givenValidRequestWhenGenerationNotStartedThenGenerationRequested` | Covered |
| TC-B06 | URS-B09 | `AppStoreBundleControllerTest.returnRetryAfterWithoutStartingBundleGeneration`, `StartBundleGenerationMockedIT.callRequestBundleWhenGenerationWasAlreadyTriggered_*`, `AppStoreBundleControllerRealRequestsTest.givenValidRequestWhenGenerationRequestedThenGenerationSkipped` | Covered |
| TC-B07 | URS-B08, B10 | `AppStoreBundleControllerTest.returnRetryAfterAndTriggerNewBundleGenerationBecauseOfBundleErrorStatus`, `StartBundleGenerationMockedIT.callRequestBundleWhenGenerationHadError_*` | Covered |
| TC-B08 | URS-B07 | none | **GAP — `getLatestBundle` ordering.** No test inserts multiple rows for the same coordinates, so the ascending `coalesce(updated_at, created_at)` ordering (returns the oldest row) is unasserted (`appstore-bundle-service appstore-bundle-service-storage/storage-persistent/src/main/java/com/lgi/appstorebundle/storage/persistent/JooqBundleDao.java:54-64`) |
| TC-B09 | URS-B16, B17 | `AppStoreBundleControllerTest.returnRetryAfter*` (all) | Covered |
| TC-B10 | URS-B15 | `BundleServiceTest.triggerBundleGenerationWithException_*`, `AppStoreBundleControllerRealRequestsTest.givenValidRequestWhenTriggeringGenerationThrowsExceptionThenInternalServerError` | Covered |
| TC-B11 | URS-B12, B13, B14 | `StartBundleGenerationMockedIT` asserts a message is sent; `EncryptionMessageFactoryTest.whenCreateEncryptionMessageFromValidBundleThenCreatedWithValidOciBundleUrl` | Partial (default-exchange/routing-key semantics and the `x-request-id` message header are not asserted) |
| TC-B12, TC-B13 | URS-B19, B24 | `ConsumeFeedbackMessageMockedIT.receivedNewerFeedbackMessage_saveNewStatus`, `receivedOlderFeedbackMessage_doNotSaveNewStatus` | Covered |
| TC-B14, TC-B15 | URS-B20, B21 | `ConsumeFeedbackMessageMockedIT.receivedFeedbackMessageWithoutXRequestId_*`, `...WithoutMessageTimestamp_*` | Covered |
| TC-B16, TC-B17 | URS-B22, B23 | none | GAP (unknown `phaseCode`, malformed payload) |
| TC-B18, TC-B19, TC-B20 | URS-B26 | `EncryptionHelperTest.testEncryption{Enabled,Disabled}ForApplicationMetadata`, `AppStoreBundleControllerTest.returnRetryAfterWithEncryption{Enabled,Disabled}`, `StartBundleGenerationMockedIT` encryption-true/false cases, `StartBundleGenerationEncryptionDisabledMockedIT` | Covered |
| TC-B21, TC-B22 | URS-B25, B27, B28 | `ConsumeFeedbackMessageMockedIT.receivedNewerFeedbackMessage_whenEncryptionEnabledAndBundleStatusIsGenerationCompleted_publishEncryptionMessage`, `EncryptionHelperTest.testEncryption{Enabled,Disabled}ForFeedbackMessage`, `BundleServiceTest.testEncryption` | Covered |
| TC-B23 | URS-B26, B27 | none | **GAP — encryption drift.** No test flips the ASMS flag after the bundle row is created, so the request-path (ASMS flag) versus feedback-path (persisted column) divergence is unasserted (`appstore-bundle-service appstore-bundle-service-application/src/main/java/com/lgi/appstorebundle/util/EncryptionHelper.java:43-50`) |
| TC-B24, TC-B25 | URS-B29, B28 | `BundleServiceTest.triggerBundleEncryptionWithNotExistingBundle_skipTriggeringEncryption`, `...WithException_updatesBundleStatusToError` | Covered |
| TC-B26 | URS-B30 | none (only generation feedback ordering is tested) | GAP (encryption feedback ordering) |
| TC-B27 | URS-X20, X21 | `GetInfoIT.callServiceInfo_verifySuccessfulResponse` | Partial (`health` and `prometheus` untested) |
| TC-B28, TC-B29, TC-B30 | URS-X13, X14, X15, X16 | `ClientInvokerTest` (12 methods), `ClientInvokerFactoryTest.givenCircuitBreakerConfigWhenRecoverableExceptionThenIgnored`, `AppstoreMetadataServiceClientTest.shouldCloseCircuitWhenFailureThresholdPassed*` | Covered at unit level; **GAP** end-to-end: no test asserts the HTTP response the caller receives while the breaker is open or the bulkhead is full |
| TC-C01 | URS-C01, C02 | `NginxITCase.shouldDownloadBundleIfPresent` | Covered |
| TC-C02 | URS-C03, C04 | `NginxITCase.shouldForwardRequestToBackendWhenFileIsAbsent` | Covered |
| TC-C03, TC-C04 | URS-C05 | `NginxITCase.shouldGenerateXRequestIdInNginx`, `shouldTakeXRequestIdFromClient` | Covered |
| TC-C05 | URS-C09 | `NginxITCase.shouldReturnInternalServerErrorWhenASBSIsUnreachable` | Covered |
| TC-C06 | URS-C06 | none | GAP (`/platforms` and `/bundles` routing to `asbm-backend`) |
| TC-C07, TC-C08 | URS-C07, C08 | none | **GAP — CORS behaviour.** No test issues an `OPTIONS` preflight or asserts CORS response headers on any route |
| TC-C09 | URS-C10, C11 | none | GAP (port 8081 `/healthcheck`, `/ping`, `deny all`) |
| TC-C10 | URS-C13 | `NginxITCase.shouldHaveAccessToReadBundlesOnNFS` | Covered |
| TC-C11 | URS-C12 | `NginxTest.shouldDownloadJsonFromRealService` (real environment) | Partial |
| TC-C12 | URS-C15 | none | GAP (chart rendering / missing `ASBM_SERVICE`) |
| TC-X01, TC-X02 | URS-X01, X02, X03, X04 | `NginxITCase.shouldGenerateXRequestIdInNginx`, `shouldTakeXRequestIdFromClient` cover the edge only | GAP for the ASMS and ASBS `CorrelationIdFilter` response header and MDC behaviour |
| TC-X03 | URS-X05 | `ConsumeFeedbackMessageMockedIT.receivedFeedbackMessageWithoutXRequestId_*` (negative only) | Partial |
| TC-X04, TC-X05 | URS-X06, X07, X08, X09 | Error status codes are asserted throughout the controller tests | Partial (error body shape and absence of stack traces unasserted) |
| TC-X06, TC-X07 | URS-X10, X12 | `MaintainerApiFTSpec` `developer cannot access other developer application (GET/PUT/DELETE)` | Covered at the functional level — worth re-reading, because ASMS itself performs no ownership check, so this spec documents behaviour achieved through path scoping rather than authorization `[INFERRED]` |
| TC-X08 | URS-X23 | `JsonProcessorHelperTest.shouldDeserializeJsonRegardlessOfUnknownProperties`, `ConfigurationFTSpecSanity` | Covered |
| TC-X09 | URS-X24 | Implicit in every Testcontainers-based test (`PostgresContainerInitializer`, `BaseContainersIT`) | Covered implicitly |

### 3.1 Prioritized gap list

1. **`getLatestBundle` returns the oldest row** (TC-B08) — behavioural defect with no regression test; a test inserting two rows for identical coordinates would fail today `[VERIFIED]` code, `[INFERRED]` severity.
2. **Encryption drift between request and feedback paths** (TC-B23) — untested divergence that can produce a bundle whose encryption contradicts current metadata.
3. **CORS behaviour at the edge** (TC-C07, TC-C08) — an entirely untested, permissive (`Access-Control-Allow-Origin: *` with credentials allowed) configuration.
4. **Pagination edge cases** (TC-S09, TC-M05) — offset beyond total, negative offset/limit and the `limit=0` "unlimited" contract are only partially covered.
5. **Circuit-breaker open behaviour end to end** (TC-B28, TC-B30) — unit-tested in isolation, but the HTTP response an STB sees during an ASMS outage is unasserted.
6. **Preferred-version semantics** (TC-M15, TC-M20, TC-S04) — the `preferred` column (migration V05) has no functional test asserting selection or exclusivity.
7. **Missing `x-request-id` on the ASBS REST endpoint** (TC-B01) and **correlation filter behaviour in both services** (TC-X01, TC-X02).
8. **Feedback robustness** (TC-B16, TC-B17, TC-B26) — unknown `phaseCode`, malformed payload and encryption-feedback ordering.
9. **Caching service routes other than the bundle path** (TC-C06, TC-C09, TC-C12) — `asbm-backend` routing, the management port and the missing `ASBM_SERVICE` chart value.
10. **Maintainer lifecycle negative cases** (TC-M02, TC-M03, TC-M07) — duplicate maintainer, unknown maintainer read, and refusal to delete a maintainer that still owns applications.

## Open questions

1. Are the `cases/real/` sanity and smoke specs executed in CI, or only against deployed environments? They require a live base URL `[INFERRED]`.
2. Is there an external contract or end-to-end suite (outside these repos) that exercises the worker feedback loop and the shared bundle volume?
3. Should the caching service tests assert CORS, given the headers are permissive by design?

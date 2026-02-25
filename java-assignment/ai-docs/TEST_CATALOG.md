# Test Catalog (`src/test/java`)

## Purpose
This document lists all tests under `src/test/java`, what each test verifies, and the expected outcome when it passes.

## Test Infrastructure (non-`@Test` class)

### `com.fulfilment.application.monolith.testinfra.ReusablePostgresTestResource`
Purpose:
- Provides a reusable PostgreSQL datasource for Quarkus runtime tests.
- Supports Testcontainers mode and external-datasource fallback mode.

Outcome when functioning correctly:
- Runtime tests receive datasource properties.
- Tests can run with either:
  - Docker/Testcontainers (`e2e.testcontainers.enabled=true`), or
  - External DB (`test.db.*`) when fallback is allowed.

## Unit Tests

### `com.fulfilment.application.monolith.location.LocationGatewayTest`
1. `testWhenResolveExistingLocationShouldReturn`
Purpose: verifies a known location identifier resolves.
Expected outcome: returned `Location` is not null and identifier is `ZWOLLE-001`.

2. `testWhenResolveByIdentifierWithNullShouldReturnNull`
Purpose: validates null-safe resolver behavior.
Expected outcome: returns `null`.

3. `testWhenResolveByIdentifierWithBlankShouldReturnNull`
Purpose: validates blank input handling.
Expected outcome: returns `null`.

4. `testWhenResolveUnknownLocationShouldReturnNull`
Purpose: verifies unknown identifier handling.
Expected outcome: returns `null`.

5. `testWhenResolveIdentifierWithSpacesAndDifferentCaseShouldReturnLocation`
Purpose: verifies trim + case-insensitive normalization.
Expected outcome: returned `Location` is not null with identifier `ZWOLLE-001`.

### `com.fulfilment.application.monolith.products.ProductResourceUnitTest`
1. `testGetShouldReturnAllProducts`
Purpose: verifies list endpoint delegates to repository and returns product list.
Expected outcome: returned list mirrors repository output and sort argument is provided.

2. `testGetSingleShouldThrowWhenMissing`
Purpose: validates get-by-id not-found path.
Expected outcome: throws `WebApplicationException` with status `404`.

3. `testGetSingleShouldReturnEntityWhenFound`
Purpose: validates get-by-id happy path.
Expected outcome: returns located product entity.

4. `testCreateShouldRejectPreSetId`
Purpose: validates create guard for preset identifiers.
Expected outcome: throws `WebApplicationException` with status `422`.

5. `testCreateShouldPersistAndReturnCreated`
Purpose: verifies create happy path persistence and response mapping.
Expected outcome: repository `persist` invoked and response status is `201`.

6. `testUpdateShouldRejectMissingName`
Purpose: validates update required-name guard.
Expected outcome: throws `WebApplicationException` with status `422`.

7. `testUpdateShouldThrowWhenEntityMissing`
Purpose: validates update not-found path.
Expected outcome: throws `WebApplicationException` with status `404`.

8. `testUpdateShouldMutateAndPersistEntity`
Purpose: verifies update mutation of editable fields and persistence call.
Expected outcome: existing entity is updated and persisted.

9. `testDeleteShouldThrowWhenEntityMissing`
Purpose: validates delete not-found path.
Expected outcome: throws `WebApplicationException` with status `404`.

10. `testDeleteShouldRemoveEntityAndReturnNoContent`
Purpose: verifies delete happy path.
Expected outcome: repository `delete` invoked and response status is `204`.

### `com.fulfilment.application.monolith.products.ProductResourceErrorMapperTest`
1. `testToResponseShouldMapWebApplicationExceptionStatusAndMessage`
Purpose: verifies mapper preserves HTTP status for `WebApplicationException`.
Expected outcome: response status/message fields match exception.

2. `testToResponseShouldMapGenericExceptionTo500`
Purpose: verifies non-web exceptions map to internal server error.
Expected outcome: response status is `500` and payload includes exception metadata.

3. `testToResponseShouldOmitErrorFieldWhenMessageIsNull`
Purpose: validates null-message behavior in mapper payload.
Expected outcome: response status is `500` and `error` field is absent/null.

### `com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentRepositoryTest`
1. `testCountDistinctWarehousesByStoreAndProduct`
Purpose: verifies aggregate query parameter mapping for store+product count.
Expected outcome: returns typed query result with expected query parameters.

2. `testCountDistinctWarehousesByStore`
Purpose: verifies store-level distinct warehouse count query path.
Expected outcome: returns typed query result and correct store parameter.

3. `testCountDistinctProductsByWarehouse`
Purpose: verifies warehouse-level distinct product count query path.
Expected outcome: returns typed query result and correct warehouse parameter.

### `com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentServiceTest`
1. `testAssignShouldRejectInvalidInput`
Purpose: validates required-field guard before any repository access.
Expected outcome: throws `IllegalArgumentException`.

2. `testAssignShouldFailWhenStoreDoesNotExist`
Purpose: verifies assign flow when store entity is missing.
Expected outcome: throws `IllegalArgumentException`.

3. `testAssignShouldFailWhenProductDoesNotExist`
Purpose: verifies assign flow when product entity is missing.
Expected outcome: throws `IllegalArgumentException`.

4. `testAssignShouldFailWhenWarehouseDoesNotExist`
Purpose: verifies assign flow when target warehouse is missing/archived.
Expected outcome: throws `IllegalArgumentException`.

5. `testAssignShouldReturnExistingAssignmentWhenAlreadyPresent`
Purpose: verifies idempotent assignment behavior.
Expected outcome: existing assignment is returned and new persist is skipped.

6. `testAssignShouldFailWhenStoreAndProductAlreadyUseTwoWarehouses`
Purpose: enforces max 2 warehouses per product per store rule.
Expected outcome: throws `IllegalStateException`.

7. `testAssignShouldFailWhenStoreAlreadyUsesThreeWarehouses`
Purpose: enforces max 3 warehouses per store rule.
Expected outcome: throws `IllegalStateException`.

8. `testAssignShouldFailWhenWarehouseAlreadyStoresFiveProducts`
Purpose: enforces max 5 products per warehouse rule.
Expected outcome: throws `IllegalStateException`.

9. `testAssignShouldPersistAssignmentWhenValid`
Purpose: verifies happy-path assignment creation.
Expected outcome: new assignment is persisted with populated fields and timestamp.

10. `testListAllShouldDelegateToRepository`
Purpose: verifies list-all delegation path.
Expected outcome: returns repository list without transformation.

### `com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentResourceTest`
1. `testListAllShouldDelegateToService`
Purpose: verifies resource list endpoint delegation.
Expected outcome: service list is returned directly.

2. `testCreateShouldDelegateAndReturnAssignment`
Purpose: verifies request-to-service mapping for create endpoint.
Expected outcome: service receives request fields and returns assignment.

3. `testCreateShouldMapIllegalArgumentExceptionToBadRequest`
Purpose: validates API translation for validation failures.
Expected outcome: `IllegalArgumentException` maps to `400`.

4. `testCreateShouldMapIllegalStateExceptionToConflict`
Purpose: validates API translation for business-rule conflicts.
Expected outcome: `IllegalStateException` maps to `409`.

### `com.fulfilment.application.monolith.stores.LegacyStoreManagerGatewayTest`
1. `testPublishStoreEventShouldIncreaseProcessedCount`
Purpose: verifies successful publish increments processed events.
Expected outcome: processed event count becomes `1`.

2. `testPublishStoreEventShouldIgnoreDuplicateIdempotencyKey`
Purpose: verifies idempotency guard for duplicate publications.
Expected outcome: second publish with same idempotency key is ignored; processed count remains `1`.

3. `testPublishStoreEventShouldFailWhenFailNextPublicationEnabled`
Purpose: verifies one-shot failure mode.
Expected outcome: publish throws `IllegalStateException`; processed count stays `0`.

4. `testPublishStoreEventShouldFailWhenAlwaysFailEnabled`
Purpose: verifies persistent failure mode.
Expected outcome: publish throws `IllegalStateException`; processed count stays `0`.

5. `testClearTestStateShouldResetFailureFlagsAndIdempotencyCache`
Purpose: verifies reset behavior for idempotency cache and failure flags.
Expected outcome: after reset, publish with previous idempotency key succeeds and processed count is `1`.

6. `testPublishStoreEventShouldWrapWriteFailures`
Purpose: verifies write failure path is wrapped consistently.
Expected outcome: invalid payload triggers `IllegalStateException`; processed count stays `0`.

### `com.fulfilment.application.monolith.stores.StoreResourceErrorMapperTest`
1. `testToResponseShouldMapWebApplicationExceptionStatusAndMessage`
Purpose: verifies HTTP status and message mapping for `WebApplicationException`.
Expected outcome: response code matches exception status (`422`) and error payload contains message.

2. `testToResponseShouldMapGenericExceptionTo500`
Purpose: verifies generic exception mapping to internal server error.
Expected outcome: response code is `500` and payload includes exception type/message.

3. `testToResponseShouldOmitErrorFieldWhenMessageIsNull`
Purpose: verifies null-message handling in error payload.
Expected outcome: response code is `500` and `error` field is absent/null.

### `com.fulfilment.application.monolith.stores.StoreResourceValidationTest`
1. `testCreateShouldFailWhenIdIsPreset`
Purpose: validates create guard for pre-populated IDs.
Expected outcome: throws `WebApplicationException` with status `422`.

2. `testCreateShouldPersistAndEnqueueStoreCreatedEvent`
Purpose: validates create happy path without persistence runtime by using a test-double store.
Expected outcome: returns `201`, marks store as persisted, and enqueues `StoreCreated`.

3. `testUpdateShouldFailWhenNameMissing`
Purpose: validates update guard for missing store name.
Expected outcome: throws `WebApplicationException` with status `422`.

### `com.fulfilment.application.monolith.stores.StoreResourceUnitTest`
1. `testGetShouldReturnStoresFromGateway`
Purpose: verifies list endpoint delegates to gateway and preserves sort semantics.
Expected outcome: returned list is sorted by store name.

2. `testGetSingleShouldThrow404WhenMissing`
Purpose: validates get-by-id not-found branch.
Expected outcome: throws `WebApplicationException` with status `404`.

3. `testGetSingleShouldReturnExistingStore`
Purpose: validates get-by-id happy path.
Expected outcome: returns the existing store instance.

4. `testUpdateShouldThrow404WhenMissing`
Purpose: validates update not-found branch.
Expected outcome: throws `WebApplicationException` with status `404`.

5. `testUpdateShouldMutateExistingStoreAndEnqueueEvent`
Purpose: verifies update mutation path and outbox dispatch.
Expected outcome: entity fields are updated and `StoreUpdated` is enqueued.

6. `testPatchShouldThrow404WhenMissing`
Purpose: validates patch not-found branch.
Expected outcome: throws `WebApplicationException` with status `404`.

7. `testPatchShouldUpdateNameAndIgnoreNegativeStock`
Purpose: validates patch rules for name updates and negative stock guard.
Expected outcome: name is updated, stock remains unchanged, and `StorePatched` is enqueued.

8. `testPatchShouldUpdateStockWhenNonNegativeAndKeepNameWhenNull`
Purpose: validates partial patch when only stock changes.
Expected outcome: stock is updated, name remains unchanged, and `StorePatched` is enqueued.

9. `testDeleteShouldThrow404WhenMissing`
Purpose: validates delete not-found branch.
Expected outcome: throws `WebApplicationException` with status `404`.

10. `testDeleteShouldReturnNoContentAndDeleteStore`
Purpose: verifies delete happy path and outbox dispatch.
Expected outcome: returns `204`, removes store via gateway, and enqueues `StoreDeleted`.

11. `testCreateShouldPersistViaGateway`
Purpose: verifies create happy path persistence through `StoreGateway`.
Expected outcome: returns `201`, assigns id through gateway, and enqueues `StoreCreated`.

### `com.fulfilment.application.monolith.stores.outbox.OutboxPublisherMetricsTest`
1. `testRecordSuccessAndFailureShouldUpdateCountersAndAverage`
Purpose: verifies success/failure counters and average latency calculation.
Expected outcome: counters increment and average latency is computed correctly.

2. `testGetAveragePublishLatencyShouldReturnZeroWhenNoSuccess`
Purpose: validates zero-success baseline behavior.
Expected outcome: average publish latency is `0`.

3. `testResetShouldClearAllCounters`
Purpose: verifies metric reset semantics.
Expected outcome: published/failed/latency values reset to `0`.

### `com.fulfilment.application.monolith.stores.outbox.StoreOutboxServiceTest`
1. `testEnqueueStoreChangedShouldCreateOutboxMessage`
Purpose: verifies message creation and payload/correlation metadata mapping.
Expected outcome: repository `create` receives populated message with serialized payload.

2. `testEnqueueStoreChangedShouldThrowWhenPayloadSerializationFails`
Purpose: validates serialization failure handling.
Expected outcome: throws `IllegalStateException` and no message is written.

### `com.fulfilment.application.monolith.stores.outbox.OutboxPublisherTest`
1. `testPublishPendingShouldPublishValidMessages`
Purpose: verifies successful relay flow for pending messages.
Expected outcome: message is marked published, metrics success increments, and legacy publish is invoked.

2. `testPublishPendingShouldMarkFailedWhenPayloadCannotBeParsed`
Purpose: verifies failure branch and retry scheduling when payload is invalid.
Expected outcome: message is marked failed with retry timestamp and failure metrics increment.

3. `testPublishPendingShouldReturnZeroWhenNoPendingMessages`
Purpose: verifies no-op behavior for empty batch.
Expected outcome: returns `0` and no repository mark methods are called.

4. `testStartShouldSkipSchedulerWhenDisabled`
Purpose: validates config-controlled scheduler disable behavior.
Expected outcome: scheduler is not created.

5. `testStartAndShutdownShouldManageSchedulerLifecycle`
Purpose: validates scheduler lifecycle when enabled.
Expected outcome: scheduler starts and then shuts down cleanly.

### `com.fulfilment.application.monolith.stores.outbox.OutboxAdminResourceTest`
1. `testStatsShouldReturnRepositoryAndRelayMetrics`
Purpose: verifies stats endpoint aggregation of repository counts and relay metrics.
Expected outcome: response map returns expected pending/failed/published/relay values.

2. `testPublishNowShouldReturnProcessedCount`
Purpose: verifies manual relay endpoint response.
Expected outcome: returns `processed` count from relay execution.

3. `testReplayShouldFailWhenRequiredParamsMissing`
Purpose: validates required query params for replay endpoint.
Expected outcome: throws `WebApplicationException` with status `400`.

4. `testReplayShouldFailWhenDateFormatIsInvalid`
Purpose: validates replay datetime format.
Expected outcome: throws `WebApplicationException` with status `400`.

5. `testReplayShouldFailWhenToIsBeforeFrom`
Purpose: validates replay range ordering.
Expected outcome: throws `WebApplicationException` with status `400`.

6. `testReplayShouldResetMetricsAndReturnAffectedCount`
Purpose: verifies replay execution plus optional metric reset behavior.
Expected outcome: returns affected row count, captures parsed window, and metrics reset to zero.

### `com.fulfilment.application.monolith.stores.outbox.OutboxMessageRepositoryTest`
1. `testCreateShouldPersistMessage`
Purpose: verifies repository create path delegates to persistence.
Expected outcome: message is persisted.

2. `testListPendingShouldApplyQueryAndPaging`
Purpose: verifies pending query predicate, ordering, and paging behavior.
Expected outcome: query/paging arguments are correct and pending list is returned.

3. `testMarkPublishedShouldSetFieldsAndMerge`
Purpose: verifies mark-published state updates.
Expected outcome: `publishedAt` is set, `lastError` is cleared, and entity is merged.

4. `testMarkFailedShouldIncrementAttemptsAndTruncateError`
Purpose: verifies retry bookkeeping and error truncation behavior.
Expected outcome: attempts increment, error is truncated, retry timestamp is set.

5. `testMarkFailedShouldKeepNullErrorAsNull`
Purpose: verifies null-error handling in failure path.
Expected outcome: attempts increment and `lastError` remains null.

6. `testCountMethodsShouldUseExpectedPredicates`
Purpose: verifies count query predicates for pending/failed/published states.
Expected outcome: each count method returns mapped result for expected predicate.

7. `testReplayShouldResetPublicationFieldsAndReturnAffectedRows`
Purpose: verifies replay update query and parameter mapping.
Expected outcome: publication fields are reset for filtered rows and affected count is returned.

8. `testClearAllShouldDeleteAllRows`
Purpose: verifies maintenance clear operation.
Expected outcome: repository delete-all is invoked.

9. `testSetNextAttemptAtShouldCallUpdateQuery`
Purpose: verifies next-attempt scheduling update.
Expected outcome: update query is invoked with expected id and timestamp.

### `com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouseTest`
1. `testFromWarehouseShouldMapAllFields`
Purpose: verifies domain-to-entity mapping.
Expected outcome: all fields are mapped to `DbWarehouse`.

2. `testToWarehouseShouldMapAllFields`
Purpose: verifies entity-to-domain mapping.
Expected outcome: all fields are mapped to `Warehouse`.

### `com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepositoryTest`
1. `testGetAllShouldMapActiveEntities`
Purpose: verifies active-list query and mapping.
Expected outcome: only active query predicate is used and entities are mapped to domain models.

2. `testCreateShouldInitializeDefaultsAndBackPropagateEntityFields`
Purpose: verifies create defaulting and id/timestamp back-propagation.
Expected outcome: `createdAt` default is set when missing and generated id is copied to domain.

3. `testCreateShouldKeepProvidedCreatedAt`
Purpose: verifies create path preserves provided creation timestamp.
Expected outcome: provided `createdAt` is not overwritten.

4. `testUpdateShouldUseIdWhenProvided`
Purpose: verifies id-based update path.
Expected outcome: repository resolves by id and updates editable fields.

5. `testUpdateShouldResolveByBusinessUnitWhenIdMissing`
Purpose: verifies business-unit fallback update path.
Expected outcome: repository resolves active warehouse by business unit and updates fields.

6. `testUpdateShouldThrowWhenWarehouseMissing`
Purpose: validates update-not-found behavior.
Expected outcome: throws `IllegalStateException`.

7. `testRemoveShouldDeleteWhenEntityFound`
Purpose: verifies remove happy path.
Expected outcome: found entity is deleted.

8. `testRemoveShouldDoNothingWhenEntityMissing`
Purpose: verifies remove no-op behavior.
Expected outcome: no delete call occurs when entity is absent.

9. `testFindByBusinessUnitCodeShouldMapEntity`
Purpose: verifies active-business-unit lookup.
Expected outcome: mapped domain warehouse is returned with active predicate.

10. `testFindByIdAsDomainShouldReturnNullWhenMissing`
Purpose: verifies id lookup null behavior.
Expected outcome: returns null when record is absent.

11. `testCountMethodsShouldDelegateToCountQuery`
Purpose: verifies count helpers delegate to expected predicates.
Expected outcome: active count queries are issued for location and business-unit dimensions.

12. `testSumActiveCapacityByLocationShouldReturnIntValue`
Purpose: verifies capacity sum helper query behavior.
Expected outcome: summed capacity is returned as an `int`.

13. `testFindActiveEntityByBusinessUnitCodeShouldReturnEntity`
Purpose: verifies entity-level active lookup.
Expected outcome: active entity is returned for business unit code.

14. `testGetAllIncludingArchivedShouldMapAllRows`
Purpose: verifies list-all path includes archived records.
Expected outcome: all entities are mapped regardless of archived state.

15. `testClearTestWarehousesShouldDeleteNonSeedRowsOnly`
Purpose: verifies cleanup scope for test business-unit prefix.
Expected outcome: delete query targets only `TST-%` business-unit rows.

16. `testFlushShouldCallEntityManagerFlush`
Purpose: verifies explicit flush delegation.
Expected outcome: entity manager flush is invoked.

17. `testLockForMutationShouldAcquireDeterministicAdvisoryLocks`
Purpose: verifies advisory lock acquisition for mutation safeguards.
Expected outcome: business-unit and location lock keys are generated and acquired.

### `com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCaseTest`
1. `testCreateWarehouseSuccess`
Purpose: happy-path create flow.
Expected outcome: no exception; one warehouse is created.

2. `testCreateWarehouseShouldFailWhenBusinessUnitAlreadyExists`
Purpose: enforces active business-unit uniqueness.
Expected outcome: throws `WarehouseConflictException`.

3. `testCreateWarehouseShouldFailWhenLocationIsInvalid`
Purpose: blocks unknown locations.
Expected outcome: throws `WarehouseValidationException`.

4. `testCreateWarehouseShouldFailWhenMaxNumberReached`
Purpose: enforces max warehouses per location.
Expected outcome: throws `WarehouseConflictException`.

5. `testCreateWarehouseShouldFailWhenCapacityExceedsLocationLimit`
Purpose: enforces aggregate capacity limit for location.
Expected outcome: throws `WarehouseValidationException`.

6. `testCreateWarehouseShouldFailWhenStockGreaterThanCapacity`
Purpose: enforces `capacity >= stock`.
Expected outcome: throws `WarehouseValidationException`.

7. `testCreateWarehouseShouldFailWhenRequiredFieldsAreMissing`
Purpose: validates required payload fields.
Expected outcome: throws `WarehouseValidationException`.

8. `testCreateWarehouseShouldFailWhenValuesAreNegative`
Purpose: validates non-negative numeric constraints.
Expected outcome: throws `WarehouseValidationException`.

### `com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCaseTest`
1. `testReplaceShouldFailWhenCurrentWarehouseIsMissing`
Purpose: replacement requires existing active warehouse.
Expected outcome: throws `WarehouseNotFoundException`.

2. `testReplaceShouldFailWhenStockDoesNotMatch`
Purpose: replacement must preserve stock quantity.
Expected outcome: throws `WarehouseValidationException`.

3. `testReplaceShouldFailWhenCapacityCannotAccommodateStock`
Purpose: replacement capacity must accommodate stock.
Expected outcome: throws `WarehouseValidationException`.

4. `testReplaceWarehouseSuccess`
Purpose: happy-path replacement workflow.
Expected outcome: current warehouse is archived; new warehouse is created with target location.

5. `testReplaceShouldFailWhenLocationIsInvalid`
Purpose: blocks replacement to unknown location.
Expected outcome: throws `WarehouseValidationException`.

6. `testReplaceShouldFailWhenTargetLocationMaxWarehousesReached`
Purpose: enforces location max-warehouse rule during replacement.
Expected outcome: throws `WarehouseConflictException`.

7. `testReplaceWarehouseSuccessWhenReplacingWithinSameLocation`
Purpose: verifies same-location replacement branch and capacity recalculation path.
Expected outcome: replacement succeeds, current is archived, replacement remains in same location, and flush is invoked.

8. `testReplaceShouldFailWhenTargetLocationCapacityExceeded`
Purpose: enforces target-location aggregate capacity validation during replacement.
Expected outcome: throws `WarehouseValidationException`.

9. `testReplaceShouldFailWhenPayloadIsNull`
Purpose: validates null payload guard.
Expected outcome: throws `WarehouseValidationException`.

10. `testReplaceShouldFailWhenBusinessUnitCodeMissing`
Purpose: validates required business-unit code field.
Expected outcome: throws `WarehouseValidationException`.

11. `testReplaceShouldFailWhenLocationMissing`
Purpose: validates required location field.
Expected outcome: throws `WarehouseValidationException`.

12. `testReplaceShouldFailWhenCapacityMissing`
Purpose: validates required capacity field.
Expected outcome: throws `WarehouseValidationException`.

13. `testReplaceShouldFailWhenStockMissing`
Purpose: validates required stock field.
Expected outcome: throws `WarehouseValidationException`.

14. `testReplaceShouldFailWhenNegativeValuesProvided`
Purpose: validates non-negative numeric constraints.
Expected outcome: throws `WarehouseValidationException`.

### `com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCaseTest`
1. `testArchiveShouldFailWhenWarehouseIsNull`
Purpose: validates null input handling.
Expected outcome: throws `WarehouseValidationException`.

2. `testArchiveShouldFailWhenAlreadyArchived`
Purpose: prevents re-archiving archived warehouse.
Expected outcome: throws `WarehouseConflictException`.

3. `testArchiveShouldSetArchivedAtAndUpdateWarehouse`
Purpose: verifies archive state transition and persistence update call.
Expected outcome: `archivedAt` is set on warehouse and updated entity.

### `com.fulfilment.application.monolith.warehouses.adapters.restapi.WarehouseResourceImplUnitTest`
1. `testListAllWarehousesUnitsShouldMapDomainWarehouses`
Purpose: verifies domain-to-API mapping in list endpoint.
Expected outcome: mapped API payload contains id/code/location/capacity/stock values.

2. `testCreateANewWarehouseUnitShouldCreateAndReturnMappedWarehouse`
Purpose: verifies create endpoint mapping and lookup of created warehouse.
Expected outcome: request maps to domain model, use case is invoked, and mapped response is returned.

3. `testCreateANewWarehouseUnitShouldMapValidationExceptionTo400`
Purpose: validates create endpoint validation error mapping.
Expected outcome: `WarehouseValidationException` maps to `400`.

4. `testCreateANewWarehouseUnitShouldMapConflictExceptionTo409`
Purpose: validates create endpoint conflict error mapping.
Expected outcome: `WarehouseConflictException` maps to `409`.

5. `testCreateANewWarehouseUnitShouldMapPersistenceDuplicateKeyTo409`
Purpose: validates duplicate-key persistence mapping.
Expected outcome: persistence duplicate/unique signals map to `409`.

6. `testCreateANewWarehouseUnitShouldMapPersistenceCheckConstraintTo400`
Purpose: validates check-constraint persistence mapping.
Expected outcome: capacity/stock check-constraint signals map to `400`.

7. `testCreateANewWarehouseUnitShouldMapUnknownPersistenceTo500`
Purpose: validates fallback persistence error mapping.
Expected outcome: unknown persistence failures map to `500`.

8. `testGetAWarehouseUnitByIDShouldRejectNonNumericId`
Purpose: validates id parsing guard.
Expected outcome: non-numeric id maps to `400`.

9. `testGetAWarehouseUnitByIDShouldReturnNotFoundWhenMissing`
Purpose: validates get-by-id not-found mapping.
Expected outcome: missing warehouse maps to `404`.

10. `testGetAWarehouseUnitByIDShouldReturnMappedResponse`
Purpose: verifies get-by-id happy path mapping.
Expected outcome: returned API payload matches domain warehouse.

11. `testArchiveAWarehouseUnitByIDShouldMapValidationExceptionTo400`
Purpose: validates archive validation error mapping.
Expected outcome: `WarehouseValidationException` maps to `400`.

12. `testArchiveAWarehouseUnitByIDShouldRejectNonNumericId`
Purpose: validates archive id parsing guard.
Expected outcome: non-numeric id maps to `400`.

13. `testArchiveAWarehouseUnitByIDShouldReturnNotFoundWhenWarehouseMissing`
Purpose: validates archive not-found mapping.
Expected outcome: missing warehouse maps to `404`.

14. `testArchiveAWarehouseUnitByIDShouldMapConflictExceptionTo409`
Purpose: validates archive conflict mapping.
Expected outcome: `WarehouseConflictException` maps to `409`.

15. `testArchiveAWarehouseUnitByIDShouldMapPersistenceException`
Purpose: validates archive persistence exception mapping.
Expected outcome: mapped HTTP status follows persistence details analysis.

16. `testReplaceTheCurrentActiveWarehouseShouldReplaceAndReturnMappedWarehouse`
Purpose: verifies replace endpoint happy path and business-unit override from path parameter.
Expected outcome: replacement use case receives path business unit and mapped response is returned.

17. `testReplaceTheCurrentActiveWarehouseShouldMapNotFoundTo404`
Purpose: validates replace not-found mapping.
Expected outcome: `WarehouseNotFoundException` maps to `404`.

18. `testReplaceTheCurrentActiveWarehouseShouldMapValidationAndConflict`
Purpose: validates replace validation and conflict mappings.
Expected outcome: validation maps to `400`; conflict maps to `409`.

19. `testReplaceTheCurrentActiveWarehouseShouldHandlePersistenceAndMissingCreatedWarehouse`
Purpose: validates replace persistence mapping and post-replace lookup failure branch.
Expected outcome: duplicate-key persistence maps to `409`; missing created warehouse maps to `500`.

## Runtime API / E2E Tests (`@QuarkusTest`, tag `e2e`)

### `com.fulfilment.application.monolith.products.ProductEndpointTest`
1. `testCrudProduct`
Purpose: basic product endpoint smoke test.
Expected outcome:
- initial list contains seeded products,
- delete `/product/1` returns `204`,
- subsequent list excludes deleted product.

### `com.fulfilment.application.monolith.stores.StoreOutboxTest`
1. `testStoreCreateShouldWriteOutboxAndPublishOnlyAfterRelayRuns`
Purpose: validates transactional outbox write and post-commit publication flow.
Expected outcome:
- create store returns `201`,
- pending outbox count increments,
- no legacy publication before relay,
- after relay, message is published and one legacy event processed.

2. `testStoreRollbackShouldNotPersistOutboxEvent`
Purpose: ensures rollback does not persist outbox events.
Expected outcome: failing store create (`500`) leaves outbox row count unchanged.

3. `testOutboxPublisherShouldRetryAfterLegacyFailure`
Purpose: validates retry behavior and idempotent publication.
Expected outcome:
- first publish attempt fails and increments attempts,
- retry publishes successfully,
- additional publish does not duplicate legacy side effect.

### `com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentTest`
1. `testMaxTwoWarehousesPerProductPerStoreConstraint`
Purpose: enforces max 2 warehouses per product per store.
Expected outcome: first 2 assignments return `200`, 3rd returns `409`.

2. `testMaxThreeWarehousesPerStoreConstraint`
Purpose: enforces max 3 warehouses per store.
Expected outcome: first 3 assignments return `200`, 4th returns `409`.

3. `testMaxFiveProductsPerWarehouseConstraint`
Purpose: enforces max 5 product types per warehouse.
Expected outcome: first 5 assignments return `200`, 6th returns `409`.

### `com.fulfilment.application.monolith.warehouses.adapters.restapi.WarehouseEndpointTest`
1. `testSimpleListWarehouses`
Purpose: list endpoint baseline check.
Expected outcome: `200` and seeded warehouse business-unit codes are present.

2. `testGetWarehouseByIdShouldSupportHappyAndInvalidPaths`
Purpose: validates get-by-id happy path and invalid/non-existent id handling.
Expected outcome:
- created warehouse fetch returns `200`,
- non-numeric id returns `400`,
- unknown numeric id returns `404`.

3. `testCreateWarehouseAndArchiveWarehouse`
Purpose: end-to-end create + archive lifecycle.
Expected outcome:
- create returns `200`,
- archive returns `204`,
- archived id returns `404`,
- list excludes archived business unit.

4. `testCreateWarehouseShouldFailWhenBusinessUnitCodeAlreadyExists`
Purpose: duplicate business unit validation.
Expected outcome: create returns `409`.

5. `testCreateWarehouseShouldFailWhenLocationIsInvalid`
Purpose: invalid location validation.
Expected outcome: create returns `400`.

6. `testCreateWarehouseShouldFailWhenCapacityLowerThanStock`
Purpose: `capacity >= stock` validation.
Expected outcome: create returns `400`.

7. `testCreateWarehouseShouldFailWhenLocationCapacityExceeded`
Purpose: location max-capacity validation.
Expected outcome: create returns `400`.

8. `testCreateWarehouseShouldFailWhenLocationMaxWarehousesReached`
Purpose: max-warehouse-count validation.
Expected outcome: first create `200`, second create at same constrained location `409`.

9. `testReplaceWarehouseSuccessAndValidationFailure`
Purpose: replacement validation + successful replacement path.
Expected outcome:
- invalid stock replacement returns `400`,
- valid replacement returns `200` with new location,
- exactly one active warehouse remains for business unit.

10. `testReplaceWarehouseShouldReturnNotFoundWhenMissingBusinessUnitCode`
Purpose: replacement not-found handling.
Expected outcome: returns `404`.

11. `testArchiveWarehouseShouldValidateIdInputAndNotFound`
Purpose: archive id input validation and not-found handling.
Expected outcome: non-numeric id `400`; unknown id `404`.

12. `testConcurrentCreateWithSameBusinessUnitCodeShouldAllowOnlyOneActiveWarehouse`
Purpose: concurrency safety for duplicate business-unit create.
Expected outcome:
- concurrent statuses are only `200`/`409`,
- at most one `200`,
- repository confirms max one active record for that business unit.

13. `testConcurrentCreateAtLocationLimitShouldNotExceedConfiguredMax`
Purpose: concurrency safety for location max-warehouse limit.
Expected outcome:
- concurrent statuses are only `200`/`409`,
- at most one `200` for constrained location,
- repository confirms active count at location does not exceed configured max.

## Optional Packaged Runtime Tests (`@QuarkusIntegrationTest`)

### `com.fulfilment.application.monolith.warehouses.adapters.restapi.WarehouseEndpointIT`
Execution condition:
- Enabled only when `-Drun.packaged.it=true` is provided.
- Tagged `packaged-it`.

1. `testSimpleListWarehouses`
Purpose: packaged-runtime smoke test for warehouse list endpoint.
Expected outcome: `200` with seeded warehouse business-unit codes present.

2. `testSimpleCheckingArchivingWarehouses`
Purpose: packaged-runtime smoke test for archive lifecycle.
Expected outcome:
- initial list contains seeded warehouse/location values,
- archive `DELETE /warehouse/1` returns `204`,
- subsequent list excludes archived location (`ZWOLLE-001`).

## Current Count Summary
- Test classes with `@Test` methods: 26.
- Test methods total: 168.
- Optional/gated methods by default: 22 (`WarehouseEndpointIT` + `e2e`-tagged runtime tests).
- Methods typically executed by default Maven `./mvnw test`: 146.

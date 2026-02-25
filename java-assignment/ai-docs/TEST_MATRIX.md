# Test Matrix

## Task 1

1. `LocationGatewayTest`
   1. Existing identifier resolves.
   2. Unknown identifier returns null.
   3. Null/blank handling.
   4. Trim + case-insensitive handling.

## Task 2

1. Unit tests (store package, non-entity focus)
   1. `LegacyStoreManagerGatewayTest`
      1. Success, idempotency, failure-mode, and state-reset behavior.
   2. `StoreResourceErrorMapperTest`
      1. `WebApplicationException` mapping, generic `500` mapping, null-message payload behavior.
   3. `StoreResourceValidationTest`
      1. Create/update validation guards and create happy-path outbox dispatch using test doubles.
   4. `StoreResourceUnitTest`
      1. Full resource behavior coverage through `StoreGateway` test doubles (`GET`, `GET/{id}`, `POST`, `PUT`, `PATCH`, `DELETE`).
   5. `OutboxPublisherMetricsTest`
      1. Relay counters, average latency computation, and reset behavior.
   6. `StoreOutboxServiceTest`
      1. Outbox message creation metadata/payload mapping and serialization failure behavior.
   7. `OutboxPublisherTest`
      1. Publish success/failure branches, empty-batch no-op, scheduler lifecycle branches.
   8. `OutboxAdminResourceTest`
      1. Stats/publish/replay validation and successful replay behavior.
   9. `OutboxMessageRepositoryTest`
      1. Pending query paging, mark published/failed semantics, replay, maintenance operations.
2. Unit tests (product package)
   1. `ProductResourceUnitTest`
      1. CRUD guardrails and happy-path resource behavior without runtime DB.
   2. `ProductResourceErrorMapperTest`
      1. `WebApplicationException`/generic/null-message error mapper behavior.
3. Unit tests (fulfillment package)
   1. `FulfillmentAssignmentRepositoryTest`
      1. Count-query parameter and aggregate result behavior.
   2. `FulfillmentAssignmentServiceTest`
      1. Input validation, missing dependency entities, rule limits, idempotency, and happy path.
   3. `FulfillmentAssignmentResourceTest`
      1. API mapping of success + `400`/`409` error translation.
4. `StoreOutboxTest` (implemented, Quarkus runtime required)
   1. Commit writes outbox row and relay publishes.
   2. Rollback path does not persist outbox row.
   3. Retry path keeps pending then publishes successfully.
   4. Duplicate relay invocation does not duplicate legacy side-effect.
5. UAT script:
   1. `uat/store_task2_uat.sh`

## Task 3

1. Unit tests
   1. `DbWarehouseTest`
      1. Bidirectional domain/entity mapping for all fields.
   2. `WarehouseRepositoryTest`
      1. Repository query/update/aggregate/lock helper behavior and mapping coverage.
   3. `CreateWarehouseUseCaseTest`
   4. `ReplaceWarehouseUseCaseTest`
   5. `ArchiveWarehouseUseCaseTest`
   6. `WarehouseResourceImplUnitTest`
      1. REST adapter mapping + validation + persistence-error translation branches.
2. Endpoint coverage
   1. `WarehouseEndpointTest` (implemented, Quarkus runtime required)
   2. `WarehouseEndpointIT` baseline integration class remains.
   3. Includes runtime coverage for:
      1. list/get/create/replace/archive happy paths
      2. invalid id and not-found handling
      3. location, capacity and stock validation failures
      4. duplicate business unit conflict
      5. concurrent duplicate create and concurrent location-limit create scenarios
3. UAT script:
   1. `uat/warehouse_uat.sh`

## Bonus

1. `FulfillmentAssignmentTest` (implemented, Quarkus runtime required)
   1. Max 2 warehouses per product per store.
   2. Max 3 warehouses per store.
   3. Max 5 product types per warehouse.

## Execution Notes

1. Pure unit tests pass with:
   1. `./mvnw -Dtest=LocationGatewayTest,ProductResourceUnitTest,ProductResourceErrorMapperTest,FulfillmentAssignmentRepositoryTest,FulfillmentAssignmentServiceTest,FulfillmentAssignmentResourceTest,LegacyStoreManagerGatewayTest,StoreResourceErrorMapperTest,StoreResourceValidationTest,StoreResourceUnitTest,OutboxPublisherMetricsTest,StoreOutboxServiceTest,OutboxPublisherTest,OutboxAdminResourceTest,OutboxMessageRepositoryTest,DbWarehouseTest,WarehouseRepositoryTest,WarehouseResourceImplUnitTest,CreateWarehouseUseCaseTest,ReplaceWarehouseUseCaseTest,ArchiveWarehouseUseCaseTest test`
2. Quarkus integration-style tests require:
   1. Docker/Testcontainers or a configured reachable PostgreSQL for test profile.
3. Runtime E2E suite executed successfully on February 25, 2026 with:
   1. `./mvnw -Dtest=ProductEndpointTest,StoreOutboxTest,WarehouseEndpointTest,FulfillmentAssignmentTest -Dtest.excluded.groups= -De2e.testcontainers.enabled=true -De2e.testcontainers.required=true test`
   2. Result: `BUILD SUCCESS` (20 tests, 0 failures).
4. Rollback-safe mode:
   1. Keep default build command `./mvnw test` (e2e excluded by default).
   2. Use non-strict fallback `-De2e.testcontainers.required=false` with external datasource `test.db.url`, `test.db.username`, `test.db.password`.
5. Task 3 runtime verification command:
   1. `./mvnw -Dtest=WarehouseEndpointTest -Dtest.excluded.groups= -De2e.testcontainers.enabled=true -De2e.testcontainers.required=true test`
   2. Last execution result on February 25, 2026: `BUILD SUCCESS` (13 tests, 0 failures).

# Codex Final Implementation Plan

## Scope
This plan combines:
1. Assignment tasks from `CODE_ASSIGNMENT.md` (Task 1, Task 2, Task 3, Bonus).
2. Additional reliability/migration stories from `additional_user_stories.md`.
3. Ordered execution guidance from `phased_implementation_plan.md`.
4. Current repository architecture constraints (mixed patterns, weak Task 2 transactional guarantees).

## Phase Status Legend
1. `Todo[✓] Started[ ] Completed [ ]` = planned, not started
2. `Todo[ ] Started[✓] Completed [ ]` = in progress
3. `Todo[ ] Started[ ] Completed [✓]` = done

## Phase 1 - Baseline and Task 1 Closure
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Confirm environment and build entry points (`mvnw`, Quarkus, DB config).
2. Implement `LocationGateway.resolveByIdentifier`.
3. Add/complete unit tests for location resolution behavior.
4. Run targeted tests and confirm Task 1 is complete.
5. Record Task 1 completion in plan artifacts.

## Phase 2 - Task 2 Design Lock (Outbox-First)
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Freeze architecture decision for Task 2: Transactional Outbox instead of direct legacy call inside REST transaction.
2. Define Store event contract:
   1. Event types: `StoreCreated`, `StoreUpdated`, `StorePatched`, `StoreDeleted`.
   2. Envelope fields: `eventId`, `aggregateId`, `aggregateVersion or txId`, `occurredAt`, `correlationId`.
   3. Payload: committed Store snapshot.
3. Define outbox persistence model:
   1. Fields: `id`, `aggregateType`, `aggregateId`, `eventType`, `payload`, `attempts`, `publishedAt`, `lastError`, `createdAt`.
   2. Indexes for relay scan and idempotency.
4. Define delivery semantics:
   1. At-least-once publish from relay.
   2. Idempotency key propagation to legacy.
5. Finalize acceptance mapping to US-01/US-02/US-03 criteria before coding.

## Phase 3 - Task 2 Implementation (Store + Outbox + Relay)
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add `OutboxMessage` entity and repository.
2. Refactor `StoreResource`:
   1. Keep DB writes in `@Transactional` methods.
   2. Remove direct `LegacyStoreManagerGateway` calls from request path.
   3. Write outbox record in the same transaction as Store state change.
3. Implement relay publisher:
   1. Scheduled polling of pending outbox rows.
   2. Publish to legacy gateway with idempotency key.
   3. Mark published on success.
   4. Increment attempts and capture `lastError` on failure.
4. Fix gateway error handling:
   1. Stop swallowing exceptions silently.
   2. Return explicit success/failure to relay.
5. Align payload source to persisted entity state (not raw request object).

## Phase 4 - Task 2 Verification, UAT, and Operability
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add automated tests for post-commit guarantee:
   1. Rollback path emits no committed outbox event.
   2. Commit path emits one event and relay publishes once.
   3. Retry path retains pending event until successful publish.
2. Add duplicate-delivery/idempotency test.
3. Add endpoint or query for outbox operational visibility.
4. Add UAT scripts:
   1. Create/update/delete happy flows.
   2. Forced failure and replay scenarios.
5. Add runbook notes for retry, dead-letter handling, and re-drive.

## Phase 5 - Task 3 Warehouse Core Implementation
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Implement missing `WarehouseResourceImpl` handlers.
2. Implement repository operations currently throwing `UnsupportedOperationException`.
3. Implement use cases for create/replace/archive with assignment validations:
   1. Business unit uniqueness.
   2. Location validity via `LocationGateway`.
   3. Max warehouses per location.
   4. Capacity and stock constraints.
   5. Replace-specific capacity accommodation and stock matching.
4. Ensure clear API error mapping (`400/404/409`) by validation category.
5. Add unit tests for each validation and integration tests for endpoint behavior.

## Phase 6 - Bonus Constraints (Saga-Ready in Monolith)
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Introduce fulfillment association model for Store-Product-Warehouse.
2. Enforce constraints in one transactional application service:
   1. Max 2 warehouses per product per store.
   2. Max 3 warehouses per store.
   3. Max 5 product types per warehouse.
3. Add DB constraints/indexes to reduce race-condition risk.
4. Add tests for concurrency and deterministic rejection at constraint boundaries.

## Phase 7 - Microservice-Transition Readiness (Design + Minimal Hooks)
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add correlation ID propagation for request and outbox events.
2. Add outbox metrics: pending count, publish latency, failure rate, retry count.
3. Define replay controls by `aggregateId` and date range.
4. Version event schema and document compatibility rules.
5. Define initial strangler boundaries:
   1. Store Sync adapter as first extraction candidate.
   2. Warehouse bounded context as second extraction candidate.

## Phase 8 - Documentation and Delivery Pack
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Update README with local run/test steps and DB setup.
2. Document architecture decision record for Task 2 outbox approach.
3. Add a test matrix mapping each acceptance criterion to automated test/UAT coverage.
4. Add release checklist for cutover readiness.

## Phase 9 - Runtime E2E Enablement (Testcontainers)
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add reusable Quarkus test resource to bootstrap shared PostgreSQL Testcontainer for `@QuarkusTest` classes.
2. Tag runtime tests with `@Tag("e2e")` and keep them excluded by default for stable local/CI builds.
3. Add strict mode for container startup:
   1. `-De2e.testcontainers.enabled=true`
   2. `-De2e.testcontainers.required=true`
4. Add rollback/safe mode:
   1. Default build keeps `e2e` group excluded.
   2. Optional external datasource fallback when strict mode is not enabled.
5. Execute full runtime E2E suite with Docker/Testcontainers and close deterministic retry-test timing gap.

## Ordered Execution Sequence
1. Execute Phase 2.
2. Execute Phase 3.
3. Execute Phase 4.
4. Execute Phase 5.
5. Execute Phase 6 (optional but recommended for bonus scope).
6. Execute Phase 7.
7. Execute Phase 8.

## Delivery Notes
1. Implemented features, tests, and docs are committed in code under:
   1. `stores/outbox` for Task 2 reliability.
   2. `warehouses` adapters/usecases/repository for Task 3 completion.
   3. `fulfillment` package for bonus constraints.
   4. `ai-docs` and `uat` folders for ops/docs artifacts.
2. Full Quarkus runtime tests require Docker/Testcontainers or configured reachable PostgreSQL test datasource.
3. Runtime E2E verification command (executed successfully on February 24, 2026):
   1. `./mvnw -Dtest=ProductEndpointTest,StoreOutboxTest,WarehouseEndpointTest,FulfillmentAssignmentTest -Dtest.excluded.groups= -De2e.testcontainers.enabled=true -De2e.testcontainers.required=true test`
4. Rollback/safe execution options:
   1. Default: `./mvnw test` (keeps `e2e` tests excluded).
   2. External DB fallback: `-De2e.testcontainers.required=false` plus `test.db.*` properties.

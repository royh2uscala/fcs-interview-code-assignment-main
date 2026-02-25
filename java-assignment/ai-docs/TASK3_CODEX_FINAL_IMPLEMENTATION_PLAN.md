# Task 3 Codex Final Implementation Plan

## Scope
This plan is based on:
1. Task 3 requirements in `CODE_ASSIGNMENT.md`.
2. Updated user stories and acceptance criteria in `TASK3_USER_STORIES_AND_ACCEPTANCE_CRITERIA_updated_V02.md`.
3. Ordered guidance in `TASK3_phased_implementation_plan_V02.md`.

## Phase Status Legend
1. `Todo[✓] Started[ ] Completed [ ]` = planned, not started
2. `Todo[ ] Started[✓] Completed [ ]` = in progress
3. `Todo[ ] Started[ ] Completed [✓]` = complete

## Phase 1 - Requirements Lock and Traceability
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Confirm Task 3 functional scope: list, get by id, create, replace, archive.
2. Lock validation scope: uniqueness, valid location, location max warehouses, capacity/stock, replacement capacity/stock matching.
3. Lock response semantics for API errors:
4. `400` for invalid input and invalid location/business request format.
5. `404` for missing warehouse entities.
6. `409` for business rule conflicts (duplicate code, location count limit).
7. Produce a traceability matrix mapping T3-US-01 to T3-US-12 to endpoint/use case/test.

## Phase 2 - API Contract and Error Model Alignment
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Verify OpenAPI and generated interface for Warehouse endpoints.
2. Align handler behavior with accepted status codes and payload shape.
3. Standardize API error responses for 400/404/409 with deterministic messages.
4. Confirm package and endpoint conventions in docs to avoid `warehouse` vs `warehouses` ambiguity.

## Phase 3 - Persistence and Data Integrity Foundations
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Validate warehouse persistence fields required for Task 3 operations.
2. Ensure archive is implemented as a soft-delete state transition.
3. Enforce integrity constraints:
4. Non-negative stock and capacity.
5. Capacity must be greater than or equal to stock.
6. Uniqueness strategy for active warehouse business unit code.
7. Add/confirm indexes for active-by-location and active-by-business-unit queries.

## Phase 4 - Read and Archive Use Cases
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Implement list active warehouses use case and endpoint wiring.
2. Implement get warehouse by id use case with active-only semantics.
3. Implement archive warehouse by id use case and endpoint wiring.
4. Ensure archived warehouses are excluded from list and treated as not found on get.
5. Ensure id parsing failures return `400`.

## Phase 5 - Create Warehouse Use Case and Validations
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Implement create request payload validation.
2. Implement location existence validation via `LocationGateway`.
3. Implement active business unit code conflict check.
4. Implement location max warehouses feasibility check.
5. Implement capacity validation against stock.
6. Implement capacity validation against location capacity limits.
7. Persist created warehouse as active record and return created response.
8. Map validation and conflict outcomes to consistent `400` and `409` responses.

## Phase 6 - Replace Warehouse Use Case and Transactional Workflow
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Implement replacement lookup by active business unit code.
2. Return `404` when no active warehouse exists for replacement target.
3. Validate replacement payload fields and non-negative values.
4. Validate stock matching between current and replacement warehouse.
5. Validate replacement capacity accommodates stock.
6. Validate replacement location exists.
7. Re-validate location count and capacity constraints for replacement target.
8. Execute replacement atomically in one transaction:
9. Archive current active warehouse.
10. Create replacement warehouse as active.
11. Guarantee exactly one active warehouse for the business unit code after commit.
12. Map validation and conflict outcomes to `400` and `409`.

## Phase 7 - API Handler Completion and Integration
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Complete all warehouse endpoint handlers and remove unsupported/incomplete branches.
2. Ensure request-to-domain mapping and response mapping are consistent across operations.
3. Ensure exception translation is deterministic and test-friendly.
4. Verify read-after-write behavior for create, replace, and archive flows.

## Phase 8 - Unit Test Coverage for Domain Rules
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add or refine unit tests for create use case rules.
2. Add or refine unit tests for replace use case rules.
3. Add or refine unit tests for archive use case behavior.
4. Add unit tests for rule edge-cases:
5. Duplicate business unit code.
6. Invalid location.
7. Max warehouses reached at location.
8. Capacity and stock violations.
9. Replacement stock mismatch and insufficient capacity.

## Phase 9 - Quarkus Runtime Integration and End-to-End Tests
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Ensure runtime endpoint tests cover all Task 3 user stories.
2. Validate end-to-end status codes and response payloads for success and failure cases.
3. Execute tests with Testcontainers-backed PostgreSQL where available.
4. Keep fallback/safe execution mode for environments without Docker.
5. Verify archive and replacement outcomes via endpoint reads, not only internal repository state.

## Phase 10 - Concurrency and Runtime Risk Tests
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Add targeted concurrency tests for duplicate business unit creation attempts.
2. Add targeted concurrency tests for location max warehouse limit races.
3. Validate that constraints prevent invalid final states under concurrent requests.
4. Capture and document any remaining race-risk assumptions.

## Phase 11 - UAT and Operational Validation
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Align `uat/warehouse_uat.sh` scenarios to T3-US-01 to T3-US-12.
2. Include positive and negative API examples for all required validations.
3. Document required runtime prerequisites for local UAT execution.
4. Add expected output/status reference for quick regression checks.

## Phase 12 - Documentation and Sign-off
Status: `Todo[ ] Started[ ] Completed [✓]`

1. Update Task 3 docs with final endpoint behavior and status code conventions.
2. Publish acceptance-to-test mapping for each user story.
3. Record final test commands and latest execution outcomes.
4. Mark each phase status based on implementation evidence.

## Recommended Execution Order
1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4
5. Phase 5
6. Phase 6
7. Phase 7
8. Phase 8
9. Phase 9
10. Phase 10
11. Phase 11
12. Phase 12

## Latest Execution Evidence
1. Date: February 25, 2026.
2. Unit/domain tests:
   1. `./mvnw test`
   2. Result: `BUILD SUCCESS` (22 tests, 0 failures).
3. Task 3 runtime E2E with Testcontainers:
   1. `./mvnw -Dtest=WarehouseEndpointTest -Dtest.excluded.groups= -De2e.testcontainers.enabled=true -De2e.testcontainers.required=true test`
   2. Result: `BUILD SUCCESS` (13 tests, 0 failures).
4. Cross-module runtime suite with Testcontainers:
   1. `./mvnw -Dtest=ProductEndpointTest,StoreOutboxTest,WarehouseEndpointTest,FulfillmentAssignmentTest -Dtest.excluded.groups= -De2e.testcontainers.enabled=true -De2e.testcontainers.required=true test`
   2. Result: `BUILD SUCCESS` (20 tests, 0 failures).
5. Rollback-safe option remains available:
   1. keep default `./mvnw test` where `e2e` group is excluded.
   2. run runtime tests with `-De2e.testcontainers.required=false` and `test.db.*` overrides when Docker is unavailable.

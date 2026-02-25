# Task 3 — Ordered, Phased Implementation Plan (guided by V02 stories)

This plan is designed to be **copied into Codex prompts** and executed in order. It aligns with:
- Task 3 requirements in `CODE_ASSIGNMENT.md` fileciteturn0file1  
- Updated stories/criteria in `TASK3_USER_STORIES_AND_ACCEPTANCE_CRITERIA_updated_V02.md` fileciteturn4file0  
- Current stack + run/test workflow in `README.md` (Quarkus + JDK17 + Postgres/Docker) fileciteturn4file1  

> **Best-fit strategy for the assignment:** implement Task 3 in the **modular monolith** first (single deployable, single DB), using clean boundaries (ports/adapters) and DB constraints for correctness. Treat “microservice extraction” as an optional Phase 8 design step.

---

## Phase 0 — Baseline & Traceability (½ day)

### 0.1 Build a Task 3 “implementation checklist”
**Objective:** identify every incomplete handler/use case and map it to a V02 story.

**Codex prompt**
- “Scan the codebase for Warehouse API handlers (resources/controllers) and use cases. List every TODO/UnsupportedOperationException. For each method, map it to a user story ID from V02 (T3-US-01…12) and to the relevant Task 3 bullet in CODE_ASSIGNMENT.md.”

**Deliverables**
- `docs/task3-checklist.md` listing:
  - Endpoint → handler method → use case → repository/gateway dependencies → tests needed
  - Status code convention to use (400/404/409) per V02

### 0.2 Confirm tech baseline locally
Use the README flow:
- Build: `./mvnw package`
- Dev run: `./mvnw quarkus:dev`
- DB: Postgres or Docker fileciteturn4file1

**Codex prompt**
- “Add a minimal `DEV.md` with exact steps to run Task 3 locally and run tests.”

---

## Phase 1 — API Contract & DTO/Mapping Verification (½ day)

### 1.1 Confirm endpoint paths and payloads
**Objective:** avoid mismatches between assignment `/warehouse` wording and actual code paths.

**Codex prompt**
- “Locate the OpenAPI/JAX-RS interface for Warehouse endpoints. Confirm exact paths and request/response DTOs for:
  - GET list
  - GET by id
  - POST create
  - POST replacement by businessUnitCode
  - DELETE archive by id
  Output a summary table.”

**Deliverables**
- `docs/task3-api-surface.md` with:
  - Method + path + request/response types + expected statuses (per V02)

### 1.2 Define error response shape (consistent)
**Objective:** deterministic error handling for UAT & tests.

**Recommendation**
- Standardize an error envelope:
  - `code` (e.g., `WAREHOUSE_NOT_FOUND`, `BUSINESS_UNIT_CONFLICT`, `LOCATION_INVALID`, `CAPACITY_INVALID`)
  - `message`
  - `details` (optional)

**Codex prompt**
- “Implement/standardize an exception mapper (JAX-RS) returning {code,message,details}. Ensure 400/404/409 are returned consistently.”

---

## Phase 2 — Persistence Model + DB Constraints (½–1 day)

### 2.1 Warehouse persistence model review
**Objective:** ensure required fields exist and support “archive” as soft delete.

**Codex prompt**
- “Review Warehouse entity/table schema. Ensure it supports:
  - businessUnitCode
  - locationIdentifier (or FK)
  - capacity, stock
  - archived flag/timestamp
  - createdAt/updatedAt (optional)
  Propose migrations if needed.”

### 2.2 Add DB-level constraints to support correctness under concurrency
This makes acceptance criteria like uniqueness and max-limits safer.

**Recommended DB constraints**
- Unique index on `(business_unit_code)` **for active warehouses only** if DB supports partial indexes (Postgres does):
  - `UNIQUE (business_unit_code) WHERE archived = false`
- Check constraints:
  - `capacity >= 0`, `stock >= 0`, `capacity >= stock`
- Optional: index on `(location_identifier, archived)` for counting active warehouses quickly

**Codex prompt**
- “Add flyway/liquibase or SQL init scripts for: unique active business unit code, check constraints, helpful indexes. Update tests accordingly.”

---

## Phase 3 — Core Use Cases (Application Layer) (1 day)

> Implement in the order that minimizes dependency surprises.

### 3.1 T3-US-01: List active warehouses
- Query only active (archived=false)
- Return `200`

**Codex prompt**
- “Implement ListActiveWarehouses use case + repository query. Add integration test: archived warehouses are excluded.”

### 3.2 T3-US-02: Get warehouse by ID
- Parse path param:
  - non-numeric → `400`
- Active exists → `200`
- Not found or archived → `404`

**Codex prompt**
- “Implement GetWarehouseById use case with ‘active only’ semantics. Add tests for 200/404/400.”

### 3.3 T3-US-12: Archive warehouse by ID
- Active exists → set archived flag/timestamp; return `204`
- Not found → `404`
- Non-numeric → `400`

**Codex prompt**
- “Implement ArchiveWarehouse use case (soft delete). Add tests for 204/404/400 and confirm archived entity disappears from list and get-by-id returns 404.”

---

## Phase 4 — Create Warehouse + Validations (1–1.5 days)

### 4.1 Implement CreateWarehouse with validations
Align to V02: uniqueness, location, max warehouses per location, capacity/stock rules. fileciteturn4file0

**Validation ordering (recommended)**
1) Payload shape & numeric checks → `400`
2) Location validity (via LocationGateway) → `400` (or `404`, pick one)
3) Business constraints:
   - businessUnitCode conflict → `409`
   - max warehouses at location reached → `409`
4) Capacity rules:
   - capacity > location.maxCapacity → `400`
   - capacity < stock → `400`

**Codex prompt**
- “Implement CreateWarehouse:
  - Validate request (non-null fields, non-negative numbers, capacity >= stock).
  - Validate location exists via LocationGateway.
  - Enforce max warehouses per location (count active warehouses at location).
  - Enforce capacity <= location.maxCapacity.
  - Enforce unique businessUnitCode (prefer DB unique constraint + translate to 409).
  Return 201 (or 200 if contract dictates) with created entity.”

### 4.2 Tests for CreateWarehouse
**Integration tests (must-have)**
- Happy path create → appears in list
- Duplicate businessUnitCode → 409
- Invalid location → 400/404 (whichever you standardize)
- Max warehouses per location reached → 409
- capacity < stock → 400
- capacity > location.maxCapacity → 400

**Codex prompt**
- “Add integration tests for each CreateWarehouse acceptance criterion. Include test data builders/fixtures to reduce boilerplate.”

---

## Phase 5 — Replace Warehouse Workflow (1–2 days)

Replacement is the trickiest because it modifies multiple rows and must remain consistent. V02 clarifies: *exactly one active warehouse for a businessUnitCode after replacement* and the old one is archived. fileciteturn4file0

### 5.1 Implement ReplaceWarehouse (transactional)
**Recommended pattern:** **Transactional workflow** (single local transaction, since monolith)
- Fetch active warehouse by businessUnitCode → else `404`
- Validate payload numeric checks → `400`
- Validate replacement rules:
  - payload stock == current stock → `400` if mismatch
  - new capacity >= current stock → `400` if not
  - new capacity >= payload stock → `400` if not
- Validate replacement location exists → 400/404
- Validate location constraints:
  - max warehouses at target location → consider that the old warehouse will be archived, and the new one created.
  - If replacement stays in same location, net active count may stay constant.
- Persist:
  - archive old
  - create new active warehouse record (or update-in-place depending on code style, but acceptance criteria prefers “archive + create”)
- Return `200` with the active replacement

**Codex prompt**
- “Implement ReplaceWarehouse in a single @Transactional method:
  - get active by businessUnitCode
  - validate stock matching, capacity rules, location validity, max warehouses per location
  - archive existing, create replacement
  - guarantee exactly one active for that businessUnitCode
  Add tests for: 200 success, 404 missing businessUnitCode, 400 stock mismatch, 400 capacity < stock, 409 max warehouses, 400 invalid location.”

### 5.2 Concurrency safety
**Objective:** prevent two replacements creating two actives.
- Use DB uniqueness on active businessUnitCode (partial unique index)
- Optionally lock the current warehouse row (`SELECT … FOR UPDATE`) in repository layer

**Codex prompt**
- “Add repository method to fetch-by-businessUnitCode with pessimistic lock during replacement. Ensure unique active index exists to guard concurrency.”

---

## Phase 6 — Endpoint Wiring + Error Mapping (½ day)

### 6.1 Wire resource handlers to use cases
**Objective:** remove any remaining placeholder endpoints and ensure status codes match V02.

**Codex prompt**
- “Update WarehouseResource implementation to call the new use cases. Ensure:
  - 200/201/204/400/404/409 statuses match the V02 criteria
  - error envelope is consistent
  - archived behavior is enforced.”

### 6.2 Add OpenAPI examples (optional but helpful)
- request/response examples for create/replace
- error examples for 400/404/409

---

## Phase 7 — User Acceptance Test Plan + Scripts (½–1 day)

README already suggests UAT scripts under `uat/` (if present) and Docker/DB options. fileciteturn4file1

### 7.1 UAT scenarios aligned to V02 stories
Create a `uat/warehouse_uat.sh` (or Postman collection) covering:

**UAT-01 List**
- Create 1 active + 1 archived, verify list shows only active.

**UAT-02 Get**
- Get active by id → 200
- Get archived by id → 404
- Get invalid id “abc” → 400

**UAT-03 Create validations**
- Duplicate businessUnitCode → 409
- Invalid location → 400/404
- Exceed max warehouses per location → 409
- capacity < stock → 400
- capacity > location.maxCapacity → 400

**UAT-04 Replacement**
- Replace existing businessUnitCode → 200
- Verify old archived and new active
- Stock mismatch → 400
- New capacity too low → 400
- businessUnitCode missing → 404

**UAT-05 Archive**
- Archive by id → 204
- Confirm list excludes, get-by-id returns 404

**Codex prompt**
- “Create `uat/warehouse_uat.sh` with curl commands + expected status codes. Add a short `uat/README.md` describing prerequisites and how to run.”

### 7.2 Automated test suite alignment
Ensure each V02 acceptance criterion has a corresponding test.
- `WarehouseResourceIT` (integration tests)
- `WarehouseValidationTest` (unit tests)

---

## Phase 8 — Containerisation (Docker) for Repeatable Runs (½ day)

The repo supports Postgres via Docker per README. fileciteturn4file1  
Add compose to reduce setup friction.

### 8.1 docker-compose for Postgres
**Deliverables**
- `docker-compose.yml` at repo root
- uses `postgres:13.3` with the env vars from README example
- maps port `15432:5432`
- persistent volume for data

**Codex prompt**
- “Add docker-compose.yml for Postgres 13.3 mirroring README credentials, plus a make-like script in `scripts/` to start/stop DB.”

### 8.2 Optional: Containerize the Quarkus app (not required for assignment)
- Dockerfile (multi-stage): Maven build → runtime
- Environment variables for DB URL
- Compose profile to run app + DB together

---

## Phase 9 — Optional Microservice Extraction Plan (design step, no code unless requested)

Not required to satisfy Task 3, but useful if you want to show microservice readiness.

### 9.1 Candidate: Warehouse Service extraction
- Keep API contract stable
- Move Warehouse module into its own Quarkus service
- Owns its DB schema

### 9.2 Internal communication (only if you split)
- If Store/Product must react to warehouse changes, publish events:
  - `WarehouseCreated`, `WarehouseReplaced`, `WarehouseArchived`
- Use **Transactional Outbox** in Warehouse service so events are published only after commit (same concept as Task 2). fileciteturn4file1

### 9.3 Saga (only if you split and replacement becomes cross-service)
Replacement today is a local transaction. If later replacement touches other services (inventory, shipments):
- Use **Saga** to coordinate:
  - Step 1: reserve new capacity
  - Step 2: migrate inventory
  - Step 3: archive old
  - Compensations on failure

---

## Recommended design patterns to enhance Task 3 solution

1) **Hexagonal Architecture / Ports & Adapters**
- Keep use cases pure; repositories/gateways behind interfaces.
- Warehouse validations reside in application/domain layer.

2) **Transactional boundary**
- Replacement and archive operations should be `@Transactional`.

3) **DB constraints + exception mapping**
- Enforce uniqueness and basic invariants in DB.
- Translate constraint violations into `409` reliably.

4) **(Optional) Domain events**
- Publish `WarehouseArchived/Created/Replaced` as in-process events now; later becomes outbox/broker.

---

## Copy/paste execution checklist for Codex (ordered)

1. Enumerate incomplete Warehouse handlers/use cases; map to V02 stories. fileciteturn4file0  
2. Confirm endpoint paths/DTOs; define error envelope + status conventions.  
3. Ensure entity supports archived flag/timestamp; add DB constraints/indexes.  
4. Implement List active (T3-US-01) + tests.  
5. Implement Get by ID (T3-US-02) + tests.  
6. Implement Archive by ID (T3-US-12) + tests.  
7. Implement Create warehouse validations (T3-US-03..07) + integration tests for each rule.  
8. Implement Replace warehouse workflow (T3-US-08..11) with transactional archive+create; add lock/uniqueness; tests.  
9. Wire endpoints → use cases; ensure correct 200/201/204/400/404/409.  
10. Add UAT script(s) covering all V02 acceptance criteria.  
11. Add docker-compose for Postgres and document run steps. fileciteturn4file1  
12. (Optional) Draft microservice extraction + outbox event publishing plan.


# Ordered, Phased Implementation Plan (to guide Codex)

This plan targets the **best-fit solution for completing the current code assignment** while laying groundwork for a safe monolith → microservices transition later.

Key requirements from the assignment:
- **Task 1:** Implement `LocationGateway.resolveByIdentifier`. fileciteturn2file4  
- **Task 2:** Ensure `LegacyStoreManagerGateway` is invoked **only after DB commit** for Store changes. fileciteturn2file4  
- **Task 3:** Implement Warehouse endpoints + use cases with validations, plus replacement/archive rules. fileciteturn2file0  
- **Bonus:** Store–Product–Warehouse fulfillment constraints. fileciteturn2file0  

Current stack assumptions:
- Quarkus app, JDK 17+, PostgreSQL (local or Docker). fileciteturn2file3  

---

## Phase 0 — Repo Baseline & Guardrails (½ day)

### 0.1 Create a “working agreement” for Codex prompts
**Goal:** Keep implementation consistent and avoid architecture thrash.
- Preferred style: treat this as a **modular monolith** with clear package boundaries.
- For Task 2 reliability: implement **Transactional Outbox** (recommended), not “fire HTTP after commit in the same thread.”

**Codex prompt**
- “Scan CODE_ASSIGNMENT.md tasks, list exact TODO/UnsupportedOperationException points and affected classes/packages. Create a checklist and reference each missing method by fully-qualified class name.” fileciteturn2file4  

### 0.2 Set up local runtime + DB
**Goal:** Make it easy to run tests and endpoints.
- Follow README build/run steps:
  - `./mvnw package`
  - `./mvnw quarkus:dev`
  - PostgreSQL via Docker if needed. fileciteturn2file3

**Deliverables**
- `docker-compose.yml` for Postgres (preferred over manual `docker run`).
- `.env` (optional) with DB settings to reduce friction.

**Codex prompt**
- “Add docker-compose.yml for Postgres 13.3 matching README env vars and ports; update application.properties to support dev/test overrides cleanly.”

---

## Phase 1 — Task 1: Location Warm-up + Tests (½ day)

### 1.1 Implement `LocationGateway.resolveByIdentifier`
**Goal:** unblock Warehouse validation (“Location Validation”). fileciteturn2file0
- Implement resolution rules (as defined in domain/briefing): handle valid identifier formats, return empty/exception appropriately.

**Codex prompt**
- “Open `com.fulfilment.application.monolith.location.LocationGateway` and implement `resolveByIdentifier`. Add unit tests covering: valid ID found, valid ID not found, invalid format, null/blank.”

### 1.2 Add contract-ish tests for Location gateway
**Deliverables**
- Tests using Quarkus test framework
- Use `@QuarkusTest` + test DB (Quarkus can spin up DB in test mode)

---

## Phase 2 — Task 2: Store Post-Commit Legacy Sync (Outbox) + UAT (1–1.5 days)

> Requirement: legacy sync must occur **only after Store change is committed**. fileciteturn2file4  

### 2.1 Introduce Transactional Outbox in the monolith (core design)
**Goal:** Strong guarantee, retryability, and microservice-readiness.

**Steps**
1. Create an `outbox` table/entity (e.g., `OutboxMessage`):
   - `id` (UUID), `aggregateType`, `aggregateId`, `eventType`, `payloadJson`, `createdAt`, `publishedAt`, `attempts`, `lastError`
   - optional: `correlationId`, `aggregateVersion`
2. In Store write operations (create/update/patch/delete):
   - within the same DB transaction: persist Store + insert Outbox message
   - **remove direct legacy calls from REST methods**
3. Add a background publisher (“relay”) that:
   - polls unprocessed messages
   - calls `LegacyStoreManagerGateway`
   - marks message published on success
   - retries with backoff; stops and records `lastError` after N attempts

**Codex prompt (implementation)**
- “Implement a Transactional Outbox:
  - Create `OutboxMessage` JPA entity + repository.
  - Add `StoreChanged` events for create/update/patch/delete written within the Store transaction.
  - Implement `OutboxPublisher` scheduled job using Quarkus scheduler to publish pending messages via `LegacyStoreManagerGateway` and mark them published.
  - Ensure idempotency fields exist (eventId + aggregateId + version).
  - Ensure StoreResource no longer calls legacy gateway directly.”

### 2.2 Decide message format + event types
**Recommendation**
- Event types: `StoreCreated`, `StoreUpdated`, `StorePatched`, `StoreDeleted`
- Payload: full Store snapshot (safe for legacy sync)

### 2.3 Tests (must-have) proving post-commit behavior
**Automated tests**
- **Rollback test:** force DB rollback (e.g., violate unique constraint) and assert **no outbox message** committed (or not published).
- **Commit test:** perform store update and assert outbox record exists; then trigger publisher and assert legacy gateway invoked.
- **Retry test:** make legacy gateway throw; ensure outbox remains pending; later succeeds and is marked published.

**Codex prompt (tests)**
- “Add tests verifying:
  - outbox row is inserted only if Store transaction commits
  - legacy gateway is invoked only by publisher (not in REST path)
  - publisher retries and records lastError/attempts
  - delete emits event (ensure parity).”

### 2.4 User Acceptance Test plan for Task 2
**UAT scenarios**
1. Create store → DB row exists → within polling interval legacy receives create.
2. Create store with invalid data that triggers rollback → no legacy call occurs.
3. Update store → legacy receives only committed state (compare payload to persisted row).
4. Simulate legacy downtime → system still returns 2xx for Store write, outbox grows; once restored, publisher drains backlog.

**UAT execution notes**
- Provide a small Postman collection or curl script.
- Expose an admin endpoint for outbox status (optional but helpful).

---

## Phase 3 — Task 3: Warehouse Endpoints + Use Cases + Validations (1–2 days)

Warehouse requirements and validations are explicit in CODE_ASSIGNMENT. fileciteturn2file0  

### 3.1 Map the API surface (OpenAPI-generated interfaces)
**Goal:** Ensure endpoint handlers are correctly wired (no UnsupportedOperationException).

**Codex prompt**
- “Find the generated Warehouse API interface and its implementation (`WarehouseResourceImpl`). List endpoints and identify which methods throw UnsupportedOperationException or are incomplete. Implement handler methods to call use cases.”

### 3.2 Implement core Warehouse use cases
**Use cases**
- Create warehouse
- Retrieve warehouse(s)
- Replace warehouse
- Archive warehouse

**Validations (create)**
- Business unit code unique
- Location exists (via LocationGateway)
- Max warehouses per location not exceeded
- Capacity <= location max capacity
- Capacity can handle stock informed fileciteturn2file0  

**Validations (replace)**
- New capacity accommodates old warehouse stock
- Stock matches old stock exactly fileciteturn2file0  

**Codex prompt**
- “Implement the Warehouse use cases with the validations listed in CODE_ASSIGNMENT. Ensure response codes are appropriate (400/409/404 etc). Add persistence as needed and ensure archive marks state rather than hard delete unless required.”

### 3.3 Testing strategy for Warehouse
**Unit tests**
- pure validation tests for each rule

**Integration tests**
- create warehouse OK path
- duplicate business unit code → 409
- invalid location → 404/400
- max warehouses at location reached → 409
- capacity over location max → 400
- replace with mismatched stock → 400
- replace where new capacity < old stock → 400
- archive transitions state, prevents further replace (if applicable)

### 3.4 Warehouse UAT plan
- Happy paths for create/get/replace/archive
- Negative paths for each validation above
- Include sample payloads and expected responses

---

## Phase 4 — Bonus: Fulfillment Constraints (Saga-ready in monolith) (1–2 days, optional)

Bonus constraints: fileciteturn2file0  
1) Product fulfilled by max 2 warehouses per store  
2) Store fulfilled by max 3 warehouses  
3) Warehouse stores max 5 product types  

### 4.1 Best-fit approach for assignment scope
Because this is still one deployable unit, implement as **a single local transaction** (simpler than distributed Saga) but model it as **a future Saga**:
- Create a “FulfillmentAssociationService” (application service/use case)
- Put constraint checks into a single place
- Use DB constraints/indexes where possible to prevent race-condition violations

### 4.2 Data model suggestions
- `fulfillment_assignment` table:
  - `store_id`, `product_id`, `warehouse_id`
  - unique constraints to prevent duplicates
- Aggregate queries for counts:
  - warehouses per (store, product)
  - warehouses per store
  - distinct products per warehouse

### 4.3 Tests + UAT
- Positive: create 2 assignments for same product-store with different warehouses
- Negative: third warehouse for same product-store rejected
- Negative: fourth distinct warehouse for same store rejected
- Negative: 6th distinct product for same warehouse rejected

---

## Phase 5 — Containerisation & Local Dev UX (½ day)

### 5.1 docker-compose (DB + app optional)
From README, Postgres is required; Quarkus can run locally. fileciteturn2file3  

**Deliverables**
- `docker-compose.yml` for Postgres
- optional `docker-compose.override.yml` to run the Quarkus app containerized

### 5.2 Dockerfile (optional)
- Multi-stage build: Maven build then runtime image
- Configure via env vars: DB URL/user/password

**Codex prompt**
- “Add docker-compose.yml for Postgres and an optional Dockerfile to run the app. Document run commands in README or a new DEV.md.”

---

## Phase 6 — “Microservice-ready” optional extraction plan (Strangler Fig) (design-only, unless requested)

Not required for the assignment, but helpful if you want to show architecture maturity.

### 6.1 Extractable candidates
1) **Store Sync Service**
- Owns outbox relay and legacy integration
- Consumes StoreChanged events

2) **Warehouse Service**
- Already has stronger ports/adapters vibe
- Owns warehouse persistence + validations

### 6.2 Internal communication approach
- Keep it simple: event bus (Kafka/RabbitMQ) later
- For now: outbox relay can publish to an in-process channel OR write to a table
- When splitting: outbox relay becomes broker publisher

### 6.3 Saga when splitting
- Replace Warehouse becomes orchestrated saga:
  - steps per service, compensation on failures
- Outbox is still used per service for reliable event publication

---

## Phase 7 — Documentation & “How to Run / How to Test” (½ day)

### 7.1 Update docs
- `README` additions:
  - how to start DB (docker-compose)
  - how to run app
  - how to run tests
  - how to run UAT scripts / Postman

### 7.2 Provide UAT artifacts
- `uat/` folder:
  - `store_task2_uat.sh` (curl)
  - `warehouse_uat.sh`
  - expected response examples

**Codex prompt**
- “Create UAT scripts under uat/ with curl commands for Task 2 and Task 3, including failure cases. Add instructions in README.”

---

# Suggested Execution Order Checklist (copy/paste for Codex)

1. Implement LocationGateway.resolveByIdentifier + tests. fileciteturn2file4  
2. Add Outbox entity/table + repository.  
3. Refactor StoreResource to only write Store + Outbox message in same transaction. fileciteturn2file4  
4. Implement OutboxPublisher scheduled relay + retry/backoff + metrics/logs.  
5. Add Task 2 tests proving post-commit guarantee. fileciteturn2file4  
6. Implement Warehouse endpoint handlers + use cases + validations + tests. fileciteturn2file0  
7. Optional: implement Bonus fulfillment constraints + tests. fileciteturn2file0  
8. Add docker-compose for Postgres + optional Dockerfile; document in README. fileciteturn2file3  
9. Add UAT scripts and brief docs for running them.


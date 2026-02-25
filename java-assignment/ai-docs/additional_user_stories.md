# Additional User Stories for a Monolith → Microservices Transition

Context: Current system is a Quarkus monolith with mixed styles across modules (active-record Store, repository Product, partial ports/adapters for Warehouse) and a key requirement that Store changes **must sync to a legacy system only after DB commit**. fileciteturn0file0 fileciteturn0file1

These stories are written to be implementation-oriented and explicitly call out where **Saga** and/or **Transactional Outbox** patterns help.

---

## 1) Store → Legacy Sync Reliability (Outbox-first)

### US-01: Guarantee post-commit legacy sync for Store changes
**As** a platform engineer  
**I want** Store create/update/patch/delete to emit a “StoreChanged” event only after the Store row is committed  
**So that** the legacy Store Manager never receives changes that later roll back.

**Acceptance criteria**
- When Store transaction rolls back, **no** downstream event/call occurs.
- When Store commits, exactly one event is recorded for that change (idempotency key = storeId + version or storeId + txId).
- Integration retries do not create duplicate side-effects in legacy.

**Implementation notes**
- Use **Transactional Outbox**: write Store + Outbox row in same DB transaction; relay publishes to broker/HTTP worker afterward.
- If keeping synchronous HTTP to legacy, do it from a background worker reading Outbox; not from the REST transaction.

---

### US-02: Provide delivery guarantees and observability for Store sync
**As** an operator  
**I want** to monitor and re-drive failed Store sync messages  
**So that** integration failures don’t silently diverge state.

**Acceptance criteria**
- Outbox relay exposes metrics: pending count, publish latency, failure rate, retry count.
- Failed publishes go to a dead-letter mechanism after N retries and are visible in an admin endpoint.
- A “replay by storeId/date range” endpoint exists for controlled reprocessing.

---

### US-03: Make legacy sync idempotent and safe to retry
**As** an integrator  
**I want** each outbound request/event to include an idempotency token  
**So that** at-least-once delivery won’t duplicate records.

**Acceptance criteria**
- Every event includes `eventId` and `aggregateVersion` (or monotonic sequence).
- Legacy call includes `Idempotency-Key` header (or equivalent field).
- Duplicate deliveries are ignored by legacy (or our adapter) without error.

---

## 2) Service Boundary Discovery (Strangler Fig)

### US-04: Introduce “bounded context” APIs without breaking clients
**As** a developer  
**I want** to front the monolith with an API gateway (or façade module)  
**So that** we can gradually route Store/Warehouse traffic to new services.

**Acceptance criteria**
- Existing endpoints remain stable.
- New endpoints can be introduced per bounded context (Store, Warehouse, Product, Location).
- Routing can be toggled per endpoint using config/feature flags.

---

### US-05: Extract Warehouse module behind a stable service contract
**As** a product owner  
**I want** Warehouse operations to be available via a dedicated service interface  
**So that** Warehouse rules can evolve independently.

**Acceptance criteria**
- Warehouse API contract is versioned and published (OpenAPI).
- Warehouse service owns its persistence and business validations.
- Monolith calls Warehouse via adapter (HTTP/gRPC/messaging) with timeouts/circuit breaker.

**Pattern help**
- For synchronous request/response: use resiliency patterns (timeouts, retries, circuit breaker).
- For cross-service workflows (e.g., replacing warehouse + constraints), prefer **Saga**.

---

## 3) Cross-Service Consistency (Saga)

### US-06: “Replace Warehouse” as a Saga across services
**As** a fulfillment manager  
**I want** replacing a warehouse to be reliable even if partial failures occur  
**So that** stock/capacity constraints remain consistent.

**Acceptance criteria**
- Replace flow coordinates steps:
  1) Validate location + capacity limits
  2) Create/activate new warehouse
  3) Migrate/verify stock
  4) Archive old warehouse
- If any step fails, compensations occur:
  - If stock migration fails: deactivate new warehouse, keep old active
  - If archiving fails: mark saga as pending and retry archive

**Implementation notes**
- Use **orchestrated Saga** (a “WarehouseReplacementCoordinator” service) or **choreographed** events.
- Use Outbox in each service to publish state changes reliably.

---

### US-07: Fulfillment constraints enforcement via Saga
(BONUS-domain constraints from assignment)

**As** a planner  
**I want** the “associate warehouse as fulfillment unit” constraints to hold across Store/Product/Warehouse boundaries  
**So that** we never violate caps (2 warehouses per product per store, 3 warehouses per store, 5 product types per warehouse). fileciteturn0file1

**Acceptance criteria**
- Attempting to exceed any constraint fails deterministically.
- Concurrency-safe: two requests racing cannot violate constraints.
- If one service accepts and another rejects, the system compensates and returns a clear error.

**Pattern help**
- Use a Saga that reserves capacity/slots:
  - Step A: reserve “store-warehouse slots” in Store service
  - Step B: reserve “product-warehouse slot” in Product service
  - Step C: reserve “warehouse product-type slot” in Warehouse service
  - Commit all reservations or compensate (release) on failure.
- Each reservation write uses Outbox to publish “Reserved/Released” events.

---

## 4) Data Ownership and Duplication

### US-08: Define canonical ownership for Location data
**As** an architect  
**I want** Location to be owned by a single service  
**So that** other services reference it consistently.

**Acceptance criteria**
- One service is the system of record for Location (IDs, max capacity, max warehouses).
- Other services store only references plus a cached snapshot (optional).
- Changes to Location propagate by events, not shared DB tables.

**Pattern help**
- Outbox events “LocationUpdated” for cache invalidation or snapshot refresh.

---

### US-09: Establish read-models for queries that span services
**As** a frontend developer  
**I want** a unified “Fulfillment View” endpoint  
**So that** I can query Store + Products + Warehouses without multiple round trips.

**Acceptance criteria**
- A dedicated query service/materialized view is built from events.
- Read model is eventually consistent; staleness is measurable (lag metric).
- Full rebuild is possible from event log (or snapshots + log).

---

## 5) Resilience, Performance, and Operations

### US-10: Introduce standardized resiliency policies for inter-service calls
**As** an SRE  
**I want** consistent timeouts, retries, circuit breakers, and bulkheads  
**So that** one degraded service doesn’t cascade failures.

**Acceptance criteria**
- All sync calls have explicit timeouts and bounded retries.
- Circuit breakers trip on sustained failure and auto-recover.
- Bulkheads prevent thread pool exhaustion.

---

### US-11: Provide end-to-end tracing and correlation IDs
**As** an operator  
**I want** to trace a request across services and async events  
**So that** production incidents can be debugged quickly.

**Acceptance criteria**
- Correlation ID is generated at ingress and propagated via headers and message metadata.
- Trace spans include Outbox relay publish and consumer processing.
- Logs include correlation ID and aggregate IDs.

---

### US-12: Introduce schema/version compatibility for events
**As** a platform engineer  
**I want** event schemas to be versioned and backward compatible  
**So that** services can deploy independently.

**Acceptance criteria**
- Events include schema version.
- Consumers tolerate unknown fields.
- Breaking changes require parallel topic/stream or versioned event types.

---

## 6) Migration Safety and Delivery Strategy

### US-13: Implement a dual-write/dual-read safety window (limited time)
**As** a migration lead  
**I want** a controlled period where monolith and new service outputs can be compared  
**So that** we can validate correctness before cutover.

**Acceptance criteria**
- For selected flows, emit events to both legacy and new pipeline (or mirror to a shadow topic).
- A comparator job reports mismatches (Store counts, versions, key fields).
- Feature flag allows disabling shadow mode instantly.

**Pattern help**
- Outbox makes dual-publishing and audit logs straightforward.

---

### US-14: Backfill events for existing data
**As** a migration lead  
**I want** to publish initial snapshots/events for existing Stores/Warehouses/Products  
**So that** new services/read models start with correct state.

**Acceptance criteria**
- Backfill is idempotent and resumable.
- Backfill does not overload production DB (rate-limited).
- A completion marker is recorded per aggregate type.

---

## 7) Governance and Security

### US-15: Enforce service-level authorization boundaries
**As** a security engineer  
**I want** authorization decisions to be enforced per bounded context  
**So that** extracting a service doesn’t weaken access control.

**Acceptance criteria**
- Each service validates JWT/scopes/roles for its operations.
- Gateway/facade does not become the only enforcement point.
- Audit logs capture who changed what and when.

---

# Notes: When to choose Outbox vs Saga

- **Outbox** is the default for *reliable event publishing after local DB commit* (perfect fit for the Store→Legacy “post-commit” requirement). fileciteturn0file0
- **Saga** is for *multi-step workflows spanning multiple services* where you need coordination + compensations (e.g., Warehouse replacement, fulfillment association constraints). fileciteturn0file1
- They work best together: each step in a Saga performs a **local transaction** and uses **Outbox** to publish the next event reliably.


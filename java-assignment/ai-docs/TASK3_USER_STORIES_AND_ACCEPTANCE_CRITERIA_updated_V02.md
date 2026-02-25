# Task 3 User Stories and Acceptance Criteria — Updated V02

This document reviews and updates the current **Codex-derived** Task 3 user stories/acceptance criteria against Task 3 requirements in `CODE_ASSIGNMENT.md`. fileciteturn3file0 fileciteturn3file1

---

## 1) Review summary (what’s good, what to tighten)

### What’s already solid
The current stories cover the full “must have” surface area:
- **Create**, **List**, **Get by ID**, **Replace**, **Archive** are all present. fileciteturn3file1
- All required validations are included:
  - Business unit uniqueness
  - Location validity
  - Max warehouses per location
  - Capacity/stock constraints
  - Replacement constraints (capacity + stock matching) fileciteturn3file0

### What should be clarified/adjusted
These are small but important to avoid implementation ambiguity and test brittleness:

1) **HTTP status code semantics**
- Your criteria use `400` for invalid location and `404` for “not found warehouse”. Both can be valid, but define consistently:
  - **Warehouse not found**: `404`
  - **Invalid request payload / invalid identifier format**: `400`
  - **Valid request but conflicts with business constraints** (duplicate businessUnitCode, max warehouses reached): `409`

2) **Replacement wording**
- Current story says “keep the same business unit code.” That is likely true (path is `{businessUnitCode}`), but the important requirement is: **replace active warehouse associated with that businessUnitCode** and archive the previous active record. We should specify:
  - after replacement, there is still **exactly one active warehouse** for that businessUnitCode.

3) **Capacity rule phrasing**
- Current criteria says: “capacity would cause total active capacity at the location to exceed location max capacity”.
- The assignment requirement says: “capacity does not exceed max capacity associated with the location AND can handle the stock informed.” fileciteturn3file0  
  If the domain is “per-warehouse max capacity at that location”, then “total active capacity” is not required.
  - **Update:** phrase the rule as “warehouse capacity ≤ location.maxCapacity” unless the codebase explicitly models an aggregate capacity constraint.

4) **Archived handling**
- You already defined: archived warehouses not listed and `GET /warehouse/{id}` returns 404 for archived. Good. Make the rule explicit:
  - Archive is a **soft delete** state transition.
  - Replacement archives the previous one.

5) **Endpoint naming/package naming**
- The assignment states package `...warehouse`; user stories note repo uses `...warehouses`. Keep tests resilient to the actual endpoint path and implementation package. fileciteturn3file0 fileciteturn3file1

6) **Validation ordering / error precedence**
- For consistent API behavior, specify precedence:
  - Parse/format errors (`400`) first
  - Not-found (`404`) next
  - Constraint violations (`409`) last
  - Payload validation (`400`) for missing/negative numbers

---

## 2) Updated user stories (V02)

> Notes:
> - The endpoint path below uses the assignment’s `/warehouse` form (as in the existing stories). Adjust if the code uses `/warehouses`.
> - “Active” means “not archived”.

### T3-US-01: List active warehouses
**As** an operations user  
**I want** to list all active warehouse units  
**So that** I can see currently usable fulfillment infrastructure.

**Acceptance criteria**
- Given active and archived warehouses exist, when `GET /warehouse` is called, then response is `200` and contains **only active** warehouses.
- Each returned item includes: `id`, `businessUnitCode`, `locationIdentifier`, `capacity`, `stock` (or the equivalent fields used by the API DTO).

---

### T3-US-02: Get warehouse by ID
**As** an operations user  
**I want** to retrieve one warehouse by ID  
**So that** I can inspect a specific warehouse unit.

**Acceptance criteria**
- Given an existing **active** warehouse ID, when `GET /warehouse/{id}` is called, then response is `200` and returns that warehouse.
- Given a non-existing warehouse ID, when `GET /warehouse/{id}` is called, then response is `404`.
- Given an **archived** warehouse ID, when `GET /warehouse/{id}` is called, then response is `404`.
- Given a non-numeric ID, when `GET /warehouse/{id}` is called, then response is `400`.

---

### T3-US-03: Create warehouse with valid data
**As** a fulfillment manager  
**I want** to create a warehouse  
**So that** capacity can be added to a valid location.

**Acceptance criteria**
- Given a valid request payload, when `POST /warehouse` is called, then response is `201` (or `200` if the current API contract requires it) and returns the created warehouse.
- When the new warehouse is listed, then it appears in `GET /warehouse` as active.

---

### T3-US-04: Enforce unique business unit code on creation
**As** a fulfillment manager  
**I want** business unit codes to be unique for active warehouses  
**So that** warehouse identity is unambiguous.

**Acceptance criteria**
- Given an **active** warehouse already exists with the same `businessUnitCode`, when `POST /warehouse` is called, then response is `409`.
- (Optional, if you choose to allow reuse) Given only an **archived** warehouse exists with that `businessUnitCode`, when `POST /warehouse` is called, then creation is allowed and response is `201/200`.

> If the codebase already enforces uniqueness across all records (active + archived), remove the optional criterion and keep strict `409`.

---

### T3-US-05: Validate location on creation
**As** a fulfillment manager  
**I want** warehouse location values to be validated  
**So that** warehouses are created only in known locations.

**Acceptance criteria**
- Given a location identifier that does not exist, when `POST /warehouse` is called, then response is `400` (invalid business request) **or** `404` (resource not found) — choose one and keep consistent across create/replace.
- Given a blank/missing location identifier, when `POST /warehouse` is called, then response is `400`.

---

### T3-US-06: Enforce location warehouse-count limit on creation
**As** a fulfillment manager  
**I want** to prevent creating warehouses beyond a location’s maximum allowed count  
**So that** operational constraints are respected.

**Acceptance criteria**
- Given the location already has its maximum number of **active** warehouses, when `POST /warehouse` is called for that location, then response is `409`.

---

### T3-US-07: Enforce capacity and stock rules on creation
**As** a fulfillment manager  
**I want** capacity constraints enforced  
**So that** warehouses remain operationally valid.

**Acceptance criteria**
- Given `capacity < stock`, when `POST /warehouse` is called, then response is `400`.
- Given `capacity` exceeds `location.maxCapacity`, when `POST /warehouse` is called, then response is `400`. fileciteturn3file0
- Given required fields are missing/blank, when `POST /warehouse` is called, then response is `400`.
- Given numeric values are negative, when `POST /warehouse` is called, then response is `400`.

---

### T3-US-08: Replace an active warehouse unit by businessUnitCode
**As** a fulfillment manager  
**I want** to replace the active warehouse for a given business unit code  
**So that** I can transition to a new unit while preserving the warehouse identity.

**Acceptance criteria**
- Given an active warehouse exists for `{businessUnitCode}`, when `POST /warehouse/{businessUnitCode}/replacement` is called with valid payload, then response is `200` and returns the replacement warehouse.
- Given replacement succeeds:
  - the previous active warehouse is archived
  - the replacement is active
  - there is **exactly one** active warehouse for that `businessUnitCode`
- Given no active warehouse exists for `{businessUnitCode}`, when replacement is requested, then response is `404`.

---

### T3-US-09: Validate replacement stock matching
**As** a fulfillment manager  
**I want** replacement stock to match the previous warehouse stock  
**So that** stock transfer is consistent.

**Acceptance criteria**
- Given replacement payload `stock` differs from current active warehouse stock, when replacement is requested, then response is `400`. fileciteturn3file0

---

### T3-US-10: Validate replacement capacity accommodation
**As** a fulfillment manager  
**I want** replacement capacity to accommodate stock  
**So that** inventory does not exceed physical capacity.

**Acceptance criteria**
- Given replacement `capacity` is less than previous warehouse stock, when replacement is requested, then response is `400`. fileciteturn3file0
- Given replacement `capacity` is less than replacement payload `stock`, when replacement is requested, then response is `400`.

---

### T3-US-11: Re-apply location constraints on replacement
**As** a fulfillment manager  
**I want** replacement operations to obey location constraints  
**So that** limits remain valid after relocation.

**Acceptance criteria**
- Given replacement location is invalid, when replacement is requested, then response is `400` (or `404` if that is your chosen convention).
- Given replacement would exceed maximum warehouses for target location, when replacement is requested, then response is `409`.
- Given replacement `capacity` exceeds `location.maxCapacity`, when replacement is requested, then response is `400`. fileciteturn3file0

---

### T3-US-12: Archive warehouse by ID
**As** an operations user  
**I want** to archive a warehouse  
**So that** it is no longer active for fulfillment.

**Acceptance criteria**
- Given an existing active warehouse ID, when `DELETE /warehouse/{id}` is called, then response is `204` and the warehouse becomes archived.
- Given an archived warehouse, when `GET /warehouse` is called, then it does not include the archived warehouse.
- Given non-existing warehouse ID, when `DELETE /warehouse/{id}` is called, then response is `404`.
- Given non-numeric ID, when `DELETE /warehouse/{id}` is called, then response is `400`.
- (Optional but recommended) Given an already archived warehouse ID, when `DELETE /warehouse/{id}` is called, then response is `404` or `204` (idempotent delete) — choose one convention and document it.

---

## 3) Additional “tightening” criteria (recommended, still within scope)

These are not new features; they improve correctness and testability.

### T3-NFR-01: Validation precedence and deterministic errors
**Acceptance criteria**
- For all endpoints: invalid path parameters → `400` before any DB lookup.
- For replacement: if `{businessUnitCode}` not found → `404` even if payload is invalid (or choose the opposite, but keep consistent).
- Constraint violations that depend on DB state use `409`.

### T3-NFR-02: Concurrency safety for uniqueness/limits
**Acceptance criteria**
- Under concurrent create requests with the same `businessUnitCode`, at most one succeeds; the others return `409`.
- Under concurrent creates to the same location, the max-warehouses constraint is not violated.

> Implementation hint: DB unique constraints + transaction isolation / locking are often needed.

---

## 4) Traceability mapping to `CODE_ASSIGNMENT.md`
- Create/retrieve/replace/archive handlers and use cases: T3-US-01/02/03/08/12 fileciteturn3file0  
- Business unit code verification: T3-US-04 fileciteturn3file0  
- Location validation: T3-US-05, T3-US-11 fileciteturn3file0  
- Warehouse creation feasibility: T3-US-06 fileciteturn3file0  
- Capacity & stock validation: T3-US-07, T3-US-10, T3-US-11 fileciteturn3file0  
- Replacement validations: T3-US-09, T3-US-10 fileciteturn3file0  

---

## Bottom line
- **Yes**, the Codex stories are broadly correct and cover all required statements.
- **V02 adjustments** mainly remove ambiguity (capacity rule, replacement semantics, status code conventions, archived behavior) to make implementation + tests smoother and more deterministic.


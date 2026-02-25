# Task 3 User Stories and Acceptance Criteria

## Scope and Source
- Source requirement: `CODE_ASSIGNMENT.md` -> Task 3 (Warehouse, Must have).
- Current implementation reference: `com.fulfilment.application.monolith.warehouses` package and `/warehouse` endpoints.

## Notes
- Assignment text says package `...warehouse`; current repo uses `...warehouses`.
- Acceptance criteria below are written to be testable against current repo behavior.

## User Stories

### T3-US-01: List active warehouses
As an operations user, I want to list all active warehouse units so that I can see currently usable fulfillment infrastructure.

Acceptance criteria:
- Given active and archived warehouses exist, when `GET /warehouse` is called, then response is `200` and contains only active warehouses.
- Given active warehouses exist, when `GET /warehouse` is called, then each item includes `id`, `businessUnitCode`, `location`, `capacity`, and `stock`.

### T3-US-02: Get warehouse by ID
As an operations user, I want to retrieve one warehouse by ID so that I can inspect a specific warehouse unit.

Acceptance criteria:
- Given an existing active warehouse ID, when `GET /warehouse/{id}` is called, then response is `200` and returns that warehouse.
- Given a non-existing or archived warehouse ID, when `GET /warehouse/{id}` is called, then response is `404`.
- Given a non-numeric ID, when `GET /warehouse/{id}` is called, then response is `400`.

### T3-US-03: Create warehouse with valid data
As a fulfillment manager, I want to create a warehouse so that capacity can be added to a valid location.

Acceptance criteria:
- Given a valid request payload, when `POST /warehouse` is called, then a warehouse is created and returned.
- Given a successful creation, when the new warehouse is listed, then it appears in `GET /warehouse` as active.

### T3-US-04: Enforce unique business unit code on creation
As a fulfillment manager, I want business unit codes to be unique so that warehouse identity is unambiguous.

Acceptance criteria:
- Given an active warehouse already exists with the same `businessUnitCode`, when `POST /warehouse` is called, then response is `409`.

### T3-US-05: Validate location on creation
As a fulfillment manager, I want warehouse location values to be validated so that warehouses are created only in known locations.

Acceptance criteria:
- Given a location identifier that does not exist, when `POST /warehouse` is called, then response is `400`.

### T3-US-06: Enforce location warehouse-count limit on creation
As a fulfillment manager, I want to prevent creating warehouses beyond a location's maximum allowed count.

Acceptance criteria:
- Given the location already has its maximum number of active warehouses, when `POST /warehouse` is called for that location, then response is `409`.

### T3-US-07: Enforce capacity and stock rules on creation
As a fulfillment manager, I want capacity constraints enforced so that warehouses remain operationally valid.

Acceptance criteria:
- Given `capacity < stock`, when `POST /warehouse` is called, then response is `400`.
- Given `capacity` would cause total active capacity at the location to exceed location max capacity, when `POST /warehouse` is called, then response is `400`.
- Given required fields are missing/blank or numeric values are negative, when `POST /warehouse` is called, then response is `400`.

### T3-US-08: Replace an active warehouse unit
As a fulfillment manager, I want to replace an active warehouse so that I can transition to a new unit while keeping the same business unit code.

Acceptance criteria:
- Given an active warehouse exists for `{businessUnitCode}`, when `POST /warehouse/{businessUnitCode}/replacement` is called with valid payload, then response is `200` and returns replacement warehouse data.
- Given replacement succeeds, when `GET /warehouse` is called, then the previous warehouse version is archived (not listed) and the replacement is active.

### T3-US-09: Validate replacement stock matching
As a fulfillment manager, I want replacement stock to match the previous warehouse stock so that stock transfer is consistent.

Acceptance criteria:
- Given replacement payload `stock` differs from current active warehouse stock, when `POST /warehouse/{businessUnitCode}/replacement` is called, then response is `400`.

### T3-US-10: Validate replacement capacity accommodation
As a fulfillment manager, I want replacement capacity to accommodate stock so that inventory does not exceed physical capacity.

Acceptance criteria:
- Given replacement `capacity` is less than previous warehouse stock, when replacement is requested, then response is `400`.
- Given replacement `capacity` is less than replacement payload `stock`, when replacement is requested, then response is `400`.

### T3-US-11: Re-apply location constraints on replacement
As a fulfillment manager, I want replacement operations to obey location constraints so that limits remain valid after relocation.

Acceptance criteria:
- Given replacement location is invalid, when replacement is requested, then response is `400`.
- Given replacement would exceed maximum warehouses for target location, when replacement is requested, then response is `409`.
- Given replacement would exceed location max capacity, when replacement is requested, then response is `400`.

### T3-US-12: Archive warehouse by ID
As an operations user, I want to archive a warehouse so that it is no longer active for fulfillment.

Acceptance criteria:
- Given an existing active warehouse ID, when `DELETE /warehouse/{id}` is called, then response is `204` and warehouse is archived.
- Given an archived warehouse, when `GET /warehouse` is called, then it does not include the archived warehouse.
- Given non-existing warehouse ID, when `DELETE /warehouse/{id}` is called, then response is `404`.
- Given non-numeric ID, when `DELETE /warehouse/{id}` is called, then response is `400`.

## Traceability to Task 3 Requirement Statements
- Implement Warehouse creation, retrieval, replacement, archive: T3-US-01, T3-US-02, T3-US-03, T3-US-08, T3-US-12.
- Business Unit Code Verification: T3-US-04.
- Location Validation: T3-US-05, T3-US-11.
- Warehouse Creation Feasibility: T3-US-06.
- Capacity and Stock Validation: T3-US-07, T3-US-10, T3-US-11.
- Additional replacement validations (capacity accommodation, stock matching): T3-US-09, T3-US-10.

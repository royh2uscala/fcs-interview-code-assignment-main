# Task 1 Implementation Plan - Location Gateway

## Understanding Confirmation

I understand the assignment tasks in `CODE_ASSIGNMENT.md`:

1. Implement `resolveByIdentifier` in `com.fulfilment.application.monolith.location.LocationGateway` (Task 1, must have).
2. Ensure `StoreResource` sync calls to `LegacyStoreManagerGateway` happen only after DB commit (Task 2, must have).
3. Implement Warehouse API/use case logic with required validations and constraints (Task 3, must have).
4. Bonus: Product-Store-Warehouse fulfilment associations with additional constraints.

This plan focuses only on **Task 1**.

## Phase Status Legend

- `Todo[✓] Started[ ] Completed[ ]` = planned, not started
- `Todo[ ] Started[✓] Completed[ ]` = in progress
- `Todo[ ] Started[ ] Completed[✓]` = done

## Task 1 Phased Plan

### Phase 1 - Unit Test Scenarios First
Status: `Todo[ ] Started[ ] Completed[✓]`

1. Review current `LocationGatewayTest` coverage and identify missing scenarios for `resolveByIdentifier`.
2. Add/adjust tests for expected behavior:
   - valid known identifier resolves to a non-null location/domain object.
   - unknown identifier results in expected empty/exception behavior (based on existing contract).
   - null/blank identifier handling follows intended validation behavior.
   - identifier normalization behavior (if applicable, e.g., trim/case handling) matches contract.
3. Run only location-related tests to validate behavior after test additions.

### Phase 2 - Implement `resolveByIdentifier`
Status: `Todo[ ] Started[ ] Completed[✓]`

1. Implement method logic in `LocationGateway` to satisfy all tests.
2. Keep implementation aligned with existing architecture and method contract (no unrelated refactors).
3. Handle input validation and lookup behavior consistently with project conventions.

### Phase 3 - Verification and Stability Check
Status: `Todo[ ] Started[ ] Completed[✓]`

1. Re-run location tests and confirm all pass.
2. Run related module tests likely affected by location resolution usage.
3. Perform a quick code review pass for readability and edge-case handling.

### Phase 4 - Completion Notes
Status: `Todo[ ] Started[ ] Completed[✓]`

1. Implemented behavior:
   - returns matching `Location` for existing identifier.
   - returns `null` for unknown, `null`, or blank identifier.
   - trims input and resolves case-insensitively.
2. Test results for Task 1 scope:
   - `LocationGatewayTest`: 5 tests, 0 failures, 0 errors.
   - Targeted verification run succeeded: `LocationGatewayTest, CreateWarehouseUseCaseTest, ReplaceWarehouseUseCaseTest, ArchiveWarehouseUseCaseTest`.
3. All Task 1 phases marked as completed.

# Task 3 Concurrency Notes

## Implemented Controls

1. Active business unit uniqueness is enforced with DB index:
   - `uk_warehouse_active_buc` (partial unique index where `archivedAt is null`).
2. Create/replace critical sections use PostgreSQL transaction-scoped advisory locks:
   - via `WarehouseStore.lockForMutation(...)` and `pg_advisory_xact_lock(...)`
   - lock is held until transaction commit/rollback
3. Create/replace also use `WarehouseMutationLockManager` for JVM-local lock ordering:
   - lock key on business unit (`BU:*`)
   - lock key on location (`LOC:*`)
4. Domain rules are checked while lock is held:
   - duplicate business unit
   - max warehouses per location
   - capacity validations

## Tested Concurrency Scenarios

1. Concurrent create requests with same business unit:
   - exactly one success, one conflict.
2. Concurrent create requests against location limit:
   - location active count never exceeds configured max.

## Residual Risk / Assumptions

1. The advisory-lock strategy is PostgreSQL-specific.
2. Dynamic location limits are still enforced by application rules, not a dedicated DB table constraint.
3. For non-PostgreSQL multi-instance deployments, equivalent cross-instance lock semantics would be required.

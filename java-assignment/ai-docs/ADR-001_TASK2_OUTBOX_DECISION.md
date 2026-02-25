# ADR-001: Task 2 Store Sync Uses Transactional Outbox

## Status
Accepted

## Date
2026-02-24

## Context
Task 2 requires legacy synchronization to happen only after Store data is committed.
Direct legacy calls inside `@Transactional` REST handlers can publish changes that later roll back.

## Decision
Use a Transactional Outbox pattern:

1. Store mutation and outbox write happen in the same DB transaction.
2. A relay (`OutboxPublisher`) publishes committed outbox messages asynchronously.
3. Relay marks messages as published only on success.
4. Failures are retried with backoff and attempt/error metadata.
5. Replay controls are exposed via admin endpoints.

## Event Contract

1. Aggregate type: `Store`
2. Event types: `StoreCreated`, `StoreUpdated`, `StorePatched`, `StoreDeleted`
3. Fields:
   1. `eventId`
   2. `aggregateId`
   3. `eventType`
   4. `schemaVersion`
   5. `correlationId`
   6. `payloadJson`

## Consequences

### Positive
1. Guarantees no legacy sync on rolled-back transactions.
2. Provides retryability and observability.
3. Aligns with future microservice extraction.

### Tradeoffs
1. Adds outbox table and relay operational complexity.
2. Legacy propagation becomes eventually consistent.

## Related Endpoints

1. `GET /admin/outbox/stats`
2. `POST /admin/outbox/publish`
3. `POST /admin/outbox/replay`

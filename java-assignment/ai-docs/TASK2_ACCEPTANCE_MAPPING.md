# Task 2 Acceptance Mapping

## Assignment Requirement

`LegacyStoreManagerGateway` calls must happen only after Store DB changes are committed.

## Mapping to Additional Stories

1. US-01 post-commit guarantee
   1. Implemented via Store transaction + outbox row in same transaction.
   2. Relay publishes only committed outbox rows.
2. US-02 delivery guarantees/observability
   1. Outbox stats endpoint.
   2. Retry metadata: attempts, lastError, nextAttemptAt.
   3. Replay endpoint by aggregate/date range.
3. US-03 idempotent retry safety
   1. Event ID + idempotency key on publish.
   2. Gateway ignores duplicated idempotency key.

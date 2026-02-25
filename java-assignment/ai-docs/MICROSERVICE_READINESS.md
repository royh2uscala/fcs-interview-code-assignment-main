# Microservice Readiness Notes

## Implemented Hooks

1. Correlation ID propagation
   1. `X-Correlation-Id` accepted/generated on ingress.
   2. Correlation ID echoed in response.
   3. Correlation ID stored in outbox messages.
2. Replay controls
   1. Replay by aggregate and date range via `/admin/outbox/replay`.
3. Event versioning
   1. Outbox schema includes `schemaVersion`.
4. Relay metrics
   1. Publish success/failure counters.
   2. Average publish latency.

## Initial Strangler Boundaries

1. First extraction candidate: Store sync relay + outbox publisher.
2. Second extraction candidate: Warehouse bounded context (`warehouses` package).

## Compatibility Guidance

1. Keep event payload backward compatible:
   1. Add fields only as optional.
   2. Consumers must ignore unknown fields.
2. For breaking changes:
   1. Bump `schemaVersion`.
   2. Support parallel consumer logic during rollout.

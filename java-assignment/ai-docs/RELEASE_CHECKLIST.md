# Release Checklist

1. Build and packaging
   1. `./mvnw -DskipTests package` succeeds.
2. Unit test baseline
   1. Task 1 + Warehouse use-case tests pass.
3. Outbox readiness
   1. `outbox_message` table exists after startup.
   2. `GET /admin/outbox/stats` is reachable.
   3. Replay endpoint validated in non-prod.
4. Store sync safety
   1. No direct legacy calls from `StoreResource`.
   2. Legacy sync only via relay.
5. Warehouse endpoints
   1. Create/get/replace/archive endpoint behavior verified.
   2. Validation responses return expected status codes.
6. Bonus constraints
   1. Fulfillment assignment constraints verified.
7. Operational visibility
   1. Correlation ID header propagation validated.
   2. Relay metrics visible via stats endpoint.
8. Deployment guardrails
   1. Confirm Java runtime compatibility (`.mvn/jvm.config` includes ByteBuddy experimental flag for Java 23 environments).
   2. Confirm datasource/test profile configuration for target environment.

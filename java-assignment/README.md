# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".

## Assignment Implementation Notes

### Task 2: Store Post-Commit Legacy Sync

Store writes now use a Transactional Outbox flow:

1. `StoreResource` writes Store data and an `outbox_message` row in the same transaction.
2. `OutboxPublisher` asynchronously publishes pending outbox messages to `LegacyStoreManagerGateway`.
3. Messages are marked as published only after successful gateway execution.
4. Failed publishes are retried with backoff and tracked with attempts/error metadata.

Operational endpoints:

- `GET /admin/outbox/stats`
- `POST /admin/outbox/publish`
- `POST /admin/outbox/replay?aggregateId=...&from=...&to=...`

### Bonus Fulfillment Assignments

Added endpoint:

- `POST /fulfillment/assignment`

Constraints enforced:

1. Max 2 warehouses per product per store.
2. Max 3 warehouses per store.
3. Max 5 product types per warehouse.

### Task 3: Warehouse Validation and Lifecycle

Warehouse endpoints implemented:

- `GET /warehouse`
- `GET /warehouse/{id}`
- `POST /warehouse`
- `POST /warehouse/{businessUnitCode}/replacement`
- `DELETE /warehouse/{id}`

Validation/error conventions:

1. `400` invalid input or business validation failure (invalid id format, invalid location, capacity/stock issues).
2. `404` active warehouse not found.
3. `409` conflict constraints (duplicate business unit code, max warehouses reached at location).

Concurrency protections:

1. Active business-unit uniqueness via DB partial unique index.
2. PostgreSQL transaction-scoped advisory locks for create/replace critical sections (business unit + location keys).
3. In-process lock manager remains as a JVM-local guard for deterministic lock ordering.

## Local Database via Docker Compose

Use the compose file at project root:

```sh
docker compose up -d
```

Then run:

```sh
./mvnw quarkus:dev
```

## UAT Scripts

See `uat/` folder:

- `uat/store_task2_uat.sh`
- `uat/warehouse_uat.sh`

## Code Coverage (JaCoCo)

Coverage commands:

1. Generate coverage report (stable/local):
   - `./mvnw clean verify`
2. Enforce coverage gate (line `>=80%`, branch `>=60%`):
   - `./mvnw -Pcoverage clean verify`
3. Optional runtime/E2E coverage run (Docker/Testcontainers dependent):
   - `./mvnw -Pcoverage-e2e clean verify`

Report output:

- `target/site/jacoco/index.html`

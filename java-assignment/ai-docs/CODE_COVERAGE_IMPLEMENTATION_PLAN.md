# Code Coverage Implementation Plan (JaCoCo)

## Goal
Track and enforce source-code coverage using JaCoCo with an expected threshold of **80% or above**.

## 1. Define Coverage Policy First
1. Decide the primary metric: line coverage (and optionally branch coverage).
2. Set target: line coverage `>= 80%` (optionally branch `>= 60%`).
3. Define scope: include production packages; exclude generated/test/support code as needed.

## 2. Add JaCoCo to Maven Build
1. Add `jacoco-maven-plugin` in `pom.xml`.
2. Configure goals:
   1. `prepare-agent` (before tests)
   2. `report` (after tests)
   3. `check` (enforce threshold at `verify`)
3. Start with project-level threshold rule; later add package/class rules if needed.
4. Implemented thresholds:
   1. Line coverage `>= 0.80`
   2. Branch coverage `>= 0.60`

## 3. Handle Current Repo Test Modes
1. Use default `./mvnw test` as baseline coverage run (fast and stable).
2. Keep E2E/Testcontainers coverage optional in a separate run to avoid local/CI instability.
3. If needed later, merge unit + integration coverage into one report.
4. Implemented profile split:
   1. `coverage` profile: stable coverage gate, no E2E dependency.
   2. `coverage-e2e` profile: optional E2E coverage run (Docker/Testcontainers).

## 4. Produce and Publish Reports
1. Generate HTML/XML reports under `target/site/jacoco/`.
2. Use XML report for CI tools (e.g., SonarQube, quality gates).
3. Treat Maven JaCoCo output as source of truth (not IDE-only coverage views).

## 5. Enforce in CI
1. Add CI job running `./mvnw -Pcoverage clean verify`.
2. Fail build when JaCoCo threshold is not met.
3. Upload JaCoCo HTML report as CI artifact for investigation.

## 6. Raise Coverage to 80%+
1. Run baseline and identify low-coverage classes.
2. Prioritize high-value domains (warehouses, outbox/store flows, fulfillment constraints).
3. Add focused tests for:
   1. validation/error paths
   2. edge cases
   3. exception/status mapping
4. Re-run coverage after each batch and track trend.

## 7. Team Workflow Rules
1. New feature PRs should not reduce total coverage below threshold.
2. Bug fixes require accompanying regression tests.
3. Document local commands for quick coverage verification.

## Acceptance Criteria
1. `./mvnw clean verify` generates JaCoCo reports.
2. `./mvnw -Pcoverage clean verify` enforces 80%+ gate and fails on violations.
3. Optional E2E coverage runs separately via `./mvnw -Pcoverage-e2e clean verify`.
4. CI provides coverage evidence (report and/or quality gate result).

## Implemented Commands
1. Stable local coverage report (no gate): `./mvnw clean verify`
2. Stable local coverage gate: `./mvnw -Pcoverage clean verify`
3. Optional E2E/Testcontainers coverage run: `./mvnw -Pcoverage-e2e clean verify`

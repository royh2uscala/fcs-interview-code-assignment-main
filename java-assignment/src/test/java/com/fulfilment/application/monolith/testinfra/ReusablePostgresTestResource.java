package com.fulfilment.application.monolith.testinfra;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;
import org.testcontainers.containers.PostgreSQLContainer;

public class ReusablePostgresTestResource implements QuarkusTestResourceLifecycleManager {

  private static final Logger LOGGER = Logger.getLogger(ReusablePostgresTestResource.class);
  private static final Object LOCK = new Object();
  private static PostgreSQLContainer<?> sharedContainer;

  @Override
  public Map<String, String> start() {
    if (!isTestcontainersEnabled()) {
      LOGGER.info("Testcontainers disabled for E2E tests; using configured external datasource.");
      return externalDatasourceConfig();
    }

    try {
      synchronized (LOCK) {
        if (sharedContainer == null) {
          sharedContainer =
              new PostgreSQLContainer<>(imageName())
                  .withDatabaseName(dbName())
                  .withUsername(dbUsername())
                  .withPassword(dbPassword())
                  .withReuse(true);
          sharedContainer.start();
          LOGGER.infof(
              "Started reusable PostgreSQL Testcontainer at %s", sharedContainer.getJdbcUrl());
        } else if (!sharedContainer.isRunning()) {
          sharedContainer.start();
        }
      }
      return testcontainersDatasourceConfig();
    } catch (Throwable ex) {
      if (isContainerStartupRequired()) {
        throw ex;
      }
      return fallbackDatasourceConfig(ex);
    }
  }

  @Override
  public void stop() {
    if (!Boolean.parseBoolean(systemOrEnv("e2e.testcontainers.keep-running", "true"))) {
      synchronized (LOCK) {
        if (sharedContainer != null) {
          sharedContainer.stop();
          sharedContainer = null;
        }
      }
    }
  }

  private boolean isTestcontainersEnabled() {
    return Boolean.parseBoolean(systemOrEnv("e2e.testcontainers.enabled", "true"));
  }

  private boolean isContainerStartupRequired() {
    return Boolean.parseBoolean(systemOrEnv("e2e.testcontainers.required", "false"));
  }

  private Map<String, String> testcontainersDatasourceConfig() {
    Map<String, String> props = new HashMap<>();
    props.put("quarkus.datasource.devservices.enabled", "false");
    props.put("quarkus.datasource.db-kind", "postgresql");
    props.put("quarkus.datasource.jdbc.url", sharedContainer.getJdbcUrl());
    props.put("quarkus.datasource.username", sharedContainer.getUsername());
    props.put("quarkus.datasource.password", sharedContainer.getPassword());
    return props;
  }

  private Map<String, String> externalDatasourceConfig() {
    Map<String, String> props = new HashMap<>();
    props.put("quarkus.datasource.devservices.enabled", "false");
    props.put("quarkus.datasource.db-kind", "postgresql");
    props.put(
        "quarkus.datasource.jdbc.url",
        systemOrEnv("test.db.url", "jdbc:postgresql://localhost:15432/quarkus_test"));
    props.put("quarkus.datasource.username", systemOrEnv("test.db.username", "quarkus_test"));
    props.put("quarkus.datasource.password", systemOrEnv("test.db.password", "quarkus_test"));
    return props;
  }

  private Map<String, String> fallbackDatasourceConfig(Throwable startupFailure) {
    LOGGER.warn(
        "Testcontainers startup failed; falling back to configured external datasource. "
            + "Set e2e.testcontainers.required=true to fail fast instead.",
        startupFailure);
    return externalDatasourceConfig();
  }

  private String imageName() {
    return systemOrEnv("e2e.testcontainers.image", "postgres:13.3");
  }

  private String dbName() {
    return systemOrEnv("e2e.testcontainers.db-name", "quarkus_test");
  }

  private String dbUsername() {
    return systemOrEnv("e2e.testcontainers.username", "quarkus_test");
  }

  private String dbPassword() {
    return systemOrEnv("e2e.testcontainers.password", "quarkus_test");
  }

  private String systemOrEnv(String key, String defaultValue) {
    String sysValue = System.getProperty(key);
    if (sysValue != null && !sysValue.isBlank()) {
      return sysValue;
    }
    String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
    String envValue = System.getenv(envKey);
    if (envValue != null && !envValue.isBlank()) {
      return envValue;
    }
    return defaultValue;
  }
}

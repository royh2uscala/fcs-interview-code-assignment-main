package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.outbox.StoreChangedEventPayload;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);
  private final Set<String> processedIdempotencyKeys = ConcurrentHashMap.newKeySet();
  private final AtomicInteger processedEvents = new AtomicInteger();
  private final AtomicBoolean failNextPublication = new AtomicBoolean(false);
  private volatile boolean alwaysFailPublications;

  public void publishStoreEvent(
      String eventId,
      String idempotencyKey,
      String eventType,
      int schemaVersion,
      String correlationId,
      StoreChangedEventPayload payload) {
    if (alwaysFailPublications || failNextPublication.getAndSet(false)) {
      throw new IllegalStateException("Legacy gateway simulated failure");
    }

    if (!processedIdempotencyKeys.add(idempotencyKey)) {
      LOGGER.infof("Ignoring duplicated store sync event idempotencyKey=%s", idempotencyKey);
      return;
    }

    writeToFile(eventId, idempotencyKey, eventType, schemaVersion, correlationId, payload);
    processedEvents.incrementAndGet();
  }

  public int processedEventsCount() {
    return processedEvents.get();
  }

  public void failNextPublication() {
    failNextPublication.set(true);
  }

  public void setAlwaysFailPublications(boolean alwaysFailPublications) {
    this.alwaysFailPublications = alwaysFailPublications;
  }

  public void clearTestState() {
    processedEvents.set(0);
    failNextPublication.set(false);
    alwaysFailPublications = false;
    processedIdempotencyKeys.clear();
  }

  private void writeToFile(
      String eventId,
      String idempotencyKey,
      String eventType,
      int schemaVersion,
      String correlationId,
      StoreChangedEventPayload payload) {
    try {
      Path tempFile = Files.createTempFile("legacy-store-sync-", ".txt");

      String content =
          "eventId="
              + eventId
              + ",idempotencyKey="
              + idempotencyKey
              + ",eventType="
              + eventType
              + ",schemaVersion="
              + schemaVersion
              + ",correlationId="
              + correlationId
              + ",storeId="
              + payload.storeId
              + ",name="
              + payload.name
              + ",itemsOnStock="
              + payload.quantityProductsInStock
              + "]";

      Files.write(tempFile, content.getBytes());
      Files.delete(tempFile);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to write to legacy sync file", ex);
    }
  }
}

package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.stores.outbox.StoreChangedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LegacyStoreManagerGatewayTest {

  private LegacyStoreManagerGateway gateway;

  @BeforeEach
  public void setup() {
    gateway = new LegacyStoreManagerGateway();
    gateway.clearTestState();
  }

  @Test
  public void testPublishStoreEventShouldIncreaseProcessedCount() {
    gateway.publishStoreEvent(
        "evt-1",
        "idem-1",
        "StoreCreated",
        1,
        "corr-1",
        new StoreChangedEventPayload(10L, "STORE-1", 5));

    assertEquals(1, gateway.processedEventsCount());
  }

  @Test
  public void testPublishStoreEventShouldIgnoreDuplicateIdempotencyKey() {
    StoreChangedEventPayload payload = new StoreChangedEventPayload(10L, "STORE-1", 5);

    gateway.publishStoreEvent("evt-1", "idem-1", "StoreCreated", 1, "corr-1", payload);
    gateway.publishStoreEvent("evt-2", "idem-1", "StoreCreated", 1, "corr-2", payload);

    assertEquals(1, gateway.processedEventsCount());
  }

  @Test
  public void testPublishStoreEventShouldFailWhenFailNextPublicationEnabled() {
    gateway.failNextPublication();

    assertThrows(
        IllegalStateException.class,
        () ->
            gateway.publishStoreEvent(
                "evt-1",
                "idem-1",
                "StoreCreated",
                1,
                "corr-1",
                new StoreChangedEventPayload(10L, "STORE-1", 5)));
    assertEquals(0, gateway.processedEventsCount());
  }

  @Test
  public void testPublishStoreEventShouldFailWhenAlwaysFailEnabled() {
    gateway.setAlwaysFailPublications(true);

    assertThrows(
        IllegalStateException.class,
        () ->
            gateway.publishStoreEvent(
                "evt-1",
                "idem-1",
                "StoreCreated",
                1,
                "corr-1",
                new StoreChangedEventPayload(10L, "STORE-1", 5)));
    assertEquals(0, gateway.processedEventsCount());
  }

  @Test
  public void testClearTestStateShouldResetFailureFlagsAndIdempotencyCache() {
    StoreChangedEventPayload payload = new StoreChangedEventPayload(10L, "STORE-1", 5);
    gateway.publishStoreEvent("evt-1", "idem-1", "StoreCreated", 1, "corr-1", payload);
    gateway.failNextPublication();
    gateway.setAlwaysFailPublications(true);

    gateway.clearTestState();
    gateway.publishStoreEvent("evt-2", "idem-1", "StoreCreated", 1, "corr-2", payload);

    assertEquals(1, gateway.processedEventsCount());
  }

  @Test
  public void testPublishStoreEventShouldWrapWriteFailures() {
    assertThrows(
        IllegalStateException.class,
        () -> gateway.publishStoreEvent("evt-1", "idem-1", "StoreCreated", 1, "corr-1", null));
    assertEquals(0, gateway.processedEventsCount());
  }
}


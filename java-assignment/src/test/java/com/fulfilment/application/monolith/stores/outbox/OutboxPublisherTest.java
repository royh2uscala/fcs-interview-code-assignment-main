package com.fulfilment.application.monolith.stores.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

public class OutboxPublisherTest {

  private static class FakeOutboxRepository extends OutboxMessageRepository {
    final List<OutboxMessage> pending = new ArrayList<>();
    int markPublishedCalls;
    int markFailedCalls;
    LocalDateTime lastPublishedAt;
    String lastError;
    LocalDateTime lastNextAttemptAt;

    @Override
    public List<OutboxMessage> listPending(int limit, LocalDateTime now) {
      return pending;
    }

    @Override
    public void markPublished(OutboxMessage message, LocalDateTime publishedAt) {
      markPublishedCalls++;
      lastPublishedAt = publishedAt;
    }

    @Override
    public void markFailed(OutboxMessage message, String error, LocalDateTime nextAttemptAt) {
      markFailedCalls++;
      lastError = error;
      lastNextAttemptAt = nextAttemptAt;
    }
  }

  @Test
  public void testPublishPendingShouldPublishValidMessages() throws Exception {
    FakeOutboxRepository repository = new FakeOutboxRepository();
    OutboxMessage message = new OutboxMessage();
    message.id = 100L;
    message.eventId = "evt-100";
    message.aggregateType = "Store";
    message.aggregateId = "77";
    message.eventType = "StoreUpdated";
    message.schemaVersion = 1;
    message.correlationId = "corr-100";
    message.payloadJson =
        new ObjectMapper().writeValueAsString(new StoreChangedEventPayload(77L, "Store-Z", 15));
    message.attempts = 0;
    repository.pending.add(message);

    LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();
    gateway.clearTestState();
    OutboxPublisher publisher = new OutboxPublisher();
    publisher.outboxMessageRepository = repository;
    publisher.objectMapper = new ObjectMapper();
    publisher.legacyStoreManagerGateway = gateway;
    publisher.metrics = new OutboxPublisherMetrics();

    int processed = publisher.publishPending();

    assertEquals(1, processed);
    assertEquals(1, repository.markPublishedCalls);
    assertEquals(0, repository.markFailedCalls);
    assertNotNull(repository.lastPublishedAt);
    assertEquals(1, publisher.metrics.getPublishedCount());
    assertEquals(0, publisher.metrics.getFailedCount());
    assertEquals(1, gateway.processedEventsCount());
  }

  @Test
  public void testPublishPendingShouldMarkFailedWhenPayloadCannotBeParsed() {
    FakeOutboxRepository repository = new FakeOutboxRepository();
    OutboxMessage message = new OutboxMessage();
    message.id = 200L;
    message.eventId = "evt-200";
    message.aggregateType = "Store";
    message.aggregateId = "88";
    message.eventType = "StoreUpdated";
    message.schemaVersion = 1;
    message.correlationId = "corr-200";
    message.payloadJson = "{bad-json";
    message.attempts = 2;
    repository.pending.add(message);

    OutboxPublisher publisher = new OutboxPublisher();
    publisher.outboxMessageRepository = repository;
    publisher.objectMapper = new ObjectMapper();
    publisher.legacyStoreManagerGateway = new LegacyStoreManagerGateway();
    publisher.metrics = new OutboxPublisherMetrics();

    LocalDateTime before = LocalDateTime.now();
    int processed = publisher.publishPending();

    assertEquals(1, processed);
    assertEquals(0, repository.markPublishedCalls);
    assertEquals(1, repository.markFailedCalls);
    assertNotNull(repository.lastError);
    assertNotNull(repository.lastNextAttemptAt);
    long nextAttemptDelaySeconds = Duration.between(before, repository.lastNextAttemptAt).getSeconds();
    assertTrue(nextAttemptDelaySeconds >= 1);
    assertTrue(nextAttemptDelaySeconds <= 60);
    assertEquals(0, publisher.metrics.getPublishedCount());
    assertEquals(1, publisher.metrics.getFailedCount());
  }

  @Test
  public void testPublishPendingShouldReturnZeroWhenNoPendingMessages() {
    FakeOutboxRepository repository = new FakeOutboxRepository();
    OutboxPublisher publisher = new OutboxPublisher();
    publisher.outboxMessageRepository = repository;
    publisher.objectMapper = new ObjectMapper();
    publisher.legacyStoreManagerGateway = new LegacyStoreManagerGateway();
    publisher.metrics = new OutboxPublisherMetrics();

    int processed = publisher.publishPending();

    assertEquals(0, processed);
    assertEquals(0, repository.markPublishedCalls);
    assertEquals(0, repository.markFailedCalls);
  }

  @Test
  public void testStartShouldSkipSchedulerWhenDisabled() {
    OutboxPublisher publisher = new OutboxPublisher();
    publisher.enabled = false;

    publisher.start();

    assertNull(schedulerOf(publisher));
  }

  @Test
  public void testStartAndShutdownShouldManageSchedulerLifecycle() {
    OutboxPublisher publisher = new OutboxPublisher();
    publisher.outboxMessageRepository = new FakeOutboxRepository();
    publisher.objectMapper = new ObjectMapper();
    publisher.legacyStoreManagerGateway = new LegacyStoreManagerGateway();
    publisher.metrics = new OutboxPublisherMetrics();
    publisher.enabled = true;
    publisher.initialDelaySeconds = 3600;
    publisher.intervalSeconds = 3600;

    publisher.start();
    ScheduledExecutorService scheduler = schedulerOf(publisher);

    assertNotNull(scheduler);
    assertFalse(scheduler.isShutdown());

    publisher.shutdown();

    assertTrue(scheduler.isShutdown());
  }

  private static ScheduledExecutorService schedulerOf(OutboxPublisher publisher) {
    try {
      Field schedulerField = OutboxPublisher.class.getDeclaredField("scheduler");
      schedulerField.setAccessible(true);
      return (ScheduledExecutorService) schedulerField.get(publisher);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Unable to inspect scheduler field", ex);
    }
  }
}

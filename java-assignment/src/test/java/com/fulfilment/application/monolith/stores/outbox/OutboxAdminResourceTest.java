package com.fulfilment.application.monolith.stores.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class OutboxAdminResourceTest {

  private static class FakeOutboxRepository extends OutboxMessageRepository {
    long pendingCount;
    long failedCount;
    long publishedCount;
    int replayResult;
    String replayAggregateId;
    LocalDateTime replayFrom;
    LocalDateTime replayTo;

    @Override
    public long countPending() {
      return pendingCount;
    }

    @Override
    public long countFailed() {
      return failedCount;
    }

    @Override
    public long countPublished() {
      return publishedCount;
    }

    @Override
    public int replay(String aggregateId, LocalDateTime from, LocalDateTime to) {
      replayAggregateId = aggregateId;
      replayFrom = from;
      replayTo = to;
      return replayResult;
    }
  }

  private static class StubOutboxPublisher extends OutboxPublisher {
    int processed;

    @Override
    public int publishPending() {
      return processed;
    }
  }

  @Test
  public void testStatsShouldReturnRepositoryAndRelayMetrics() {
    FakeOutboxRepository repository = new FakeOutboxRepository();
    repository.pendingCount = 5;
    repository.failedCount = 2;
    repository.publishedCount = 11;

    OutboxPublisherMetrics metrics = new OutboxPublisherMetrics();
    metrics.recordSuccess(8);
    metrics.recordSuccess(12);
    metrics.recordFailure();

    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = repository;
    resource.outboxPublisher = new StubOutboxPublisher();
    resource.metrics = metrics;

    Map<String, Object> stats = resource.stats();

    assertEquals(5L, stats.get("pendingCount"));
    assertEquals(2L, stats.get("failedCount"));
    assertEquals(11L, stats.get("publishedCount"));
    assertEquals(2L, stats.get("relayPublishedCount"));
    assertEquals(1L, stats.get("relayFailureCount"));
    assertEquals(10L, stats.get("relayAveragePublishLatencyMs"));
  }

  @Test
  public void testPublishNowShouldReturnProcessedCount() {
    StubOutboxPublisher publisher = new StubOutboxPublisher();
    publisher.processed = 4;

    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = new FakeOutboxRepository();
    resource.outboxPublisher = publisher;
    resource.metrics = new OutboxPublisherMetrics();

    Map<String, Object> response = resource.publishNow();

    assertEquals(4, response.get("processed"));
  }

  @Test
  public void testReplayShouldFailWhenRequiredParamsMissing() {
    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = new FakeOutboxRepository();
    resource.outboxPublisher = new StubOutboxPublisher();
    resource.metrics = new OutboxPublisherMetrics();

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.replay("", null, null, false));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("aggregateId, from, and to are required query params", ex.getMessage());
  }

  @Test
  public void testReplayShouldFailWhenDateFormatIsInvalid() {
    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = new FakeOutboxRepository();
    resource.outboxPublisher = new StubOutboxPublisher();
    resource.metrics = new OutboxPublisherMetrics();

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replay("AGG-1", "invalid", "2026-02-24T10:00:00", false));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Invalid from/to datetime format. Use ISO-8601 LocalDateTime", ex.getMessage());
  }

  @Test
  public void testReplayShouldFailWhenToIsBeforeFrom() {
    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = new FakeOutboxRepository();
    resource.outboxPublisher = new StubOutboxPublisher();
    resource.metrics = new OutboxPublisherMetrics();

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replay("AGG-1", "2026-02-24T11:00:00", "2026-02-24T10:00:00", false));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("to must be greater than or equal to from", ex.getMessage());
  }

  @Test
  public void testReplayShouldResetMetricsAndReturnAffectedCount() {
    FakeOutboxRepository repository = new FakeOutboxRepository();
    repository.replayResult = 9;

    OutboxPublisherMetrics metrics = new OutboxPublisherMetrics();
    metrics.recordSuccess(10);
    metrics.recordFailure();

    OutboxAdminResource resource = new OutboxAdminResource();
    resource.outboxMessageRepository = repository;
    resource.outboxPublisher = new StubOutboxPublisher();
    resource.metrics = metrics;

    Map<String, Object> response =
        resource.replay("AGG-9", "2026-02-24T10:00:00", "2026-02-24T11:00:00", true);

    assertEquals(9, response.get("affected"));
    assertEquals("AGG-9", repository.replayAggregateId);
    assertEquals(LocalDateTime.parse("2026-02-24T10:00:00"), repository.replayFrom);
    assertEquals(LocalDateTime.parse("2026-02-24T11:00:00"), repository.replayTo);
    assertEquals(0, metrics.getPublishedCount());
    assertEquals(0, metrics.getFailedCount());
  }
}

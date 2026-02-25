package com.fulfilment.application.monolith.stores.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OutboxPublisherMetricsTest {

  @Test
  public void testRecordSuccessAndFailureShouldUpdateCountersAndAverage() {
    OutboxPublisherMetrics metrics = new OutboxPublisherMetrics();

    metrics.recordSuccess(10);
    metrics.recordSuccess(14);
    metrics.recordFailure();

    assertEquals(2, metrics.getPublishedCount());
    assertEquals(1, metrics.getFailedCount());
    assertEquals(12, metrics.getAveragePublishLatencyMs());
  }

  @Test
  public void testGetAveragePublishLatencyShouldReturnZeroWhenNoSuccess() {
    OutboxPublisherMetrics metrics = new OutboxPublisherMetrics();

    assertEquals(0, metrics.getAveragePublishLatencyMs());
  }

  @Test
  public void testResetShouldClearAllCounters() {
    OutboxPublisherMetrics metrics = new OutboxPublisherMetrics();
    metrics.recordSuccess(7);
    metrics.recordFailure();

    metrics.reset();

    assertEquals(0, metrics.getPublishedCount());
    assertEquals(0, metrics.getFailedCount());
    assertEquals(0, metrics.getAveragePublishLatencyMs());
  }
}

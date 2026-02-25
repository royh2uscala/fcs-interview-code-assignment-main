package com.fulfilment.application.monolith.stores.outbox;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class OutboxPublisherMetrics {

  private final AtomicLong publishedCount = new AtomicLong();
  private final AtomicLong failedCount = new AtomicLong();
  private final AtomicLong totalPublishLatencyMs = new AtomicLong();

  public void recordSuccess(long publishLatencyMs) {
    publishedCount.incrementAndGet();
    totalPublishLatencyMs.addAndGet(publishLatencyMs);
  }

  public void recordFailure() {
    failedCount.incrementAndGet();
  }

  public long getPublishedCount() {
    return publishedCount.get();
  }

  public long getFailedCount() {
    return failedCount.get();
  }

  public long getAveragePublishLatencyMs() {
    long currentPublished = publishedCount.get();
    if (currentPublished == 0) {
      return 0;
    }
    return totalPublishLatencyMs.get() / currentPublished;
  }

  public void reset() {
    publishedCount.set(0);
    failedCount.set(0);
    totalPublishLatencyMs.set(0);
  }
}

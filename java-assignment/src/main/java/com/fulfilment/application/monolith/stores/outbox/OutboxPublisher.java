package com.fulfilment.application.monolith.stores.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OutboxPublisher {

  private static final Logger LOGGER = Logger.getLogger(OutboxPublisher.class);
  private static final int BATCH_SIZE = 100;

  @Inject OutboxMessageRepository outboxMessageRepository;
  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;
  @Inject ObjectMapper objectMapper;
  @Inject OutboxPublisherMetrics metrics;

  @ConfigProperty(name = "outbox.publisher.interval-seconds", defaultValue = "30")
  long intervalSeconds;

  @ConfigProperty(name = "outbox.publisher.initial-delay-seconds", defaultValue = "10")
  long initialDelaySeconds;

  @ConfigProperty(name = "outbox.publisher.enabled", defaultValue = "true")
  boolean enabled;

  private ScheduledExecutorService scheduler;

  @PostConstruct
  void start() {
    if (!enabled) {
      LOGGER.info("Outbox background scheduler is disabled by configuration.");
      return;
    }
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            publishPending();
          } catch (Exception ex) {
            LOGGER.error("Unexpected outbox publisher failure", ex);
          }
        },
        initialDelaySeconds,
        intervalSeconds,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  void shutdown() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

  public int publishPending() {
    List<OutboxMessage> pending = outboxMessageRepository.listPending(BATCH_SIZE, LocalDateTime.now());
    for (OutboxMessage message : pending) {
      long start = System.currentTimeMillis();
      try {
        StoreChangedEventPayload payload =
            objectMapper.readValue(message.payloadJson, StoreChangedEventPayload.class);
        legacyStoreManagerGateway.publishStoreEvent(
            message.eventId,
            idempotencyKeyFor(message),
            message.eventType,
            message.schemaVersion,
            message.correlationId,
            payload);
        outboxMessageRepository.markPublished(message, LocalDateTime.now());
        metrics.recordSuccess(System.currentTimeMillis() - start);
      } catch (Exception ex) {
        LOGGER.errorf(ex, "Failed to publish outbox message id=%s eventId=%s", message.id, message.eventId);
        outboxMessageRepository.markFailed(message, ex.getMessage(), nextAttemptAt(message.attempts));
        metrics.recordFailure();
      }
    }
    return pending.size();
  }

  private LocalDateTime nextAttemptAt(int attempts) {
    int backoffSeconds = Math.min(60, Math.max(1, (int) Math.pow(2, attempts)));
    return LocalDateTime.now().plusSeconds(backoffSeconds);
  }

  private String idempotencyKeyFor(OutboxMessage message) {
    return message.aggregateType + ":" + message.aggregateId + ":" + message.eventType + ":" + message.eventId;
  }
}

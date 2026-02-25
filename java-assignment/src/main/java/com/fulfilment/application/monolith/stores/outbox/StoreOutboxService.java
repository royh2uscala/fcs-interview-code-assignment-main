package com.fulfilment.application.monolith.stores.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.common.CorrelationIdContext;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class StoreOutboxService {

  public static final int EVENT_SCHEMA_VERSION = 1;
  public static final String AGGREGATE_TYPE = "Store";

  @Inject OutboxMessageRepository outboxMessageRepository;
  @Inject ObjectMapper objectMapper;
  @Inject CorrelationIdContext correlationIdContext;

  public void enqueueStoreChanged(String eventType, Store store) {
    OutboxMessage message = new OutboxMessage();
    message.eventId = UUID.randomUUID().toString();
    message.aggregateType = AGGREGATE_TYPE;
    message.aggregateId = String.valueOf(store.id);
    message.aggregateVersion = null;
    message.eventType = eventType;
    message.schemaVersion = EVENT_SCHEMA_VERSION;
    message.correlationId = correlationIdContext.getCorrelationId();
    message.payloadJson = payloadAsJson(new StoreChangedEventPayload(store.id, store.name, store.quantityProductsInStock));
    message.createdAt = LocalDateTime.now();
    message.publishedAt = null;
    message.attempts = 0;
    message.lastError = null;
    message.nextAttemptAt = LocalDateTime.now();
    outboxMessageRepository.create(message);
  }

  private String payloadAsJson(StoreChangedEventPayload payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize store event payload", ex);
    }
  }
}

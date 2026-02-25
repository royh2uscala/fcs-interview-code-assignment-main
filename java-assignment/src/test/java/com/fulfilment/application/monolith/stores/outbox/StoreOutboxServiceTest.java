package com.fulfilment.application.monolith.stores.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.common.CorrelationIdContext;
import com.fulfilment.application.monolith.stores.Store;
import org.junit.jupiter.api.Test;

public class StoreOutboxServiceTest {

  private static class CapturingOutboxRepository extends OutboxMessageRepository {
    OutboxMessage created;

    @Override
    public void create(OutboxMessage message) {
      this.created = message;
    }
  }

  @Test
  public void testEnqueueStoreChangedShouldCreateOutboxMessage() throws Exception {
    CapturingOutboxRepository repository = new CapturingOutboxRepository();
    StoreOutboxService service = new StoreOutboxService();
    service.outboxMessageRepository = repository;
    service.objectMapper = new ObjectMapper();
    CorrelationIdContext correlationIdContext = new CorrelationIdContext();
    correlationIdContext.setCorrelationId("corr-123");
    service.correlationIdContext = correlationIdContext;

    Store store = new Store();
    store.id = 21L;
    store.name = "Store-A";
    store.quantityProductsInStock = 9;

    service.enqueueStoreChanged("StoreUpdated", store);

    assertNotNull(repository.created);
    assertNotNull(repository.created.eventId);
    assertEquals(StoreOutboxService.AGGREGATE_TYPE, repository.created.aggregateType);
    assertEquals("21", repository.created.aggregateId);
    assertEquals("StoreUpdated", repository.created.eventType);
    assertEquals(StoreOutboxService.EVENT_SCHEMA_VERSION, repository.created.schemaVersion);
    assertEquals("corr-123", repository.created.correlationId);
    assertNotNull(repository.created.createdAt);
    assertNull(repository.created.publishedAt);
    assertEquals(0, repository.created.attempts);
    assertNotNull(repository.created.nextAttemptAt);
    assertNull(repository.created.lastError);

    StoreChangedEventPayload payload =
        new ObjectMapper().readValue(repository.created.payloadJson, StoreChangedEventPayload.class);
    assertEquals(21L, payload.storeId);
    assertEquals("Store-A", payload.name);
    assertEquals(9, payload.quantityProductsInStock);
  }

  @Test
  public void testEnqueueStoreChangedShouldThrowWhenPayloadSerializationFails() {
    CapturingOutboxRepository repository = new CapturingOutboxRepository();
    StoreOutboxService service = new StoreOutboxService();
    service.outboxMessageRepository = repository;
    service.objectMapper =
        new ObjectMapper() {
          @Override
          public String writeValueAsString(Object value) throws JsonProcessingException {
            throw new JsonProcessingException("serialization failed") {};
          }
        };
    service.correlationIdContext = new CorrelationIdContext();

    Store store = new Store();
    store.id = 22L;
    store.name = "Store-B";
    store.quantityProductsInStock = 4;

    assertThrows(IllegalStateException.class, () -> service.enqueueStoreChanged("StoreCreated", store));
    assertNull(repository.created);
  }
}

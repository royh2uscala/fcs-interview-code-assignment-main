package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.stores.outbox.StoreOutboxService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class StoreResourceValidationTest {

  private static class PersistTrackingStore extends Store {
    boolean persisted;
  }

  private static class CapturingOutboxService extends StoreOutboxService {
    String capturedEventType;
    Store capturedStore;

    @Override
    public void enqueueStoreChanged(String eventType, Store store) {
      this.capturedEventType = eventType;
      this.capturedStore = store;
    }
  }

  private static class PersistTrackingStoreGateway extends StoreGateway {
    Store capturedStore;

    @Override
    public void persist(Store store) {
      this.capturedStore = store;
      ((PersistTrackingStore) store).persisted = true;
    }
  }

  @Test
  public void testCreateShouldFailWhenIdIsPreset() {
    StoreResource storeResource = new StoreResource();
    storeResource.storeGateway = new PersistTrackingStoreGateway();
    Store store = new Store();
    store.id = 1L;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> storeResource.create(store));
    assertEquals(422, ex.getResponse().getStatus());
    assertEquals("Id was invalidly set on request.", ex.getMessage());
  }

  @Test
  public void testCreateShouldPersistAndEnqueueStoreCreatedEvent() {
    StoreResource storeResource = new StoreResource();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    PersistTrackingStoreGateway storeGateway = new PersistTrackingStoreGateway();
    storeResource.storeOutboxService = outboxService;
    storeResource.storeGateway = storeGateway;

    PersistTrackingStore store = new PersistTrackingStore();
    store.name = "Store-1";
    store.quantityProductsInStock = 12;

    Response response = storeResource.create(store);

    assertEquals(201, response.getStatus());
    assertSame(store, response.getEntity());
    assertTrue(store.persisted);
    assertEquals("StoreCreated", outboxService.capturedEventType);
    assertSame(store, outboxService.capturedStore);
    assertSame(store, storeGateway.capturedStore);
  }

  @Test
  public void testUpdateShouldFailWhenNameMissing() {
    StoreResource storeResource = new StoreResource();
    storeResource.storeGateway = new PersistTrackingStoreGateway();
    Store store = new Store();
    store.name = null;

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> storeResource.update(100L, store));
    assertEquals(422, ex.getResponse().getStatus());
    assertEquals("Store Name was not set on request.", ex.getMessage());
  }
}

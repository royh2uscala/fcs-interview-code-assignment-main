package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.stores.outbox.StoreOutboxService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class StoreResourceUnitTest {

  private static class TestStoreGateway extends StoreGateway {
    private final Map<Long, Store> stores = new HashMap<>();
    private Store persistedStore;
    private Store deletedStore;
    private long nextId = 100L;

    @Override
    public List<Store> listAllByName() {
      return stores.values().stream()
          .sorted(Comparator.comparing(store -> store.name))
          .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public Store findById(Long id) {
      return stores.get(id);
    }

    @Override
    public void persist(Store store) {
      persistedStore = store;
      if (store.id == null) {
        store.id = nextId++;
      }
      stores.put(store.id, store);
    }

    @Override
    public void delete(Store store) {
      deletedStore = store;
      stores.remove(store.id);
    }

    void put(Store store) {
      stores.put(store.id, store);
    }
  }

  private static class CapturingOutboxService extends StoreOutboxService {
    String capturedEventType;
    Store capturedStore;

    @Override
    public void enqueueStoreChanged(String eventType, Store store) {
      capturedEventType = eventType;
      capturedStore = store;
    }
  }

  @Test
  public void testGetShouldReturnStoresFromGateway() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);

    Store beta = store(2L, "Beta", 4);
    Store alpha = store(1L, "Alpha", 3);
    storeGateway.put(beta);
    storeGateway.put(alpha);

    List<Store> result = resource.get();

    assertEquals(2, result.size());
    assertEquals("Alpha", result.get(0).name);
    assertEquals("Beta", result.get(1).name);
  }

  @Test
  public void testGetSingleShouldThrow404WhenMissing() {
    StoreResource resource = createResource(new TestStoreGateway(), new CapturingOutboxService());

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(99L));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Store with id of 99 does not exist.", ex.getMessage());
  }

  @Test
  public void testGetSingleShouldReturnExistingStore() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    Store existing = store(50L, "TONSTAD", 7);
    storeGateway.put(existing);
    StoreResource resource = createResource(storeGateway, new CapturingOutboxService());

    Store result = resource.getSingle(50L);

    assertSame(existing, result);
  }

  @Test
  public void testUpdateShouldThrow404WhenMissing() {
    StoreResource resource = createResource(new TestStoreGateway(), new CapturingOutboxService());
    Store updatePayload = new Store();
    updatePayload.name = "Updated";
    updatePayload.quantityProductsInStock = 12;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(10L, updatePayload));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testUpdateShouldMutateExistingStoreAndEnqueueEvent() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);

    Store existing = store(10L, "Original", 5);
    storeGateway.put(existing);
    Store updatePayload = new Store();
    updatePayload.name = "Updated";
    updatePayload.quantityProductsInStock = 42;

    Store result = resource.update(10L, updatePayload);

    assertSame(existing, result);
    assertEquals("Updated", existing.name);
    assertEquals(42, existing.quantityProductsInStock);
    assertEquals("StoreUpdated", outboxService.capturedEventType);
    assertSame(existing, outboxService.capturedStore);
  }

  @Test
  public void testPatchShouldThrow404WhenMissing() {
    StoreResource resource = createResource(new TestStoreGateway(), new CapturingOutboxService());

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.patch(88L, new Store()));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testPatchShouldUpdateNameAndIgnoreNegativeStock() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);

    Store existing = store(20L, "Before", 15);
    storeGateway.put(existing);
    Store patchPayload = new Store();
    patchPayload.name = "After";
    patchPayload.quantityProductsInStock = -1;

    Store result = resource.patch(20L, patchPayload);

    assertSame(existing, result);
    assertEquals("After", existing.name);
    assertEquals(15, existing.quantityProductsInStock);
    assertEquals("StorePatched", outboxService.capturedEventType);
    assertSame(existing, outboxService.capturedStore);
  }

  @Test
  public void testPatchShouldUpdateStockWhenNonNegativeAndKeepNameWhenNull() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);

    Store existing = store(25L, "StableName", 5);
    storeGateway.put(existing);
    Store patchPayload = new Store();
    patchPayload.name = null;
    patchPayload.quantityProductsInStock = 12;

    resource.patch(25L, patchPayload);

    assertEquals("StableName", existing.name);
    assertEquals(12, existing.quantityProductsInStock);
    assertEquals("StorePatched", outboxService.capturedEventType);
    assertSame(existing, outboxService.capturedStore);
  }

  @Test
  public void testDeleteShouldThrow404WhenMissing() {
    StoreResource resource = createResource(new TestStoreGateway(), new CapturingOutboxService());

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.delete(404L));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testDeleteShouldReturnNoContentAndDeleteStore() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);
    Store existing = store(31L, "ToDelete", 2);
    storeGateway.put(existing);

    Response response = resource.delete(31L);

    assertEquals(204, response.getStatus());
    assertSame(existing, storeGateway.deletedStore);
    assertEquals("StoreDeleted", outboxService.capturedEventType);
    assertSame(existing, outboxService.capturedStore);
    assertFalse(storeGateway.stores.containsKey(31L));
  }

  @Test
  public void testCreateShouldPersistViaGateway() {
    TestStoreGateway storeGateway = new TestStoreGateway();
    CapturingOutboxService outboxService = new CapturingOutboxService();
    StoreResource resource = createResource(storeGateway, outboxService);
    Store newStore = new Store();
    newStore.name = "CreateMe";
    newStore.quantityProductsInStock = 22;

    Response response = resource.create(newStore);

    assertEquals(201, response.getStatus());
    assertTrue(newStore.id != null);
    assertSame(newStore, storeGateway.persistedStore);
    assertEquals("StoreCreated", outboxService.capturedEventType);
    assertSame(newStore, outboxService.capturedStore);
  }

  private StoreResource createResource(
      TestStoreGateway storeGateway, CapturingOutboxService outboxService) {
    StoreResource resource = new StoreResource();
    resource.storeGateway = storeGateway;
    resource.storeOutboxService = outboxService;
    return resource;
  }

  private Store store(Long id, String name, int stock) {
    Store store = new Store();
    store.id = id;
    store.name = name;
    store.quantityProductsInStock = stock;
    return store;
  }
}

package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class FulfillmentAssignmentServiceTest {

  private static class FakeRepository extends FulfillmentAssignmentRepository {
    Store store;
    Product product;
    DbWarehouse warehouse;
    FulfillmentAssignment existingAssignment;
    long countStoreAndProduct;
    long countStore;
    long countWarehouse;
    FulfillmentAssignment persistedAssignment;
    List<FulfillmentAssignment> assignments = List.of();

    @Override
    public EntityManager getEntityManager() {
      return (EntityManager)
          Proxy.newProxyInstance(
              EntityManager.class.getClassLoader(),
              new Class<?>[] {EntityManager.class},
              (proxy, method, args) -> {
                if ("find".equals(method.getName())) {
                  Class<?> entityClass = (Class<?>) args[0];
                  if (Store.class.equals(entityClass)) {
                    return store;
                  }
                  if (Product.class.equals(entityClass)) {
                    return product;
                  }
                  return null;
                }
                if ("createQuery".equals(method.getName())) {
                  return typedQueryForWarehouse();
                }
                return defaultValue(method.getReturnType());
              });
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<DbWarehouse> typedQueryForWarehouse() {
      return (TypedQuery<DbWarehouse>)
          Proxy.newProxyInstance(
              TypedQuery.class.getClassLoader(),
              new Class<?>[] {TypedQuery.class},
              (proxy, method, args) -> {
                if ("setParameter".equals(method.getName()) || "setLockMode".equals(method.getName())) {
                  return proxy;
                }
                if ("getResultStream".equals(method.getName())) {
                  return warehouse == null ? Stream.empty() : Stream.of(warehouse);
                }
                return defaultValue(method.getReturnType());
              });
    }

    @Override
    public PanacheQuery<FulfillmentAssignment> find(String query, Object... params) {
      return panacheQueryWith(existingAssignment);
    }

    @Override
    public long countDistinctWarehousesByStoreAndProduct(Long storeId, Long productId) {
      return countStoreAndProduct;
    }

    @Override
    public long countDistinctWarehousesByStore(Long storeId) {
      return countStore;
    }

    @Override
    public long countDistinctProductsByWarehouse(String warehouseBusinessUnitCode) {
      return countWarehouse;
    }

    @Override
    public void persist(FulfillmentAssignment entity) {
      persistedAssignment = entity;
    }

    @Override
    public List<FulfillmentAssignment> listAll() {
      return assignments;
    }
  }

  @SuppressWarnings("unchecked")
  private static PanacheQuery<FulfillmentAssignment> panacheQueryWith(FulfillmentAssignment assignment) {
    return (PanacheQuery<FulfillmentAssignment>)
        Proxy.newProxyInstance(
            PanacheQuery.class.getClassLoader(),
            new Class<?>[] {PanacheQuery.class},
            (proxy, method, args) -> {
              if ("firstResult".equals(method.getName())) {
                return assignment;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static Object defaultValue(Class<?> returnType) {
    if (!returnType.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(returnType)) {
      return false;
    }
    if (char.class.equals(returnType)) {
      return '\0';
    }
    if (byte.class.equals(returnType) || short.class.equals(returnType) || int.class.equals(returnType)) {
      return 0;
    }
    if (long.class.equals(returnType)) {
      return 0L;
    }
    if (float.class.equals(returnType)) {
      return 0f;
    }
    if (double.class.equals(returnType)) {
      return 0d;
    }
    return null;
  }

  @Test
  public void testAssignShouldRejectInvalidInput() {
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.assign(1L, 2L, " "));

    assertEquals("storeId, productId and warehouseBusinessUnitCode are required", ex.getMessage());
  }

  @Test
  public void testAssignShouldFailWhenStoreDoesNotExist() {
    FakeRepository repository = new FakeRepository();
    repository.store = null;
    repository.product = new Product("P");
    repository.warehouse = new DbWarehouse();
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.assign(1L, 2L, "MWH.001"));

    assertEquals("Store does not exist", ex.getMessage());
  }

  @Test
  public void testAssignShouldFailWhenProductDoesNotExist() {
    FakeRepository repository = new FakeRepository();
    repository.store = new Store("S");
    repository.product = null;
    repository.warehouse = new DbWarehouse();
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.assign(1L, 2L, "MWH.001"));

    assertEquals("Product does not exist", ex.getMessage());
  }

  @Test
  public void testAssignShouldFailWhenWarehouseDoesNotExist() {
    FakeRepository repository = new FakeRepository();
    repository.store = new Store("S");
    repository.product = new Product("P");
    repository.warehouse = null;
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.assign(1L, 2L, "MWH.404"));

    assertEquals("Warehouse does not exist or is archived", ex.getMessage());
  }

  @Test
  public void testAssignShouldReturnExistingAssignmentWhenAlreadyPresent() {
    FakeRepository repository = baseRepositoryForHappyPath();
    FulfillmentAssignment existing = new FulfillmentAssignment();
    existing.id = 77L;
    repository.existingAssignment = existing;
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    FulfillmentAssignment result = service.assign(1L, 2L, "MWH.001");

    assertSame(existing, result);
    assertNull(repository.persistedAssignment);
  }

  @Test
  public void testAssignShouldFailWhenStoreAndProductAlreadyUseTwoWarehouses() {
    FakeRepository repository = baseRepositoryForHappyPath();
    repository.countStoreAndProduct = 2;
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.assign(1L, 2L, "MWH.001"));

    assertTrue(ex.getMessage().contains("maximum of 2"));
  }

  @Test
  public void testAssignShouldFailWhenStoreAlreadyUsesThreeWarehouses() {
    FakeRepository repository = baseRepositoryForHappyPath();
    repository.countStore = 3;
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.assign(1L, 2L, "MWH.001"));

    assertTrue(ex.getMessage().contains("maximum of 3"));
  }

  @Test
  public void testAssignShouldFailWhenWarehouseAlreadyStoresFiveProducts() {
    FakeRepository repository = baseRepositoryForHappyPath();
    repository.countWarehouse = 5;
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.assign(1L, 2L, "MWH.001"));

    assertTrue(ex.getMessage().contains("maximally 5"));
  }

  @Test
  public void testAssignShouldPersistAssignmentWhenValid() {
    FakeRepository repository = baseRepositoryForHappyPath();
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    FulfillmentAssignment result = service.assign(10L, 20L, "MWH.001");

    assertNotNull(result);
    assertSame(result, repository.persistedAssignment);
    assertEquals(10L, result.storeId);
    assertEquals(20L, result.productId);
    assertEquals("MWH.001", result.warehouseBusinessUnitCode);
    assertNotNull(result.createdAt);
  }

  @Test
  public void testListAllShouldDelegateToRepository() {
    FakeRepository repository = new FakeRepository();
    FulfillmentAssignment a = new FulfillmentAssignment();
    a.id = 1L;
    repository.assignments = List.of(a);
    FulfillmentAssignmentService service = new FulfillmentAssignmentService();
    service.repository = repository;

    List<FulfillmentAssignment> result = service.listAll();

    assertEquals(1, result.size());
    assertSame(a, result.get(0));
  }

  private static FakeRepository baseRepositoryForHappyPath() {
    FakeRepository repository = new FakeRepository();
    repository.store = new Store("S");
    repository.store.id = 10L;
    repository.product = new Product("P");
    repository.product.id = 20L;
    repository.warehouse = new DbWarehouse();
    repository.warehouse.businessUnitCode = "MWH.001";
    repository.countStoreAndProduct = 1;
    repository.countStore = 1;
    repository.countWarehouse = 1;
    return repository;
  }
}

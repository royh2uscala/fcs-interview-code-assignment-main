package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class FulfillmentAssignmentRepositoryTest {

  private static class QueryCapture {
    String jpql;
    final Map<String, Object> namedParameters = new HashMap<>();
    Long singleResult = 0L;
  }

  private static class TestRepository extends FulfillmentAssignmentRepository {
    final QueryCapture capture = new QueryCapture();

    @Override
    public EntityManager getEntityManager() {
      return (EntityManager)
          Proxy.newProxyInstance(
              EntityManager.class.getClassLoader(),
              new Class<?>[] {EntityManager.class},
              (proxy, method, args) -> {
                if ("createQuery".equals(method.getName())) {
                  capture.jpql = (String) args[0];
                  return typedQuery();
                }
                return null;
              });
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<Long> typedQuery() {
      return (TypedQuery<Long>)
          Proxy.newProxyInstance(
              TypedQuery.class.getClassLoader(),
              new Class<?>[] {TypedQuery.class},
              (proxy, method, args) -> {
                if ("setParameter".equals(method.getName())) {
                  capture.namedParameters.put((String) args[0], args[1]);
                  return proxy;
                }
                if ("getSingleResult".equals(method.getName())) {
                  return capture.singleResult;
                }
                return null;
              });
    }
  }

  @Test
  public void testCountDistinctWarehousesByStoreAndProduct() {
    TestRepository repository = new TestRepository();
    repository.capture.singleResult = 2L;

    long result = repository.countDistinctWarehousesByStoreAndProduct(10L, 20L);

    assertEquals(2L, result);
    assertEquals(10L, repository.capture.namedParameters.get("storeId"));
    assertEquals(20L, repository.capture.namedParameters.get("productId"));
    assertEquals(true, repository.capture.jpql.contains("count(distinct f.warehouseBusinessUnitCode)"));
  }

  @Test
  public void testCountDistinctWarehousesByStore() {
    TestRepository repository = new TestRepository();
    repository.capture.singleResult = 3L;

    long result = repository.countDistinctWarehousesByStore(11L);

    assertEquals(3L, result);
    assertEquals(11L, repository.capture.namedParameters.get("storeId"));
    assertEquals(true, repository.capture.jpql.contains("where f.storeId = :storeId"));
  }

  @Test
  public void testCountDistinctProductsByWarehouse() {
    TestRepository repository = new TestRepository();
    repository.capture.singleResult = 4L;

    long result = repository.countDistinctProductsByWarehouse("MWH.023");

    assertEquals(4L, result);
    assertEquals("MWH.023", repository.capture.namedParameters.get("warehouseBusinessUnitCode"));
    assertEquals(true, repository.capture.jpql.contains("count(distinct f.productId)"));
  }
}

package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class WarehouseRepositoryTest {

  private static class TestWarehouseRepository extends WarehouseRepository {
    List<DbWarehouse> findListResult = List.of();
    DbWarehouse findFirstResult;
    List<DbWarehouse> listAllResult = List.of();
    DbWarehouse findByIdResult;
    long countResult;
    Long sumCapacityResult = 0L;
    Long nextPersistedId = 1000L;

    String lastFindQuery;
    Object[] lastFindParams;
    String lastCountQuery;
    Object[] lastCountParams;
    String lastDeleteQuery;
    Object[] lastDeleteParams;
    DbWarehouse deletedEntity;
    DbWarehouse persistedEntity;
    int updateRows;
    boolean deleteAllCalled;
    boolean flushCalled;
    final List<Long> advisoryLockKeys = new ArrayList<>();

    @Override
    public PanacheQuery<DbWarehouse> find(String query, Object... params) {
      lastFindQuery = query;
      lastFindParams = params;
      return panacheQuery(findListResult, findFirstResult);
    }

    @SuppressWarnings("unchecked")
    private PanacheQuery<DbWarehouse> panacheQuery(List<DbWarehouse> listResult, DbWarehouse firstResult) {
      return (PanacheQuery<DbWarehouse>)
          Proxy.newProxyInstance(
              PanacheQuery.class.getClassLoader(),
              new Class<?>[] {PanacheQuery.class},
              (proxy, method, args) -> {
                if ("list".equals(method.getName())) {
                  return listResult;
                }
                if ("firstResult".equals(method.getName())) {
                  return firstResult;
                }
                if ("page".equals(method.getName())) {
                  return proxy;
                }
                return defaultValue(method.getReturnType());
              });
    }

    @Override
    public List<DbWarehouse> listAll() {
      return listAllResult;
    }

    @Override
    public DbWarehouse findById(Long id) {
      return findByIdResult;
    }

    @Override
    public void persist(DbWarehouse entity) {
      persistedEntity = entity;
      if (entity.id == null) {
        entity.id = nextPersistedId;
      }
    }

    @Override
    public void delete(DbWarehouse entity) {
      deletedEntity = entity;
    }

    @Override
    public long delete(String query, Object... params) {
      lastDeleteQuery = query;
      lastDeleteParams = params;
      return updateRows;
    }

    @Override
    public long count(String query, Object... params) {
      lastCountQuery = query;
      lastCountParams = params;
      return countResult;
    }

    @Override
    public long deleteAll() {
      deleteAllCalled = true;
      return 0;
    }

    @Override
    public EntityManager getEntityManager() {
      return (EntityManager)
          Proxy.newProxyInstance(
              EntityManager.class.getClassLoader(),
              new Class<?>[] {EntityManager.class},
              (proxy, method, args) -> {
                if ("createQuery".equals(method.getName())) {
                  return typedQueryForCapacity();
                }
                if ("createNativeQuery".equals(method.getName())) {
                  return nativeQueryForLocks();
                }
                if ("flush".equals(method.getName())) {
                  flushCalled = true;
                  return null;
                }
                return defaultValue(method.getReturnType());
              });
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<Long> typedQueryForCapacity() {
      return (TypedQuery<Long>)
          Proxy.newProxyInstance(
              TypedQuery.class.getClassLoader(),
              new Class<?>[] {TypedQuery.class},
              (proxy, method, args) -> {
                if ("setParameter".equals(method.getName())) {
                  return proxy;
                }
                if ("getSingleResult".equals(method.getName())) {
                  return sumCapacityResult;
                }
                return defaultValue(method.getReturnType());
              });
    }

    private Query nativeQueryForLocks() {
      return (Query)
          Proxy.newProxyInstance(
              Query.class.getClassLoader(),
              new Class<?>[] {Query.class},
              (proxy, method, args) -> {
                if ("setParameter".equals(method.getName())) {
                  advisoryLockKeys.add((Long) args[1]);
                  return proxy;
                }
                if ("getSingleResult".equals(method.getName())) {
                  return 1L;
                }
                return defaultValue(method.getReturnType());
              });
    }
  }

  @Test
  public void testGetAllShouldMapActiveEntities() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    DbWarehouse first = dbWarehouse(1L, "BU-1", "ZWOLLE-001", 100, 10);
    DbWarehouse second = dbWarehouse(2L, "BU-2", "AMSTERDAM-001", 120, 20);
    repository.findListResult = List.of(first, second);

    List<Warehouse> result = repository.getAll();

    assertEquals(2, result.size());
    assertEquals("BU-1", result.get(0).businessUnitCode);
    assertEquals("BU-2", result.get(1).businessUnitCode);
    assertEquals("archivedAt is null", repository.lastFindQuery);
  }

  @Test
  public void testCreateShouldInitializeDefaultsAndBackPropagateEntityFields() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU-NEW";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 25;
    warehouse.createdAt = null;

    repository.create(warehouse);

    assertNotNull(repository.persistedEntity);
    assertEquals("BU-NEW", repository.persistedEntity.businessUnitCode);
    assertNotNull(repository.persistedEntity.createdAt);
    assertNull(repository.persistedEntity.archivedAt);
    assertEquals(1000L, warehouse.id);
    assertSame(repository.persistedEntity.createdAt, warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  public void testCreateShouldKeepProvidedCreatedAt() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "BU-TS";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 25;
    warehouse.createdAt = LocalDateTime.of(2026, 2, 25, 7, 30);

    repository.create(warehouse);

    assertSame(warehouse.createdAt, repository.persistedEntity.createdAt);
  }

  @Test
  public void testUpdateShouldUseIdWhenProvided() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    DbWarehouse existing = dbWarehouse(10L, "BU-OLD", "OLD", 10, 1);
    repository.findByIdResult = existing;

    Warehouse update = new Warehouse();
    update.id = 10L;
    update.businessUnitCode = "BU-NEW";
    update.location = "ZWOLLE-001";
    update.capacity = 200;
    update.stock = 80;
    update.createdAt = LocalDateTime.of(2026, 2, 24, 10, 0);
    update.archivedAt = LocalDateTime.of(2026, 2, 25, 10, 0);

    repository.update(update);

    assertEquals("BU-NEW", existing.businessUnitCode);
    assertEquals("ZWOLLE-001", existing.location);
    assertEquals(200, existing.capacity);
    assertEquals(80, existing.stock);
    assertSame(update.createdAt, existing.createdAt);
    assertSame(update.archivedAt, existing.archivedAt);
  }

  @Test
  public void testUpdateShouldResolveByBusinessUnitWhenIdMissing() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    DbWarehouse existing = dbWarehouse(22L, "BU-22", "OLD", 10, 1);
    repository.findFirstResult = existing;

    Warehouse update = new Warehouse();
    update.id = null;
    update.businessUnitCode = "BU-22";
    update.location = "NEW";
    update.capacity = 11;
    update.stock = 2;
    update.createdAt = LocalDateTime.of(2026, 2, 24, 11, 0);
    update.archivedAt = null;

    repository.update(update);

    assertEquals("businessUnitCode = ?1 and archivedAt is null", repository.lastFindQuery);
    assertEquals("NEW", existing.location);
    assertEquals(11, existing.capacity);
  }

  @Test
  public void testUpdateShouldThrowWhenWarehouseMissing() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    Warehouse update = new Warehouse();
    update.id = 44L;

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> repository.update(update));

    assertEquals("Warehouse not found for update", ex.getMessage());
  }

  @Test
  public void testRemoveShouldDeleteWhenEntityFound() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    DbWarehouse existing = dbWarehouse(31L, "BU-31", "LOC", 10, 1);
    repository.findByIdResult = existing;
    Warehouse warehouse = new Warehouse();
    warehouse.id = 31L;

    repository.remove(warehouse);

    assertSame(existing, repository.deletedEntity);
  }

  @Test
  public void testRemoveShouldDoNothingWhenEntityMissing() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    Warehouse warehouse = new Warehouse();
    warehouse.id = 99L;

    repository.remove(warehouse);

    assertNull(repository.deletedEntity);
  }

  @Test
  public void testFindByBusinessUnitCodeShouldMapEntity() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    repository.findFirstResult = dbWarehouse(5L, "BU-5", "LOC", 10, 1);

    Warehouse result = repository.findByBusinessUnitCode("BU-5");

    assertNotNull(result);
    assertEquals(5L, result.id);
    assertEquals("businessUnitCode = ?1 and archivedAt is null", repository.lastFindQuery);
    assertEquals("BU-5", repository.lastFindParams[0]);
  }

  @Test
  public void testFindByIdAsDomainShouldReturnNullWhenMissing() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    repository.findFirstResult = null;

    Warehouse result = repository.findByIdAsDomain(50L);

    assertNull(result);
    assertEquals("id = ?1 and archivedAt is null", repository.lastFindQuery);
  }

  @Test
  public void testCountMethodsShouldDelegateToCountQuery() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    repository.countResult = 7L;

    long byLocation = repository.countActiveByLocation("ZWOLLE-001");
    long byBusinessUnitCode = repository.countActiveByBusinessUnitCode("BU-7");

    assertEquals(7L, byLocation);
    assertEquals(7L, byBusinessUnitCode);
    assertEquals("businessUnitCode = ?1 and archivedAt is null", repository.lastCountQuery);
    assertEquals("BU-7", repository.lastCountParams[0]);
  }

  @Test
  public void testSumActiveCapacityByLocationShouldReturnIntValue() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    repository.sumCapacityResult = 145L;

    int result = repository.sumActiveCapacityByLocation("ZWOLLE-001");

    assertEquals(145, result);
  }

  @Test
  public void testFindActiveEntityByBusinessUnitCodeShouldReturnEntity() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    DbWarehouse expected = dbWarehouse(13L, "BU-13", "LOC", 10, 1);
    repository.findFirstResult = expected;

    DbWarehouse result = repository.findActiveEntityByBusinessUnitCode("BU-13");

    assertSame(expected, result);
    assertEquals("businessUnitCode = ?1 and archivedAt is null", repository.lastFindQuery);
  }

  @Test
  public void testGetAllIncludingArchivedShouldMapAllRows() {
    TestWarehouseRepository repository = new TestWarehouseRepository();
    repository.listAllResult = List.of(dbWarehouse(1L, "A", "L1", 1, 1), dbWarehouse(2L, "B", "L2", 2, 2));

    List<Warehouse> result = repository.getAllIncludingArchived();

    assertEquals(2, result.size());
    assertEquals("A", result.get(0).businessUnitCode);
    assertEquals("B", result.get(1).businessUnitCode);
  }

  @Test
  public void testClearTestWarehousesShouldDeleteNonSeedRowsOnly() {
    TestWarehouseRepository repository = new TestWarehouseRepository();

    repository.clearTestWarehouses();

    assertEquals("businessUnitCode not in ?1", repository.lastDeleteQuery);
    @SuppressWarnings("unchecked")
    List<String> excludedSeedCodes = (List<String>) repository.lastDeleteParams[0];
    assertEquals(List.of("MWH.001", "MWH.012", "MWH.023"), excludedSeedCodes);
  }

  @Test
  public void testFlushShouldCallEntityManagerFlush() {
    TestWarehouseRepository repository = new TestWarehouseRepository();

    repository.flush();

    assertTrue(repository.flushCalled);
  }

  @Test
  public void testLockForMutationShouldAcquireDeterministicAdvisoryLocks() throws Exception {
    TestWarehouseRepository repository = new TestWarehouseRepository();

    repository.lockForMutation("BU-LOCK", "LOC-LOCK");

    assertEquals(2, repository.advisoryLockKeys.size());
    assertEquals(expectedHash(repository, "BU:BU-LOCK"), repository.advisoryLockKeys.get(0));
    assertEquals(expectedHash(repository, "LOC:LOC-LOCK"), repository.advisoryLockKeys.get(1));
  }

  private static Long expectedHash(WarehouseRepository repository, String key) throws Exception {
    Method hashMethod = WarehouseRepository.class.getDeclaredMethod("hashLockKey", String.class);
    hashMethod.setAccessible(true);
    return (Long) hashMethod.invoke(repository, key);
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(type)) {
      return false;
    }
    if (byte.class.equals(type) || short.class.equals(type) || int.class.equals(type)) {
      return 0;
    }
    if (long.class.equals(type)) {
      return 0L;
    }
    if (float.class.equals(type)) {
      return 0f;
    }
    if (double.class.equals(type)) {
      return 0d;
    }
    if (char.class.equals(type)) {
      return '\0';
    }
    return null;
  }

  private static DbWarehouse dbWarehouse(
      Long id, String businessUnitCode, String location, Integer capacity, Integer stock) {
    DbWarehouse entity = new DbWarehouse();
    entity.id = id;
    entity.businessUnitCode = businessUnitCode;
    entity.location = location;
    entity.capacity = capacity;
    entity.stock = stock;
    entity.createdAt = LocalDateTime.of(2026, 2, 25, 12, 0);
    return entity;
  }
}

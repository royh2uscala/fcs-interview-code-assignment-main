package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  @Test
  public void testReplaceShouldFailWhenCurrentWarehouseIsMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 50, 10);
    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(newWarehouse));
  }

  @Test
  public void testReplaceShouldFailWhenStockDoesNotMatch() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 40, 9);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenCapacityCannotAccommodateStock() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 8, 10);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceWarehouseSuccess() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    Warehouse current = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    current.id = 1L;
    store.activeWarehouses.add(current);

    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 60, 10);
    useCase.replace(replacement);

    assertNotNull(current.archivedAt);
    assertEquals(1, store.created.size());
    assertEquals("AMSTERDAM-001", store.created.get(0).location);
    assertEquals("MWH.001", store.created.get(0).businessUnitCode);
    assertEquals(1, store.flushCalls);
    assertEquals("MWH.001:AMSTERDAM-001", store.lockInvocations.get(0));
  }

  @Test
  public void testReplaceWarehouseSuccessWhenReplacingWithinSameLocation() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    Warehouse current = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    current.id = 1L;
    store.activeWarehouses.add(current);

    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 60, 10);
    useCase.replace(replacement);

    assertNotNull(current.archivedAt);
    assertEquals(1, store.created.size());
    assertEquals("ZWOLLE-001", store.created.get(0).location);
    assertEquals(1, store.flushCalls);
  }

  @Test
  public void testReplaceShouldFailWhenLocationIsInvalid() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "INVALID-001", 40, 10);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenTargetLocationMaxWarehousesReached() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    store.activeWarehouses.add(warehouse("MWH.777", "HELMOND-001", 10, 5));
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "HELMOND-001", 40, 10);
    assertThrows(WarehouseConflictException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenTargetLocationCapacityExceeded() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 40, 10));
    store.activeWarehouses.add(warehouse("MWH.888", "AMSTERDAM-001", 190, 20));
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 20, 10);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenPayloadIsNull() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(null));
  }

  @Test
  public void testReplaceShouldFailWhenBusinessUnitCodeMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse(" ", "ZWOLLE-001", 30, 10);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenLocationMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", " ", 30, 10);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenCapacityMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    replacement.capacity = null;
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenStockMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    replacement.stock = null;
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceShouldFailWhenNegativeValuesProvided() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ReplaceWarehouseUseCase useCase =
        new ReplaceWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", -1, -2);
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static class FakeLocationResolver implements LocationResolver {
    @Override
    public Location resolveByIdentifier(String identifier) {
      if ("ZWOLLE-001".equals(identifier)) {
        return new Location("ZWOLLE-001", 1, 100);
      }
      if ("AMSTERDAM-001".equals(identifier)) {
        return new Location("AMSTERDAM-001", 5, 200);
      }
      if ("HELMOND-001".equals(identifier)) {
        return new Location("HELMOND-001", 1, 80);
      }
      return null;
    }
  }

  private static class FakeWarehouseStore implements WarehouseStore {
    private final List<Warehouse> activeWarehouses = new ArrayList<>();
    private final List<Warehouse> created = new ArrayList<>();
    private final List<String> lockInvocations = new ArrayList<>();
    private int flushCalls = 0;

    @Override
    public List<Warehouse> getAll() {
      return activeWarehouses.stream().filter(w -> w.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouse.id = (long) (created.size() + 100);
      created.add(warehouse);
      activeWarehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
      // no-op, warehouse object is already mutable in list
    }

    @Override
    public void flush() {
      flushCalls++;
    }

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return activeWarehouses.stream()
          .filter(w -> buCode.equals(w.businessUnitCode) && w.archivedAt == null)
          .findFirst()
          .orElse(null);
    }

    @Override
    public void lockForMutation(String businessUnitCode, String location) {
      lockInvocations.add(businessUnitCode + ":" + location);
    }
  }
}

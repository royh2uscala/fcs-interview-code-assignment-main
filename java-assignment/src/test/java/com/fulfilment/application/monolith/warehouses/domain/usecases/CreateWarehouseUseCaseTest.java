package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  @Test
  public void testCreateWarehouseSuccess() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 20, 10);

    assertDoesNotThrow(() -> useCase.create(warehouse));
    assertEquals(1, store.created.size());
  }

  @Test
  public void testCreateWarehouseShouldFailWhenBusinessUnitAlreadyExists() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 10, 5));
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseConflictException.class,
        () -> useCase.create(warehouse("MWH.001", "ZWOLLE-001", 15, 10)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenLocationIsInvalid() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseValidationException.class, () -> useCase.create(warehouse("MWH.NEW", "INVALID", 10, 5)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenMaxNumberReached() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "ZWOLLE-001", 10, 5));
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseConflictException.class, () -> useCase.create(warehouse("MWH.NEW", "ZWOLLE-001", 10, 5)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenCapacityExceedsLocationLimit() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    store.activeWarehouses.add(warehouse("MWH.001", "AMSTERDAM-001", 90, 10));
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.NEW", "AMSTERDAM-001", 20, 5)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenStockGreaterThanCapacity() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.NEW", "AMSTERDAM-001", 10, 11)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenRequiredFieldsAreMissing() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    Warehouse missingFields = new Warehouse();
    missingFields.businessUnitCode = " ";
    missingFields.location = null;
    missingFields.capacity = 10;
    missingFields.stock = 5;

    assertThrows(WarehouseValidationException.class, () -> useCase.create(missingFields));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenValuesAreNegative() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    CreateWarehouseUseCase useCase =
        new CreateWarehouseUseCase(store, new FakeLocationResolver(), new WarehouseMutationLockManager());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.NEW", "AMSTERDAM-001", -1, 0)));
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
        return new Location("ZWOLLE-001", 1, 40);
      }
      if ("AMSTERDAM-001".equals(identifier)) {
        return new Location("AMSTERDAM-001", 5, 100);
      }
      return null;
    }
  }

  private static class FakeWarehouseStore implements WarehouseStore {
    private final List<Warehouse> activeWarehouses = new ArrayList<>();
    private final List<Warehouse> created = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return activeWarehouses;
    }

    @Override
    public void create(Warehouse warehouse) {
      created.add(warehouse);
      activeWarehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return activeWarehouses.stream()
          .filter(w -> buCode.equals(w.businessUnitCode) && w.archivedAt == null)
          .findFirst()
          .orElse(null);
    }
  }
}

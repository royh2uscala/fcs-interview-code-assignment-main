package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  @Test
  public void testArchiveShouldFailWhenWarehouseIsNull() {
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(new FakeWarehouseStore());
    assertThrows(WarehouseValidationException.class, () -> useCase.archive(null));
  }

  @Test
  public void testArchiveShouldFailWhenAlreadyArchived() {
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(new FakeWarehouseStore());
    Warehouse warehouse = new Warehouse();
    warehouse.archivedAt = java.time.LocalDateTime.now();
    assertThrows(WarehouseConflictException.class, () -> useCase.archive(warehouse));
  }

  @Test
  public void testArchiveShouldSetArchivedAtAndUpdateWarehouse() {
    FakeWarehouseStore store = new FakeWarehouseStore();
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertNotNull(store.updated.get(0).archivedAt);
  }

  private static class FakeWarehouseStore implements WarehouseStore {
    private final List<Warehouse> updated = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return List.of();
    }

    @Override
    public void create(Warehouse warehouse) {}

    @Override
    public void update(Warehouse warehouse) {
      updated.add(warehouse);
    }

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return null;
    }
  }
}

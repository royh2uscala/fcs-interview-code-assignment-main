package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseMutationLockManager mutationLockManager;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseMutationLockManager mutationLockManager) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.mutationLockManager = mutationLockManager;
  }

  @Override
  public void create(Warehouse warehouse) {
    validateRequiredFields(warehouse);

    mutationLockManager.withLocks(
        List.of(lockKeyForBusinessUnit(warehouse.businessUnitCode), lockKeyForLocation(warehouse.location)),
        () -> {
          warehouseStore.lockForMutation(warehouse.businessUnitCode, warehouse.location);

          if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
            throw new WarehouseConflictException("Business unit code already exists");
          }

          var location = locationResolver.resolveByIdentifier(warehouse.location);
          if (location == null) {
            throw new WarehouseValidationException("Warehouse location is invalid");
          }

          long activeWarehousesAtLocation =
              warehouseStore.getAll().stream().filter(w -> warehouse.location.equals(w.location)).count();
          if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
            throw new WarehouseConflictException("Maximum number of warehouses reached for location");
          }

          int activeCapacityAtLocation =
              warehouseStore.getAll().stream()
                  .filter(w -> warehouse.location.equals(w.location))
                  .mapToInt(w -> w.capacity == null ? 0 : w.capacity)
                  .sum();
          if (activeCapacityAtLocation + warehouse.capacity > location.maxCapacity) {
            throw new WarehouseValidationException("Warehouse capacity exceeds location maximum capacity");
          }

          if (warehouse.capacity < warehouse.stock) {
            throw new WarehouseValidationException("Warehouse capacity cannot be lower than stock");
          }

          warehouse.id = null;
          warehouse.createdAt = LocalDateTime.now();
          warehouse.archivedAt = null;
          warehouseStore.create(warehouse);
        });
  }

  private void validateRequiredFields(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseValidationException("Warehouse payload is required");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WarehouseValidationException("Business unit code is required");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WarehouseValidationException("Location is required");
    }
    if (warehouse.capacity == null || warehouse.capacity < 0) {
      throw new WarehouseValidationException("Capacity must be a non-negative integer");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseValidationException("Stock must be a non-negative integer");
    }
  }

  private String lockKeyForBusinessUnit(String businessUnitCode) {
    return "BU:" + businessUnitCode;
  }

  private String lockKeyForLocation(String location) {
    return "LOC:" + location;
  }
}

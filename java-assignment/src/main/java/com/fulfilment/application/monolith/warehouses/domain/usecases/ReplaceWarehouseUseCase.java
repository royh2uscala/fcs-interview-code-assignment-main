package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseMutationLockManager mutationLockManager;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseMutationLockManager mutationLockManager) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.mutationLockManager = mutationLockManager;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    validateRequiredFields(newWarehouse);

    mutationLockManager.withLocks(
        List.of(lockKeyForBusinessUnit(newWarehouse.businessUnitCode), lockKeyForLocation(newWarehouse.location)),
        () -> {
          warehouseStore.lockForMutation(newWarehouse.businessUnitCode, newWarehouse.location);

          Warehouse currentWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
          if (currentWarehouse == null) {
            throw new WarehouseNotFoundException("Current active warehouse was not found");
          }

          if (!newWarehouse.stock.equals(currentWarehouse.stock)) {
            throw new WarehouseValidationException(
                "Replacement warehouse stock must match previous warehouse stock");
          }

          if (newWarehouse.capacity < currentWarehouse.stock) {
            throw new WarehouseValidationException(
                "Replacement warehouse capacity cannot be lower than the previous warehouse stock");
          }

          if (newWarehouse.capacity < newWarehouse.stock) {
            throw new WarehouseValidationException("Warehouse capacity cannot be lower than stock");
          }

          var location = locationResolver.resolveByIdentifier(newWarehouse.location);
          if (location == null) {
            throw new WarehouseValidationException("Warehouse location is invalid");
          }

          long currentCountAtNewLocation =
              warehouseStore.getAll().stream().filter(w -> newWarehouse.location.equals(w.location)).count();
          int currentCapacityAtNewLocation =
              warehouseStore.getAll().stream()
                  .filter(w -> newWarehouse.location.equals(w.location))
                  .mapToInt(w -> w.capacity == null ? 0 : w.capacity)
                  .sum();

          long resultingCountAtNewLocation = currentCountAtNewLocation;
          int resultingCapacityAtNewLocation = currentCapacityAtNewLocation + newWarehouse.capacity;
          if (newWarehouse.location.equals(currentWarehouse.location)) {
            resultingCapacityAtNewLocation =
                currentCapacityAtNewLocation - currentWarehouse.capacity + newWarehouse.capacity;
          } else {
            resultingCountAtNewLocation = currentCountAtNewLocation + 1;
          }

          if (resultingCountAtNewLocation > location.maxNumberOfWarehouses) {
            throw new WarehouseConflictException("Maximum number of warehouses reached for location");
          }

          if (resultingCapacityAtNewLocation > location.maxCapacity) {
            throw new WarehouseValidationException("Warehouse capacity exceeds location maximum capacity");
          }

          currentWarehouse.archivedAt = LocalDateTime.now();
          warehouseStore.update(currentWarehouse);
          warehouseStore.flush();

          Warehouse replacement = new Warehouse();
          replacement.id = null;
          replacement.businessUnitCode = newWarehouse.businessUnitCode;
          replacement.location = newWarehouse.location;
          replacement.capacity = newWarehouse.capacity;
          replacement.stock = newWarehouse.stock;
          replacement.createdAt = LocalDateTime.now();
          replacement.archivedAt = null;
          warehouseStore.create(replacement);
          newWarehouse.id = replacement.id;
          newWarehouse.createdAt = replacement.createdAt;
          newWarehouse.archivedAt = replacement.archivedAt;
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

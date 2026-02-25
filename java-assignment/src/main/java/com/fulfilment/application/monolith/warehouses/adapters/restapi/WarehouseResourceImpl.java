package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.warehouse.api.WarehouseResource;
import jakarta.persistence.PersistenceException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseUseCase createWarehouseUseCase;
  @Inject private ArchiveWarehouseUseCase archiveWarehouseUseCase;
  @Inject private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    try {
      Warehouse warehouseToCreate = toWarehouseDomain(data);
      createWarehouseUseCase.create(warehouseToCreate);
      Warehouse created = warehouseRepository.findByBusinessUnitCode(warehouseToCreate.businessUnitCode);
      return toWarehouseResponse(created);
    } catch (WarehouseValidationException ex) {
      throw asBadRequest(ex);
    } catch (WarehouseConflictException ex) {
      throw asConflict(ex);
    } catch (PersistenceException ex) {
      throw mapPersistenceException(ex);
    }
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    Long numericId = parseWarehouseId(id);
    Warehouse warehouse = warehouseRepository.findByIdAsDomain(numericId);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse unit not found", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    Long numericId = parseWarehouseId(id);
    Warehouse warehouse = warehouseRepository.findByIdAsDomain(numericId);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse unit not found", 404);
    }

    try {
      archiveWarehouseUseCase.archive(warehouse);
    } catch (WarehouseValidationException ex) {
      throw asBadRequest(ex);
    } catch (WarehouseConflictException ex) {
      throw asConflict(ex);
    } catch (PersistenceException ex) {
      throw mapPersistenceException(ex);
    }
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    try {
      Warehouse replacement = toWarehouseDomain(data);
      replacement.businessUnitCode = businessUnitCode;
      replaceWarehouseUseCase.replace(replacement);
      Warehouse created = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      if (created == null) {
        throw new WebApplicationException("Warehouse replacement failed", 500);
      }
      return toWarehouseResponse(created);
    } catch (WarehouseNotFoundException ex) {
      throw asNotFound(ex);
    } catch (WarehouseValidationException ex) {
      throw asBadRequest(ex);
    } catch (WarehouseConflictException ex) {
      throw asConflict(ex);
    } catch (PersistenceException ex) {
      throw mapPersistenceException(ex);
    }
  }

  private com.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  private Warehouse toWarehouseDomain(com.warehouse.api.beans.Warehouse warehouseRequest) {
    Warehouse warehouse = new Warehouse();
    if (warehouseRequest.getId() != null && !warehouseRequest.getId().isBlank()) {
      warehouse.id = parseWarehouseId(warehouseRequest.getId());
    }
    warehouse.businessUnitCode = warehouseRequest.getBusinessUnitCode();
    warehouse.location = warehouseRequest.getLocation();
    warehouse.capacity = warehouseRequest.getCapacity();
    warehouse.stock = warehouseRequest.getStock();
    return warehouse;
  }

  private Long parseWarehouseId(String id) {
    try {
      return Long.parseLong(id);
    } catch (Exception ex) {
      throw new WebApplicationException("Warehouse id must be numeric", 400);
    }
  }

  private WebApplicationException asBadRequest(RuntimeException ex) {
    return new WebApplicationException(ex.getMessage(), 400);
  }

  private WebApplicationException asNotFound(RuntimeException ex) {
    return new WebApplicationException(ex.getMessage(), 404);
  }

  private WebApplicationException asConflict(RuntimeException ex) {
    return new WebApplicationException(ex.getMessage(), 409);
  }

  private WebApplicationException mapPersistenceException(PersistenceException ex) {
    String details = exceptionDetails(ex);
    if (containsAny(details, "uk_warehouse_active_buc", "duplicate key", "unique constraint")) {
      return new WebApplicationException("Business unit code already exists", 409);
    }
    if (containsAny(details, "chk_warehouse_capacity_stock", "check constraint")) {
      return new WebApplicationException("Warehouse capacity cannot be lower than stock", 400);
    }
    return new WebApplicationException("Warehouse operation failed", 500);
  }

  private String exceptionDetails(Throwable ex) {
    StringBuilder details = new StringBuilder();
    Throwable current = ex;
    while (current != null) {
      if (current.getMessage() != null) {
        details.append(current.getMessage()).append('\n');
      }
      current = current.getCause();
    }
    return details.toString().toLowerCase();
  }

  private boolean containsAny(String value, String... candidates) {
    for (String candidate : candidates) {
      if (value.contains(candidate.toLowerCase())) {
        return true;
      }
    }
    return false;
  }
}

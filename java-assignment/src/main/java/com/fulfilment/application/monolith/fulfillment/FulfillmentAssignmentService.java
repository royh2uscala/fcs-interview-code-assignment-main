package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class FulfillmentAssignmentService {

  @Inject FulfillmentAssignmentRepository repository;

  @Transactional
  public FulfillmentAssignment assign(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    validateInput(storeId, productId, warehouseBusinessUnitCode);

    Store store = repository.getEntityManager().find(Store.class, storeId, LockModeType.PESSIMISTIC_WRITE);
    if (store == null) {
      throw new IllegalArgumentException("Store does not exist");
    }

    Product product = repository.getEntityManager().find(Product.class, productId, LockModeType.PESSIMISTIC_WRITE);
    if (product == null) {
      throw new IllegalArgumentException("Product does not exist");
    }

    DbWarehouse warehouse =
        repository.getEntityManager()
            .createQuery(
                "select w from DbWarehouse w where w.businessUnitCode = :bu and w.archivedAt is null",
                DbWarehouse.class)
            .setParameter("bu", warehouseBusinessUnitCode)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultStream()
            .findFirst()
            .orElse(null);
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse does not exist or is archived");
    }

    FulfillmentAssignment existing =
        repository
            .find(
                "storeId = ?1 and productId = ?2 and warehouseBusinessUnitCode = ?3",
                storeId,
                productId,
                warehouseBusinessUnitCode)
            .firstResult();
    if (existing != null) {
      return existing;
    }

    long warehousesByStoreAndProduct = repository.countDistinctWarehousesByStoreAndProduct(storeId, productId);
    if (warehousesByStoreAndProduct >= 2) {
      throw new IllegalStateException(
          "Each Product can be fulfilled by a maximum of 2 different Warehouses per Store");
    }

    long warehousesByStore = repository.countDistinctWarehousesByStore(storeId);
    if (warehousesByStore >= 3) {
      throw new IllegalStateException(
          "Each Store can be fulfilled by a maximum of 3 different Warehouses");
    }

    long productsByWarehouse = repository.countDistinctProductsByWarehouse(warehouseBusinessUnitCode);
    if (productsByWarehouse >= 5) {
      throw new IllegalStateException(
          "Each Warehouse can store maximally 5 types of Products");
    }

    FulfillmentAssignment assignment = new FulfillmentAssignment();
    assignment.storeId = storeId;
    assignment.productId = productId;
    assignment.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    assignment.createdAt = LocalDateTime.now();
    repository.persist(assignment);

    return assignment;
  }

  public List<FulfillmentAssignment> listAll() {
    return repository.listAll();
  }

  private void validateInput(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    if (storeId == null || productId == null || warehouseBusinessUnitCode == null || warehouseBusinessUnitCode.isBlank()) {
      throw new IllegalArgumentException("storeId, productId and warehouseBusinessUnitCode are required");
    }
  }
}

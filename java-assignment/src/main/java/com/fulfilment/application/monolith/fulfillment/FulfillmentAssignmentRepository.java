package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FulfillmentAssignmentRepository implements PanacheRepository<FulfillmentAssignment> {

  public long countDistinctWarehousesByStoreAndProduct(Long storeId, Long productId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouseBusinessUnitCode) "
                + "from FulfillmentAssignment f where f.storeId = :storeId and f.productId = :productId",
            Long.class)
        .setParameter("storeId", storeId)
        .setParameter("productId", productId)
        .getSingleResult();
  }

  public long countDistinctWarehousesByStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouseBusinessUnitCode) "
                + "from FulfillmentAssignment f where f.storeId = :storeId",
            Long.class)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  public long countDistinctProductsByWarehouse(String warehouseBusinessUnitCode) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.productId) "
                + "from FulfillmentAssignment f where f.warehouseBusinessUnitCode = :warehouseBusinessUnitCode",
            Long.class)
        .setParameter("warehouseBusinessUnitCode", warehouseBusinessUnitCode)
        .getSingleResult();
  }
}

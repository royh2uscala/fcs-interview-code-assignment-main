package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fulfillment_assignment",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_assignment_store_product_warehouse",
          columnNames = {"storeId", "productId", "warehouseBusinessUnitCode"})
    },
    indexes = {
      @Index(name = "idx_assignment_store", columnList = "storeId"),
      @Index(name = "idx_assignment_warehouse", columnList = "warehouseBusinessUnitCode")
    })
public class FulfillmentAssignment extends PanacheEntity {

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false, length = 80)
  public String warehouseBusinessUnitCode;

  @Column(nullable = false)
  public LocalDateTime createdAt;
}

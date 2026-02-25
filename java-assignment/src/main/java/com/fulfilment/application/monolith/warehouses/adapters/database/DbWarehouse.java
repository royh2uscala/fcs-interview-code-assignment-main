package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "warehouse",
    indexes = {
      @Index(name = "idx_warehouse_location_archived", columnList = "location,archivedAt"),
      @Index(name = "idx_warehouse_bu_archived", columnList = "businessUnitCode,archivedAt")
    })
@Cacheable
public class DbWarehouse {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false, length = 80)
  public String businessUnitCode;

  @Column(nullable = false, length = 80)
  public String location;

  @Column(nullable = false)
  public Integer capacity;

  @Column(nullable = false)
  public Integer stock;

  @Column(nullable = false)
  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public DbWarehouse() {}

  public static DbWarehouse fromWarehouse(Warehouse warehouse) {
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.id = warehouse.id;
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    return dbWarehouse;
  }

  public Warehouse toWarehouse() {
    var warehouse = new Warehouse();
    warehouse.id = this.id;
    warehouse.businessUnitCode = this.businessUnitCode;
    warehouse.location = this.location;
    warehouse.capacity = this.capacity;
    warehouse.stock = this.stock;
    warehouse.createdAt = this.createdAt;
    warehouse.archivedAt = this.archivedAt;
    return warehouse;
  }
}

package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DbWarehouseTest {

  @Test
  public void testFromWarehouseShouldMapAllFields() {
    Warehouse warehouse = new Warehouse();
    warehouse.id = 11L;
    warehouse.businessUnitCode = "BU-11";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 30;
    warehouse.createdAt = LocalDateTime.of(2026, 2, 25, 9, 0);
    warehouse.archivedAt = LocalDateTime.of(2026, 2, 26, 9, 0);

    DbWarehouse mapped = DbWarehouse.fromWarehouse(warehouse);

    assertEquals(11L, mapped.id);
    assertEquals("BU-11", mapped.businessUnitCode);
    assertEquals("ZWOLLE-001", mapped.location);
    assertEquals(100, mapped.capacity);
    assertEquals(30, mapped.stock);
    assertSame(warehouse.createdAt, mapped.createdAt);
    assertSame(warehouse.archivedAt, mapped.archivedAt);
  }

  @Test
  public void testToWarehouseShouldMapAllFields() {
    DbWarehouse entity = new DbWarehouse();
    entity.id = 21L;
    entity.businessUnitCode = "BU-21";
    entity.location = "ROTTERDAM-001";
    entity.capacity = 250;
    entity.stock = 75;
    entity.createdAt = LocalDateTime.of(2026, 2, 20, 8, 15);
    entity.archivedAt = LocalDateTime.of(2026, 2, 21, 8, 15);

    Warehouse mapped = entity.toWarehouse();

    assertEquals(21L, mapped.id);
    assertEquals("BU-21", mapped.businessUnitCode);
    assertEquals("ROTTERDAM-001", mapped.location);
    assertEquals(250, mapped.capacity);
    assertEquals(75, mapped.stock);
    assertSame(entity.createdAt, mapped.createdAt);
    assertSame(entity.archivedAt, mapped.archivedAt);
  }
}

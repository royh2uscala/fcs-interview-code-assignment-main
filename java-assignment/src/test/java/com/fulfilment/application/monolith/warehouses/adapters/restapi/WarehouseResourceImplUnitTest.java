package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseConflictException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseMutationLockManager;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WarehouseResourceImplUnitTest {

  private static final WarehouseStore NOOP_STORE =
      new WarehouseStore() {
        @Override
        public List<Warehouse> getAll() {
          return List.of();
        }

        @Override
        public void create(Warehouse warehouse) {}

        @Override
        public void update(Warehouse warehouse) {}

        @Override
        public void remove(Warehouse warehouse) {}

        @Override
        public Warehouse findByBusinessUnitCode(String buCode) {
          return null;
        }
      };

  private static final LocationResolver NOOP_LOCATION_RESOLVER = identifier -> null;

  private static class StubWarehouseRepository extends WarehouseRepository {
    final List<Warehouse> all = new ArrayList<>();
    final Map<Long, Warehouse> byId = new HashMap<>();
    final Map<String, Warehouse> byBusinessUnitCode = new HashMap<>();

    @Override
    public List<Warehouse> getAll() {
      return all;
    }

    @Override
    public Warehouse findByIdAsDomain(Long id) {
      return byId.get(id);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return byBusinessUnitCode.get(buCode);
    }
  }

  private static class StubCreateUseCase extends CreateWarehouseUseCase {
    Warehouse captured;
    RuntimeException toThrow;

    StubCreateUseCase() {
      super(NOOP_STORE, NOOP_LOCATION_RESOLVER, new WarehouseMutationLockManager());
    }

    @Override
    public void create(Warehouse warehouse) {
      captured = warehouse;
      if (toThrow != null) {
        throw toThrow;
      }
    }
  }

  private static class StubArchiveUseCase extends ArchiveWarehouseUseCase {
    Warehouse captured;
    RuntimeException toThrow;

    StubArchiveUseCase() {
      super(NOOP_STORE);
    }

    @Override
    public void archive(Warehouse warehouse) {
      captured = warehouse;
      if (toThrow != null) {
        throw toThrow;
      }
    }
  }

  private static class StubReplaceUseCase extends ReplaceWarehouseUseCase {
    Warehouse captured;
    RuntimeException toThrow;

    StubReplaceUseCase() {
      super(NOOP_STORE, NOOP_LOCATION_RESOLVER, new WarehouseMutationLockManager());
    }

    @Override
    public void replace(Warehouse newWarehouse) {
      captured = newWarehouse;
      if (toThrow != null) {
        throw toThrow;
      }
    }
  }

  @Test
  public void testListAllWarehousesUnitsShouldMapDomainWarehouses() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    StubArchiveUseCase archive = new StubArchiveUseCase();
    StubReplaceUseCase replace = new StubReplaceUseCase();
    WarehouseResourceImpl resource = resource(repository, create, archive, replace);

    Warehouse warehouse = domainWarehouse(1L, "BU-1", "ZWOLLE-001", 100, 20);
    repository.all.add(warehouse);

    List<com.warehouse.api.beans.Warehouse> result = resource.listAllWarehousesUnits();

    assertEquals(1, result.size());
    assertEquals("1", result.get(0).getId());
    assertEquals("BU-1", result.get(0).getBusinessUnitCode());
    assertEquals("ZWOLLE-001", result.get(0).getLocation());
    assertEquals(100, result.get(0).getCapacity());
    assertEquals(20, result.get(0).getStock());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldCreateAndReturnMappedWarehouse() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    StubArchiveUseCase archive = new StubArchiveUseCase();
    StubReplaceUseCase replace = new StubReplaceUseCase();
    WarehouseResourceImpl resource = resource(repository, create, archive, replace);

    com.warehouse.api.beans.Warehouse request = apiWarehouse("17", "BU-NEW", "ZWOLLE-001", 90, 30);
    repository.byBusinessUnitCode.put("BU-NEW", domainWarehouse(17L, "BU-NEW", "ZWOLLE-001", 90, 30));

    com.warehouse.api.beans.Warehouse response = resource.createANewWarehouseUnit(request);

    assertEquals(17L, create.captured.id);
    assertEquals("BU-NEW", create.captured.businessUnitCode);
    assertEquals("17", response.getId());
    assertEquals("BU-NEW", response.getBusinessUnitCode());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldMapValidationExceptionTo400() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    create.toThrow = new WarehouseValidationException("invalid warehouse");
    WarehouseResourceImpl resource = resource(repository, create, new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createANewWarehouseUnit(apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("invalid warehouse", ex.getMessage());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldMapConflictExceptionTo409() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    create.toThrow = new WarehouseConflictException("conflict");
    WarehouseResourceImpl resource = resource(repository, create, new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createANewWarehouseUnit(apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("conflict", ex.getMessage());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldMapPersistenceDuplicateKeyTo409() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    create.toThrow = new PersistenceException("duplicate key value violates unique constraint uk_warehouse_active_buc");
    WarehouseResourceImpl resource = resource(repository, create, new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createANewWarehouseUnit(apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("Business unit code already exists", ex.getMessage());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldMapPersistenceCheckConstraintTo400() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    create.toThrow = new PersistenceException("check constraint chk_warehouse_capacity_stock");
    WarehouseResourceImpl resource = resource(repository, create, new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createANewWarehouseUnit(apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Warehouse capacity cannot be lower than stock", ex.getMessage());
  }

  @Test
  public void testCreateANewWarehouseUnitShouldMapUnknownPersistenceTo500() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubCreateUseCase create = new StubCreateUseCase();
    create.toThrow = new PersistenceException("other db failure");
    WarehouseResourceImpl resource = resource(repository, create, new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.createANewWarehouseUnit(apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(500, ex.getResponse().getStatus());
    assertEquals("Warehouse operation failed", ex.getMessage());
  }

  @Test
  public void testGetAWarehouseUnitByIDShouldRejectNonNumericId() {
    WarehouseResourceImpl resource =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("abc"));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Warehouse id must be numeric", ex.getMessage());
  }

  @Test
  public void testGetAWarehouseUnitByIDShouldReturnNotFoundWhenMissing() {
    WarehouseResourceImpl resource =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("123"));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Warehouse unit not found", ex.getMessage());
  }

  @Test
  public void testGetAWarehouseUnitByIDShouldReturnMappedResponse() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    repository.byId.put(123L, domainWarehouse(123L, "BU-123", "ZWOLLE-001", 100, 10));
    WarehouseResourceImpl resource =
        resource(repository, new StubCreateUseCase(), new StubArchiveUseCase(), new StubReplaceUseCase());

    com.warehouse.api.beans.Warehouse result = resource.getAWarehouseUnitByID("123");

    assertEquals("123", result.getId());
    assertEquals("BU-123", result.getBusinessUnitCode());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDShouldMapValidationExceptionTo400() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    repository.byId.put(2L, domainWarehouse(2L, "BU-2", "LOC", 10, 1));
    StubArchiveUseCase archive = new StubArchiveUseCase();
    archive.toThrow = new WarehouseValidationException("invalid archive");
    WarehouseResourceImpl resource = resource(repository, new StubCreateUseCase(), archive, new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("2"));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("invalid archive", ex.getMessage());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDShouldRejectNonNumericId() {
    WarehouseResourceImpl resource =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("not-a-number"));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Warehouse id must be numeric", ex.getMessage());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDShouldReturnNotFoundWhenWarehouseMissing() {
    WarehouseResourceImpl resource =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("999"));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Warehouse unit not found", ex.getMessage());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDShouldMapConflictExceptionTo409() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    repository.byId.put(3L, domainWarehouse(3L, "BU-3", "LOC", 10, 1));
    StubArchiveUseCase archive = new StubArchiveUseCase();
    archive.toThrow = new WarehouseConflictException("already archived");
    WarehouseResourceImpl resource = resource(repository, new StubCreateUseCase(), archive, new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("3"));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("already archived", ex.getMessage());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDShouldMapPersistenceException() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    repository.byId.put(4L, domainWarehouse(4L, "BU-4", "LOC", 10, 1));
    StubArchiveUseCase archive = new StubArchiveUseCase();
    archive.toThrow = new PersistenceException("check constraint chk_warehouse_capacity_stock");
    WarehouseResourceImpl resource = resource(repository, new StubCreateUseCase(), archive, new StubReplaceUseCase());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("4"));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Warehouse capacity cannot be lower than stock", ex.getMessage());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseShouldReplaceAndReturnMappedWarehouse() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    repository.byBusinessUnitCode.put("BU-1", domainWarehouse(50L, "BU-1", "ZWOLLE-001", 120, 30));
    StubReplaceUseCase replace = new StubReplaceUseCase();
    WarehouseResourceImpl resource = resource(repository, new StubCreateUseCase(), new StubArchiveUseCase(), replace);

    com.warehouse.api.beans.Warehouse request = apiWarehouse("1", "SHOULD-BE-IGNORED", "ZWOLLE-001", 120, 30);
    com.warehouse.api.beans.Warehouse result = resource.replaceTheCurrentActiveWarehouse("BU-1", request);

    assertNotNull(replace.captured);
    assertEquals("BU-1", replace.captured.businessUnitCode);
    assertEquals("50", result.getId());
    assertEquals("BU-1", result.getBusinessUnitCode());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseShouldMapNotFoundTo404() {
    StubReplaceUseCase replace = new StubReplaceUseCase();
    replace.toThrow = new WarehouseNotFoundException("not found");
    WarehouseResourceImpl resource =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), replace);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("BU", apiWarehouse(null, "BU", "LOC", 1, 1)));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("not found", ex.getMessage());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseShouldMapValidationAndConflict() {
    StubReplaceUseCase replaceValidation = new StubReplaceUseCase();
    replaceValidation.toThrow = new WarehouseValidationException("bad replacement");
    WarehouseResourceImpl resourceValidation =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), replaceValidation);

    WebApplicationException validationEx =
        assertThrows(
            WebApplicationException.class,
            () ->
                resourceValidation.replaceTheCurrentActiveWarehouse(
                    "BU", apiWarehouse(null, "BU", "LOC", 1, 1)));
    assertEquals(400, validationEx.getResponse().getStatus());
    assertEquals("bad replacement", validationEx.getMessage());

    StubReplaceUseCase replaceConflict = new StubReplaceUseCase();
    replaceConflict.toThrow = new WarehouseConflictException("conflict replacement");
    WarehouseResourceImpl resourceConflict =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), replaceConflict);

    WebApplicationException conflictEx =
        assertThrows(
            WebApplicationException.class,
            () ->
                resourceConflict.replaceTheCurrentActiveWarehouse(
                    "BU", apiWarehouse(null, "BU", "LOC", 1, 1)));
    assertEquals(409, conflictEx.getResponse().getStatus());
    assertEquals("conflict replacement", conflictEx.getMessage());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseShouldHandlePersistenceAndMissingCreatedWarehouse() {
    StubWarehouseRepository repository = new StubWarehouseRepository();
    StubReplaceUseCase replace = new StubReplaceUseCase();
    replace.toThrow = new PersistenceException("duplicate key value violates unique constraint");
    WarehouseResourceImpl resource = resource(repository, new StubCreateUseCase(), new StubArchiveUseCase(), replace);

    WebApplicationException duplicateEx =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("BU", apiWarehouse(null, "BU", "LOC", 1, 1)));
    assertEquals(409, duplicateEx.getResponse().getStatus());
    assertEquals("Business unit code already exists", duplicateEx.getMessage());

    StubReplaceUseCase replaceSuccess = new StubReplaceUseCase();
    WarehouseResourceImpl resourceMissingCreated =
        resource(new StubWarehouseRepository(), new StubCreateUseCase(), new StubArchiveUseCase(), replaceSuccess);
    WebApplicationException missingCreatedEx =
        assertThrows(
            WebApplicationException.class,
            () ->
                resourceMissingCreated.replaceTheCurrentActiveWarehouse(
                    "BU", apiWarehouse(null, "BU", "LOC", 1, 1)));
    assertEquals(500, missingCreatedEx.getResponse().getStatus());
    assertEquals("Warehouse replacement failed", missingCreatedEx.getMessage());
  }

  private static WarehouseResourceImpl resource(
      StubWarehouseRepository repository,
      StubCreateUseCase createUseCase,
      StubArchiveUseCase archiveUseCase,
      StubReplaceUseCase replaceUseCase) {
    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repository);
    setField(resource, "createWarehouseUseCase", createUseCase);
    setField(resource, "archiveWarehouseUseCase", archiveUseCase);
    setField(resource, "replaceWarehouseUseCase", replaceUseCase);
    return resource;
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field field = WarehouseResourceImpl.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Unable to set test field " + name, ex);
    }
  }

  private static Warehouse domainWarehouse(
      Long id, String businessUnitCode, String location, Integer capacity, Integer stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.id = id;
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static com.warehouse.api.beans.Warehouse apiWarehouse(
      String id, String businessUnitCode, String location, Integer capacity, Integer stock) {
    com.warehouse.api.beans.Warehouse warehouse = new com.warehouse.api.beans.Warehouse();
    warehouse.setId(id);
    warehouse.setBusinessUnitCode(businessUnitCode);
    warehouse.setLocation(location);
    warehouse.setCapacity(capacity);
    warehouse.setStock(stock);
    return warehouse;
  }
}

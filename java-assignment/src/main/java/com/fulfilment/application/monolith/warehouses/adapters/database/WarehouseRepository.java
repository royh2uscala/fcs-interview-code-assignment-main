package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return find("archivedAt is null").list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);
    if (entity.createdAt == null) {
      entity.createdAt = LocalDateTime.now();
    }
    entity.archivedAt = null;
    persist(entity);
    warehouse.id = entity.id;
    warehouse.createdAt = entity.createdAt;
    warehouse.archivedAt = entity.archivedAt;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse entity = resolveEntityForUpdate(warehouse);
    if (entity == null) {
      throw new IllegalStateException("Warehouse not found for update");
    }
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = resolveEntityForUpdate(warehouse);
    if (entity != null) {
      delete(entity);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  public Warehouse findByIdAsDomain(Long id) {
    DbWarehouse entity = find("id = ?1 and archivedAt is null", id).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  public long countActiveByLocation(String location) {
    return count("location = ?1 and archivedAt is null", location);
  }

  public long countActiveByBusinessUnitCode(String businessUnitCode) {
    return count("businessUnitCode = ?1 and archivedAt is null", businessUnitCode);
  }

  public int sumActiveCapacityByLocation(String location) {
    Long value =
        getEntityManager()
            .createQuery(
                "select coalesce(sum(w.capacity), 0) from DbWarehouse w where w.location = :location and w.archivedAt is null",
                Long.class)
            .setParameter("location", location)
            .getSingleResult();
    return value.intValue();
  }

  public DbWarehouse findActiveEntityByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
  }

  public List<Warehouse> getAllIncludingArchived() {
    return listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Transactional
  public void clearTestWarehouses() {
    delete("businessUnitCode not in ?1", List.of("MWH.001", "MWH.012", "MWH.023"));
  }

  @Override
  public void flush() {
    getEntityManager().flush();
  }

  @Override
  public void lockForMutation(String businessUnitCode, String location) {
    List<String> lockKeys =
        List.of("BU:" + businessUnitCode, "LOC:" + location).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted()
            .toList();

    for (String lockKey : lockKeys) {
      getEntityManager()
          .createNativeQuery("select pg_advisory_xact_lock(?1)")
          .setParameter(1, hashLockKey(lockKey))
          .getSingleResult();
    }
  }

  private DbWarehouse resolveEntityForUpdate(Warehouse warehouse) {
    if (warehouse.id != null) {
      return findById(warehouse.id);
    }
    return find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode).firstResult();
  }

  private long hashLockKey(String lockKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(lockKey.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.wrap(bytes, 0, Long.BYTES).getLong();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Unable to create warehouse mutation lock hash", ex);
    }
  }
}

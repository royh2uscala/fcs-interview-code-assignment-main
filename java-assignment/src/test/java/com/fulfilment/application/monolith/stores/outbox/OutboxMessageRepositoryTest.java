package com.fulfilment.application.monolith.stores.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class OutboxMessageRepositoryTest {

  private static class TestOutboxMessageRepository extends OutboxMessageRepository {
    OutboxMessage persisted;
    OutboxMessage merged;
    List<OutboxMessage> pendingResult = List.of();
    int capturedPageIndex = -1;
    int capturedPageSize = -1;
    String lastFindQuery;
    Object[] lastFindParams;
    final Map<String, Long> countResults = new HashMap<>();
    String lastCountQuery;
    int updateResult;
    String lastUpdateQuery;
    Object[] lastUpdateParams;
    boolean deleteAllCalled;

    @Override
    public void persist(OutboxMessage entity) {
      persisted = entity;
    }

    @Override
    public PanacheQuery<OutboxMessage> find(String query, Object... params) {
      lastFindQuery = query;
      lastFindParams = params;
      return panacheQuery();
    }

    @SuppressWarnings("unchecked")
    private PanacheQuery<OutboxMessage> panacheQuery() {
      return (PanacheQuery<OutboxMessage>)
          Proxy.newProxyInstance(
              PanacheQuery.class.getClassLoader(),
              new Class<?>[] {PanacheQuery.class},
              (proxy, method, args) -> {
                if ("page".equals(method.getName())) {
                  capturedPageIndex = (Integer) args[0];
                  capturedPageSize = (Integer) args[1];
                  return proxy;
                }
                if ("list".equals(method.getName())) {
                  return pendingResult;
                }
                if ("firstResult".equals(method.getName())) {
                  return pendingResult.isEmpty() ? null : pendingResult.get(0);
                }
                return defaultValue(method.getReturnType());
              });
    }

    @Override
    public long count(String query, Object... params) {
      lastCountQuery = query;
      return countResults.getOrDefault(query, 0L);
    }

    @Override
    public int update(String query, Object... params) {
      lastUpdateQuery = query;
      lastUpdateParams = params;
      return updateResult;
    }

    @Override
    public long deleteAll() {
      deleteAllCalled = true;
      return 0L;
    }

    @Override
    public EntityManager getEntityManager() {
      return (EntityManager)
          Proxy.newProxyInstance(
              EntityManager.class.getClassLoader(),
              new Class<?>[] {EntityManager.class},
              (proxy, method, args) -> {
                if ("merge".equals(method.getName())) {
                  merged = (OutboxMessage) args[0];
                  return merged;
                }
                return defaultValue(method.getReturnType());
              });
    }
  }

  @Test
  public void testCreateShouldPersistMessage() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    OutboxMessage message = new OutboxMessage();

    repository.create(message);

    assertSame(message, repository.persisted);
  }

  @Test
  public void testListPendingShouldApplyQueryAndPaging() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    OutboxMessage first = new OutboxMessage();
    OutboxMessage second = new OutboxMessage();
    repository.pendingResult = List.of(first, second);
    LocalDateTime now = LocalDateTime.of(2026, 2, 25, 12, 45);

    List<OutboxMessage> result = repository.listPending(25, now);

    assertEquals(2, result.size());
    assertSame(first, result.get(0));
    assertEquals("publishedAt is null and nextAttemptAt <= ?1 order by createdAt asc, id asc", repository.lastFindQuery);
    assertEquals(now, repository.lastFindParams[0]);
    assertEquals(0, repository.capturedPageIndex);
    assertEquals(25, repository.capturedPageSize);
  }

  @Test
  public void testMarkPublishedShouldSetFieldsAndMerge() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    OutboxMessage message = new OutboxMessage();
    message.lastError = "previous error";
    LocalDateTime publishedAt = LocalDateTime.of(2026, 2, 25, 13, 0);

    repository.markPublished(message, publishedAt);

    assertEquals(publishedAt, message.publishedAt);
    assertNull(message.lastError);
    assertSame(message, repository.merged);
  }

  @Test
  public void testMarkFailedShouldIncrementAttemptsAndTruncateError() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    OutboxMessage message = new OutboxMessage();
    message.attempts = 2;
    String longError = "x".repeat(2500);
    LocalDateTime retryAt = LocalDateTime.of(2026, 2, 25, 14, 0);

    repository.markFailed(message, longError, retryAt);

    assertEquals(3, message.attempts);
    assertEquals(1900, message.lastError.length());
    assertEquals(retryAt, message.nextAttemptAt);
    assertSame(message, repository.merged);
  }

  @Test
  public void testMarkFailedShouldKeepNullErrorAsNull() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    OutboxMessage message = new OutboxMessage();
    message.attempts = 0;
    LocalDateTime retryAt = LocalDateTime.of(2026, 2, 25, 15, 0);

    repository.markFailed(message, null, retryAt);

    assertEquals(1, message.attempts);
    assertNull(message.lastError);
    assertEquals(retryAt, message.nextAttemptAt);
  }

  @Test
  public void testCountMethodsShouldUseExpectedPredicates() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    repository.countResults.put("publishedAt is null", 4L);
    repository.countResults.put("publishedAt is null and attempts > 0", 3L);
    repository.countResults.put("publishedAt is not null", 9L);

    assertEquals(4L, repository.countPending());
    assertEquals(3L, repository.countFailed());
    assertEquals(9L, repository.countPublished());
    assertEquals("publishedAt is not null", repository.lastCountQuery);
  }

  @Test
  public void testReplayShouldResetPublicationFieldsAndReturnAffectedRows() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    repository.updateResult = 7;
    LocalDateTime from = LocalDateTime.of(2026, 2, 20, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 2, 22, 10, 0);

    int affected = repository.replay("BU-77", from, to);

    assertEquals(7, affected);
    assertTrue(repository.lastUpdateQuery.contains("publishedAt = null"));
    assertEquals("BU-77", repository.lastUpdateParams[1]);
    assertEquals(from, repository.lastUpdateParams[2]);
    assertEquals(to, repository.lastUpdateParams[3]);
  }

  @Test
  public void testClearAllShouldDeleteAllRows() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();

    repository.clearAll();

    assertTrue(repository.deleteAllCalled);
  }

  @Test
  public void testSetNextAttemptAtShouldCallUpdateQuery() {
    TestOutboxMessageRepository repository = new TestOutboxMessageRepository();
    LocalDateTime nextAttemptAt = LocalDateTime.of(2026, 2, 25, 16, 0);

    repository.setNextAttemptAt(123L, nextAttemptAt);

    assertEquals("nextAttemptAt = ?1 where id = ?2", repository.lastUpdateQuery);
    assertEquals(nextAttemptAt, repository.lastUpdateParams[0]);
    assertEquals(123L, repository.lastUpdateParams[1]);
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(type)) {
      return false;
    }
    if (byte.class.equals(type) || short.class.equals(type) || int.class.equals(type)) {
      return 0;
    }
    if (long.class.equals(type)) {
      return 0L;
    }
    if (float.class.equals(type)) {
      return 0f;
    }
    if (double.class.equals(type)) {
      return 0d;
    }
    if (char.class.equals(type)) {
      return '\0';
    }
    return null;
  }
}

package com.fulfilment.application.monolith.stores.outbox;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class OutboxMessageRepository implements PanacheRepository<OutboxMessage> {

  @Transactional
  public void create(OutboxMessage message) {
    persist(message);
  }

  public List<OutboxMessage> listPending(int limit, LocalDateTime now) {
    return find(
            "publishedAt is null and nextAttemptAt <= ?1 order by createdAt asc, id asc",
            now)
        .page(0, limit)
        .list();
  }

  @Transactional
  public void markPublished(OutboxMessage message, LocalDateTime publishedAt) {
    message.publishedAt = publishedAt;
    message.lastError = null;
    getEntityManager().merge(message);
  }

  @Transactional
  public void markFailed(OutboxMessage message, String lastError, LocalDateTime nextAttemptAt) {
    message.attempts = message.attempts + 1;
    message.lastError = truncate(lastError);
    message.nextAttemptAt = nextAttemptAt;
    getEntityManager().merge(message);
  }

  public long countPending() {
    return count("publishedAt is null");
  }

  public long countFailed() {
    return count("publishedAt is null and attempts > 0");
  }

  public long countPublished() {
    return count("publishedAt is not null");
  }

  @Transactional
  public int replay(String aggregateId, LocalDateTime from, LocalDateTime to) {
    return update(
        "publishedAt = null, nextAttemptAt = ?1, attempts = 0, lastError = null "
            + "where aggregateId = ?2 and createdAt >= ?3 and createdAt <= ?4",
        LocalDateTime.now(),
        aggregateId,
        from,
        to);
  }

  @Transactional
  public void clearAll() {
    deleteAll();
  }

  @Transactional
  public void setNextAttemptAt(Long id, LocalDateTime nextAttemptAt) {
    update("nextAttemptAt = ?1 where id = ?2", nextAttemptAt, id);
  }

  private String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > 1900 ? value.substring(0, 1900) : value;
  }
}

package com.fulfilment.application.monolith.stores.outbox;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "outbox_message",
    indexes = {
      @Index(name = "idx_outbox_pending", columnList = "publishedAt,nextAttemptAt"),
      @Index(name = "idx_outbox_aggregate", columnList = "aggregateId,createdAt")
    })
public class OutboxMessage extends PanacheEntity {

  @Column(nullable = false, unique = true, length = 100)
  public String eventId;

  @Column(nullable = false, length = 80)
  public String aggregateType;

  @Column(nullable = false, length = 80)
  public String aggregateId;

  @Column(nullable = true)
  public Long aggregateVersion;

  @Column(nullable = false, length = 80)
  public String eventType;

  @Column(nullable = false)
  public Integer schemaVersion;

  @Column(nullable = true, length = 100)
  public String correlationId;

  @Column(nullable = false, columnDefinition = "TEXT")
  public String payloadJson;

  @Column(nullable = false)
  public LocalDateTime createdAt;

  @Column(nullable = true)
  public LocalDateTime publishedAt;

  @Column(nullable = false)
  public Integer attempts;

  @Column(nullable = true, length = 2000)
  public String lastError;

  @Column(nullable = false)
  public LocalDateTime nextAttemptAt;
}

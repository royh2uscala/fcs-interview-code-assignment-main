package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.testinfra.ReusablePostgresTestResource;
import com.fulfilment.application.monolith.stores.outbox.OutboxMessage;
import com.fulfilment.application.monolith.stores.outbox.OutboxMessageRepository;
import com.fulfilment.application.monolith.stores.outbox.OutboxPublisher;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(value = ReusablePostgresTestResource.class, restrictToAnnotatedClass = true)
@Tag("e2e")
public class StoreOutboxTest {

  @jakarta.inject.Inject OutboxMessageRepository outboxMessageRepository;
  @jakarta.inject.Inject OutboxPublisher outboxPublisher;
  @jakarta.inject.Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  public void resetGatewayState() {
    outboxMessageRepository.clearAll();
    legacyStoreManagerGateway.clearTestState();
  }

  @Test
  public void testStoreCreateShouldWriteOutboxAndPublishOnlyAfterRelayRuns() {
    String name = "OUTBOX-" + shortSuffix();
    long beforePending = outboxMessageRepository.countPending();

    Long storeId =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "quantityProductsInStock", 12))
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    assertEquals(beforePending + 1, outboxMessageRepository.countPending());
    assertEquals(0, legacyStoreManagerGateway.processedEventsCount());

    outboxPublisher.publishPending();

    OutboxMessage createdEvent =
        outboxMessageRepository.find("aggregateId = ?1", String.valueOf(storeId)).firstResult();
    assertNotNull(createdEvent);
    assertNotNull(createdEvent.publishedAt);
    assertEquals(1, legacyStoreManagerGateway.processedEventsCount());
  }

  @Test
  public void testStoreRollbackShouldNotPersistOutboxEvent() {
    long beforeCount = outboxMessageRepository.count();

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "TONSTAD", "quantityProductsInStock", 11))
        .when()
        .post("/store")
        .then()
        .statusCode(500);

    assertEquals(beforeCount, outboxMessageRepository.count());
  }

  @Test
  public void testOutboxPublisherShouldRetryAfterLegacyFailure() {
    String name = "OUTBOX-RETRY-" + shortSuffix();

    Long storeId =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "quantityProductsInStock", 13))
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    legacyStoreManagerGateway.failNextPublication();
    outboxPublisher.publishPending();

    OutboxMessage failedMessage =
        outboxMessageRepository.find("aggregateId = ?1", String.valueOf(storeId)).firstResult();
    assertNotNull(failedMessage);
    assertNull(failedMessage.publishedAt);
    assertTrue(failedMessage.attempts >= 1);

    outboxMessageRepository.setNextAttemptAt(failedMessage.id, LocalDateTime.now().minusSeconds(1));
    outboxPublisher.publishPending();
    OutboxMessage publishedMessage =
        outboxMessageRepository.find("aggregateId = ?1", String.valueOf(storeId)).firstResult();
    assertNotNull(publishedMessage.publishedAt);
    assertEquals(1, legacyStoreManagerGateway.processedEventsCount());

    outboxPublisher.publishPending();
    assertEquals(1, legacyStoreManagerGateway.processedEventsCount());
  }

  private String shortSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}

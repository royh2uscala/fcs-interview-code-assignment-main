package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.testinfra.ReusablePostgresTestResource;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(value = ReusablePostgresTestResource.class, restrictToAnnotatedClass = true)
@Tag("e2e")
public class WarehouseEndpointTest {

  @jakarta.inject.Inject WarehouseRepository warehouseRepository;

  private ExecutorService executorService;

  @BeforeEach
  public void resetWarehouseState() {
    warehouseRepository.clearTestWarehouses();
  }

  @AfterEach
  public void cleanupExecutor() {
    if (executorService != null) {
      executorService.shutdownNow();
      executorService = null;
    }
  }

  @Test
  public void testSimpleListWarehouses() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testGetWarehouseByIdShouldSupportHappyAndInvalidPaths() {
    String businessUnitCode = "MWH.GET." + UUID.randomUUID().toString().substring(0, 8);
    String warehouseId = createWarehouse(businessUnitCode, "EINDHOVEN-001", 20, 10, 200);

    given().when().get("/warehouse/" + warehouseId).then().statusCode(200).body(containsString(businessUnitCode));

    given().when().get("/warehouse/NOT_NUMERIC").then().statusCode(400);

    given().when().get("/warehouse/9999999").then().statusCode(404);
  }

  @Test
  public void testCreateWarehouseAndArchiveWarehouse() {
    String businessUnitCode = "MWH.NEW." + UUID.randomUUID().toString().substring(0, 8);
    String warehouseId = createWarehouse(businessUnitCode, "EINDHOVEN-001", 25, 10, 200);

    given().when().delete("/warehouse/" + warehouseId).then().statusCode(204);

    given().when().get("/warehouse/" + warehouseId).then().statusCode(404);

    given().when().get("/warehouse").then().statusCode(200).body(not(containsString(businessUnitCode)));
  }

  @Test
  public void testCreateWarehouseShouldFailWhenBusinessUnitCodeAlreadyExists() {
    createWarehouse("MWH.001", "EINDHOVEN-001", 20, 10, 409);
  }

  @Test
  public void testCreateWarehouseShouldFailWhenLocationIsInvalid() {
    createWarehouse("MWH.BADLOC." + UUID.randomUUID().toString().substring(0, 8), "INVALID-001", 20, 10, 400);
  }

  @Test
  public void testCreateWarehouseShouldFailWhenCapacityLowerThanStock() {
    createWarehouse(
        "MWH.CAPLOW." + UUID.randomUUID().toString().substring(0, 8), "EINDHOVEN-001", 5, 10, 400);
  }

  @Test
  public void testCreateWarehouseShouldFailWhenLocationCapacityExceeded() {
    createWarehouse(
        "MWH.CAPMAX." + UUID.randomUUID().toString().substring(0, 8), "ZWOLLE-002", 100, 10, 400);
  }

  @Test
  public void testCreateWarehouseShouldFailWhenLocationMaxWarehousesReached() {
    String first = "MWH.LOCMAX.A." + UUID.randomUUID().toString().substring(0, 6);
    String second = "MWH.LOCMAX.B." + UUID.randomUUID().toString().substring(0, 6);

    createWarehouse(first, "HELMOND-001", 20, 10, 200);
    createWarehouse(second, "HELMOND-001", 20, 10, 409);
  }

  @Test
  public void testReplaceWarehouseSuccessAndValidationFailure() {
    String businessUnitCode = "MWH.REPLACE." + UUID.randomUUID().toString().substring(0, 8);

    createWarehouse(businessUnitCode, "VETSBY-001", 20, 8, 200);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("location", "AMSTERDAM-002", "capacity", 30, "stock", 7))
        .when()
        .post("/warehouse/" + businessUnitCode + "/replacement")
        .then()
        .statusCode(400);

    String replacedLocation =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("location", "AMSTERDAM-002", "capacity", 30, "stock", 8))
            .when()
            .post("/warehouse/" + businessUnitCode + "/replacement")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("location");

    assertEquals("AMSTERDAM-002", replacedLocation);
    assertEquals(1L, warehouseRepository.countActiveByBusinessUnitCode(businessUnitCode));
  }

  @Test
  public void testReplaceWarehouseShouldReturnNotFoundWhenMissingBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("location", "AMSTERDAM-002", "capacity", 30, "stock", 8))
        .when()
        .post("/warehouse/MWH.UNKNOWN.REPLACE/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void testArchiveWarehouseShouldValidateIdInputAndNotFound() {
    given().when().delete("/warehouse/NOT_NUMERIC").then().statusCode(400);
    given().when().delete("/warehouse/9999999").then().statusCode(404);
  }

  @Test
  public void testConcurrentCreateWithSameBusinessUnitCodeShouldAllowOnlyOneActiveWarehouse() throws Exception {
    String businessUnitCode = "MWH.CONCURRENT.BU." + UUID.randomUUID().toString().substring(0, 8);
    CountDownLatch start = new CountDownLatch(1);
    executorService = Executors.newFixedThreadPool(2);

    Future<Integer> first =
        executorService.submit(() -> concurrentCreate(start, businessUnitCode, "EINDHOVEN-001", 20, 10));
    Future<Integer> second =
        executorService.submit(() -> concurrentCreate(start, businessUnitCode, "EINDHOVEN-001", 20, 10));

    start.countDown();
    int s1 = first.get(15, TimeUnit.SECONDS);
    int s2 = second.get(15, TimeUnit.SECONDS);

    List<Integer> statuses = List.of(s1, s2);
    assertTrue(statuses.stream().allMatch(status -> status == 200 || status == 409), "statuses=" + statuses);
    assertTrue(
        statuses.stream().filter(status -> status == 200).count() <= 1,
        "at most one request should succeed for same business unit");
    assertTrue(
        warehouseRepository.countActiveByBusinessUnitCode(businessUnitCode) <= 1,
        "active records by business unit must not exceed one");
  }

  @Test
  public void testConcurrentCreateAtLocationLimitShouldNotExceedConfiguredMax() throws Exception {
    String first = "MWH.CONCURRENT.LOC.A." + UUID.randomUUID().toString().substring(0, 6);
    String second = "MWH.CONCURRENT.LOC.B." + UUID.randomUUID().toString().substring(0, 6);
    CountDownLatch start = new CountDownLatch(1);
    executorService = Executors.newFixedThreadPool(2);

    Future<Integer> f1 = executorService.submit(() -> concurrentCreate(start, first, "HELMOND-001", 20, 10));
    Future<Integer> f2 = executorService.submit(() -> concurrentCreate(start, second, "HELMOND-001", 20, 10));

    start.countDown();
    int s1 = f1.get(15, TimeUnit.SECONDS);
    int s2 = f2.get(15, TimeUnit.SECONDS);

    List<Integer> statuses = List.of(s1, s2);
    assertTrue(statuses.stream().allMatch(status -> status == 200 || status == 409), "statuses=" + statuses);
    assertTrue(
        statuses.stream().filter(status -> status == 200).count() <= 1,
        "location max=1 should allow at most one success");
    assertTrue(
        warehouseRepository.countActiveByLocation("HELMOND-001") <= 1,
        "active warehouses at HELMOND-001 must not exceed configured max");
  }

  private int concurrentCreate(
      CountDownLatch start,
      String businessUnitCode,
      String location,
      int capacity,
      int stock)
      throws InterruptedException {
    start.await(15, TimeUnit.SECONDS);
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "businessUnitCode", businessUnitCode,
                "location", location,
                "capacity", capacity,
                "stock", stock))
        .when()
        .post("/warehouse")
        .then()
        .extract()
        .statusCode();
  }

  private String createWarehouse(
      String businessUnitCode, String location, int capacity, int stock, int expectedStatus) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "businessUnitCode", businessUnitCode,
                "location", location,
                "capacity", capacity,
                "stock", stock))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(expectedStatus)
        .extract()
        .jsonPath()
        .getString("id");
  }
}

package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;

import com.fulfilment.application.monolith.testinfra.ReusablePostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(value = ReusablePostgresTestResource.class, restrictToAnnotatedClass = true)
@Tag("e2e")
public class FulfillmentAssignmentTest {

  @Test
  public void testMaxTwoWarehousesPerProductPerStoreConstraint() {
    Long storeId = createStore();
    Long productId = createProduct();

    createAssignment(storeId, productId, "MWH.001", 200);
    createAssignment(storeId, productId, "MWH.012", 200);
    createAssignment(storeId, productId, "MWH.023", 409);
  }

  @Test
  public void testMaxThreeWarehousesPerStoreConstraint() {
    Long storeId = createStore();
    Long productA = createProduct();
    Long productB = createProduct();
    Long productC = createProduct();
    Long productD = createProduct();
    String extraWarehouseCode = createWarehouse("EINDHOVEN-001", 20, 5);

    createAssignment(storeId, productA, "MWH.001", 200);
    createAssignment(storeId, productB, "MWH.012", 200);
    createAssignment(storeId, productC, "MWH.023", 200);
    createAssignment(storeId, productD, extraWarehouseCode, 409);
  }

  @Test
  public void testMaxFiveProductsPerWarehouseConstraint() {
    Long storeId = createStore();
    String warehouseCode = createWarehouse("AMSTERDAM-001", 20, 1);

    Long p1 = createProduct();
    Long p2 = createProduct();
    Long p3 = createProduct();
    Long p4 = createProduct();
    Long p5 = createProduct();
    Long p6 = createProduct();

    createAssignment(storeId, p1, warehouseCode, 200);
    createAssignment(storeId, p2, warehouseCode, 200);
    createAssignment(storeId, p3, warehouseCode, 200);
    createAssignment(storeId, p4, warehouseCode, 200);
    createAssignment(storeId, p5, warehouseCode, 200);
    createAssignment(storeId, p6, warehouseCode, 409);
  }

  private Long createStore() {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "STORE-" + shortSuffix(), "quantityProductsInStock", 10))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private Long createProduct() {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "PRODUCT-" + shortSuffix(), "stock", 1))
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private String createWarehouse(String location, int capacity, int stock) {
    String businessUnitCode = "MWH.BONUS." + UUID.randomUUID().toString().substring(0, 8);
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
        .statusCode(200)
        .extract()
        .jsonPath()
        .getString("businessUnitCode");
  }

  private void createAssignment(Long storeId, Long productId, String warehouseCode, int expectedStatus) {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "storeId", storeId,
                "productId", productId,
                "warehouseBusinessUnitCode", warehouseCode))
        .when()
        .post("/fulfillment/assignment")
        .then()
        .statusCode(expectedStatus);
  }

  private String shortSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}

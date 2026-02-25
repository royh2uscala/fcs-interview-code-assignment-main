package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FulfillmentAssignmentResourceTest {

  private static class StubService extends FulfillmentAssignmentService {
    List<FulfillmentAssignment> assignments = List.of();
    FulfillmentAssignment returnedAssignment;
    RuntimeException toThrow;
    Long capturedStoreId;
    Long capturedProductId;
    String capturedWarehouseBusinessUnitCode;

    @Override
    public List<FulfillmentAssignment> listAll() {
      return assignments;
    }

    @Override
    public FulfillmentAssignment assign(Long storeId, Long productId, String warehouseBusinessUnitCode) {
      capturedStoreId = storeId;
      capturedProductId = productId;
      capturedWarehouseBusinessUnitCode = warehouseBusinessUnitCode;
      if (toThrow != null) {
        throw toThrow;
      }
      return returnedAssignment;
    }
  }

  @Test
  public void testListAllShouldDelegateToService() {
    FulfillmentAssignmentResource resource = new FulfillmentAssignmentResource();
    StubService service = new StubService();
    FulfillmentAssignment assignment = new FulfillmentAssignment();
    assignment.id = 1L;
    service.assignments = List.of(assignment);
    resource.service = service;

    List<FulfillmentAssignment> result = resource.listAll();

    assertEquals(1, result.size());
    assertSame(assignment, result.get(0));
  }

  @Test
  public void testCreateShouldDelegateAndReturnAssignment() {
    FulfillmentAssignmentResource resource = new FulfillmentAssignmentResource();
    StubService service = new StubService();
    FulfillmentAssignment assignment = new FulfillmentAssignment();
    assignment.id = 10L;
    service.returnedAssignment = assignment;
    resource.service = service;

    FulfillmentAssignmentResource.FulfillmentAssignmentRequest request =
        new FulfillmentAssignmentResource.FulfillmentAssignmentRequest();
    request.storeId = 11L;
    request.productId = 22L;
    request.warehouseBusinessUnitCode = "MWH.001";

    FulfillmentAssignment result = resource.create(request);

    assertSame(assignment, result);
    assertEquals(11L, service.capturedStoreId);
    assertEquals(22L, service.capturedProductId);
    assertEquals("MWH.001", service.capturedWarehouseBusinessUnitCode);
  }

  @Test
  public void testCreateShouldMapIllegalArgumentExceptionToBadRequest() {
    FulfillmentAssignmentResource resource = new FulfillmentAssignmentResource();
    StubService service = new StubService();
    service.toThrow = new IllegalArgumentException("invalid input");
    resource.service = service;

    FulfillmentAssignmentResource.FulfillmentAssignmentRequest request =
        new FulfillmentAssignmentResource.FulfillmentAssignmentRequest();
    request.storeId = 1L;
    request.productId = 2L;
    request.warehouseBusinessUnitCode = "MWH.001";

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(request));

    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("invalid input", ex.getMessage());
  }

  @Test
  public void testCreateShouldMapIllegalStateExceptionToConflict() {
    FulfillmentAssignmentResource resource = new FulfillmentAssignmentResource();
    StubService service = new StubService();
    service.toThrow = new IllegalStateException("rule violated");
    resource.service = service;

    FulfillmentAssignmentResource.FulfillmentAssignmentRequest request =
        new FulfillmentAssignmentResource.FulfillmentAssignmentRequest();
    request.storeId = 1L;
    request.productId = 2L;
    request.warehouseBusinessUnitCode = "MWH.001";

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(request));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("rule violated", ex.getMessage());
  }
}

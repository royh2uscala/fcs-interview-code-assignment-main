package com.fulfilment.application.monolith.fulfillment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("fulfillment/assignment")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentAssignmentResource {

  @Inject FulfillmentAssignmentService service;

  @GET
  public List<FulfillmentAssignment> listAll() {
    return service.listAll();
  }

  @POST
  @Transactional
  public FulfillmentAssignment create(FulfillmentAssignmentRequest request) {
    try {
      return service.assign(request.storeId, request.productId, request.warehouseBusinessUnitCode);
    } catch (IllegalArgumentException ex) {
      throw new WebApplicationException(ex.getMessage(), 400);
    } catch (IllegalStateException ex) {
      throw new WebApplicationException(ex.getMessage(), 409);
    }
  }

  public static class FulfillmentAssignmentRequest {
    public Long storeId;
    public Long productId;
    public String warehouseBusinessUnitCode;
  }
}

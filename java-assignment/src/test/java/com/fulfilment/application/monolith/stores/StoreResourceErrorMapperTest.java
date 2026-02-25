package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class StoreResourceErrorMapperTest {

  @Test
  public void testToResponseShouldMapWebApplicationExceptionStatusAndMessage() {
    StoreResource.ErrorMapper errorMapper = new StoreResource.ErrorMapper();
    errorMapper.objectMapper = new ObjectMapper();

    Response response = errorMapper.toResponse(new WebApplicationException("validation failed", 422));

    assertEquals(422, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(WebApplicationException.class.getName(), body.get("exceptionType").asText());
    assertEquals(422, body.get("code").asInt());
    assertEquals("validation failed", body.get("error").asText());
  }

  @Test
  public void testToResponseShouldMapGenericExceptionTo500() {
    StoreResource.ErrorMapper errorMapper = new StoreResource.ErrorMapper();
    errorMapper.objectMapper = new ObjectMapper();

    Response response = errorMapper.toResponse(new IllegalArgumentException("boom"));

    assertEquals(500, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(IllegalArgumentException.class.getName(), body.get("exceptionType").asText());
    assertEquals(500, body.get("code").asInt());
    assertEquals("boom", body.get("error").asText());
  }

  @Test
  public void testToResponseShouldOmitErrorFieldWhenMessageIsNull() {
    StoreResource.ErrorMapper errorMapper = new StoreResource.ErrorMapper();
    errorMapper.objectMapper = new ObjectMapper();

    Response response = errorMapper.toResponse(new RuntimeException((String) null));

    assertEquals(500, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(RuntimeException.class.getName(), body.get("exceptionType").asText());
    assertEquals(500, body.get("code").asInt());
    assertNull(body.get("error"));
  }
}


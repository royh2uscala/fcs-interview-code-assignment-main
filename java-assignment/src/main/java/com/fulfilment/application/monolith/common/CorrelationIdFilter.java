package com.fulfilment.application.monolith.common;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

  @Inject CorrelationIdContext correlationIdContext;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    correlationIdContext.setCorrelationId(correlationId);
    MDC.put("correlationId", correlationId);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    String correlationId = correlationIdContext.getCorrelationId();
    if (correlationId != null) {
      responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);
    }
    MDC.remove("correlationId");
  }
}

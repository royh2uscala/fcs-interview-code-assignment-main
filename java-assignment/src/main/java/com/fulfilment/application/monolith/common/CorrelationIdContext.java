package com.fulfilment.application.monolith.common;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CorrelationIdContext {

  private String correlationId;

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
}

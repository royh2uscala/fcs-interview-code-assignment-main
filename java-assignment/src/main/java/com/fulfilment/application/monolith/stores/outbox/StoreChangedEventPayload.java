package com.fulfilment.application.monolith.stores.outbox;

public class StoreChangedEventPayload {

  public Long storeId;
  public String name;
  public Integer quantityProductsInStock;

  public StoreChangedEventPayload() {}

  public StoreChangedEventPayload(Long storeId, String name, Integer quantityProductsInStock) {
    this.storeId = storeId;
    this.name = name;
    this.quantityProductsInStock = quantityProductsInStock;
  }
}

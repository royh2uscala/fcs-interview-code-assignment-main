package com.fulfilment.application.monolith.stores;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class StoreGateway {

  public List<Store> listAllByName() {
    return Store.listAll(Sort.by("name"));
  }

  public Store findById(Long id) {
    return Store.findById(id);
  }

  public void persist(Store store) {
    store.persist();
  }

  public void delete(Store store) {
    store.delete();
  }
}

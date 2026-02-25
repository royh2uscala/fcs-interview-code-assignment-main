package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ProductResourceUnitTest {

  private static class FakeProductRepository extends ProductRepository {
    final List<Product> products = new ArrayList<>();
    Product byId;
    Product persisted;
    Product deleted;
    Sort lastSort;
    Long lastFindId;

    @Override
    public List<Product> listAll(Sort sort) {
      lastSort = sort;
      return products;
    }

    @Override
    public Product findById(Long id) {
      lastFindId = id;
      return byId;
    }

    @Override
    public void persist(Product entity) {
      persisted = entity;
    }

    @Override
    public void delete(Product entity) {
      deleted = entity;
    }
  }

  @Test
  public void testGetShouldReturnAllProducts() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    Product p1 = new Product("P-1");
    Product p2 = new Product("P-2");
    repository.products.add(p1);
    repository.products.add(p2);
    resource.productRepository = repository;

    List<Product> result = resource.get();

    assertEquals(2, result.size());
    assertSame(p1, result.get(0));
    assertSame(p2, result.get(1));
    assertNotNull(repository.lastSort);
  }

  @Test
  public void testGetSingleShouldThrowWhenMissing() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(10L));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Product with id of 10 does not exist.", ex.getMessage());
    assertEquals(10L, repository.lastFindId);
  }

  @Test
  public void testGetSingleShouldReturnEntityWhenFound() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    Product product = new Product("Existing");
    product.id = 11L;
    repository.byId = product;
    resource.productRepository = repository;

    Product result = resource.getSingle(11L);

    assertSame(product, result);
  }

  @Test
  public void testCreateShouldRejectPreSetId() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;
    Product product = new Product("Invalid");
    product.id = 99L;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(product));

    assertEquals(422, ex.getResponse().getStatus());
    assertEquals("Id was invalidly set on request.", ex.getMessage());
  }

  @Test
  public void testCreateShouldPersistAndReturnCreated() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;
    Product product = new Product("Created");

    Response response = resource.create(product);

    assertEquals(201, response.getStatus());
    assertSame(product, repository.persisted);
    assertSame(product, response.getEntity());
  }

  @Test
  public void testUpdateShouldRejectMissingName() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;
    Product input = new Product();
    input.name = null;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(33L, input));

    assertEquals(422, ex.getResponse().getStatus());
    assertEquals("Product Name was not set on request.", ex.getMessage());
  }

  @Test
  public void testUpdateShouldThrowWhenEntityMissing() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;
    Product input = new Product("Update");

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(44L, input));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Product with id of 44 does not exist.", ex.getMessage());
  }

  @Test
  public void testUpdateShouldMutateAndPersistEntity() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    Product existing = new Product("Old");
    existing.id = 55L;
    existing.description = "old";
    existing.price = new BigDecimal("10.50");
    existing.stock = 1;
    repository.byId = existing;
    resource.productRepository = repository;

    Product input = new Product("New");
    input.description = "new";
    input.price = new BigDecimal("99.99");
    input.stock = 12;

    Product result = resource.update(55L, input);

    assertSame(existing, result);
    assertEquals("New", existing.name);
    assertEquals("new", existing.description);
    assertEquals(new BigDecimal("99.99"), existing.price);
    assertEquals(12, existing.stock);
    assertSame(existing, repository.persisted);
  }

  @Test
  public void testDeleteShouldThrowWhenEntityMissing() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    resource.productRepository = repository;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.delete(77L));

    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Product with id of 77 does not exist.", ex.getMessage());
  }

  @Test
  public void testDeleteShouldRemoveEntityAndReturnNoContent() {
    ProductResource resource = new ProductResource();
    FakeProductRepository repository = new FakeProductRepository();
    Product existing = new Product("Delete");
    existing.id = 88L;
    repository.byId = existing;
    resource.productRepository = repository;

    Response response = resource.delete(88L);

    assertEquals(204, response.getStatus());
    assertSame(existing, repository.deleted);
  }
}

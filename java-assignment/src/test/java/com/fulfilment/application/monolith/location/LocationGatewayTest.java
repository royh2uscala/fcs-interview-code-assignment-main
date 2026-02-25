package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testWhenResolveByIdentifierWithNullShouldReturnNull() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier(null);

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveByIdentifierWithBlankShouldReturnNull() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("   ");

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveUnknownLocationShouldReturnNull() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("UNKNOWN-001");

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveIdentifierWithSpacesAndDifferentCaseShouldReturnLocation() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("  zwolle-001  ");

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }
}

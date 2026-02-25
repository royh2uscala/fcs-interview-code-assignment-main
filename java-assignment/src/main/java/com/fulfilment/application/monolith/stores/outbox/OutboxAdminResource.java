package com.fulfilment.application.monolith.stores.outbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.Map;

@Path("admin/outbox")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OutboxAdminResource {

  @Inject OutboxMessageRepository outboxMessageRepository;
  @Inject OutboxPublisher outboxPublisher;
  @Inject OutboxPublisherMetrics metrics;

  @GET
  @Path("stats")
  public Map<String, Object> stats() {
    return Map.of(
        "pendingCount", outboxMessageRepository.countPending(),
        "failedCount", outboxMessageRepository.countFailed(),
        "publishedCount", outboxMessageRepository.countPublished(),
        "relayPublishedCount", metrics.getPublishedCount(),
        "relayFailureCount", metrics.getFailedCount(),
        "relayAveragePublishLatencyMs", metrics.getAveragePublishLatencyMs());
  }

  @POST
  @Path("publish")
  public Map<String, Object> publishNow() {
    int processed = outboxPublisher.publishPending();
    return Map.of("processed", processed);
  }

  @POST
  @Path("replay")
  public Map<String, Object> replay(
      @QueryParam("aggregateId") String aggregateId,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("resetMetrics") @DefaultValue("false") boolean resetMetrics) {
    if (aggregateId == null || aggregateId.isBlank() || from == null || to == null) {
      throw new WebApplicationException("aggregateId, from, and to are required query params", 400);
    }

    LocalDateTime fromTime;
    LocalDateTime toTime;
    try {
      fromTime = LocalDateTime.parse(from);
      toTime = LocalDateTime.parse(to);
    } catch (Exception ex) {
      throw new WebApplicationException("Invalid from/to datetime format. Use ISO-8601 LocalDateTime", 400);
    }

    if (toTime.isBefore(fromTime)) {
      throw new WebApplicationException("to must be greater than or equal to from", 400);
    }

    if (resetMetrics) {
      metrics.reset();
    }

    int affected = outboxMessageRepository.replay(aggregateId, fromTime, toTime);
    return Map.of("affected", affected);
  }
}

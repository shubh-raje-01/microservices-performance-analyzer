package com.analyzer.modules.recommendation.engine;

import com.analyzer.common.constants.MetricsConstants;
import com.analyzer.modules.recommendation.model.Recommendation;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import com.analyzer.modules.recommendation.model.RecommendationSource;
import com.analyzer.modules.simulation.model.Simulation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RuleBasedRecommendationGenerator {

    /**
     * Produces recommendations from threshold comparisons against MetricsConstants.
     * Supplements AI recommendations — especially useful when FastAPI returned a
     * DEGRADED response or produced fewer than three recommendations.
     */
    public List<Recommendation> generate(Simulation sim, ScoringContext ctx) {
        List<Recommendation> recs = new ArrayList<>();

        addLatencyRecommendations(sim, ctx, recs);
        addErrorRateRecommendations(sim, ctx, recs);
        addThroughputRecommendations(sim, ctx, recs);
        addHealthRecommendations(sim, ctx, recs);

        log.debug("RuleBasedGenerator produced {} recommendations for simulation {}",
                recs.size(), sim.getId());
        return recs;
    }

    // ── Latency rules

    private void addLatencyRecommendations (
            Simulation sim,
            ScoringContext ctx,
            List<Recommendation> recs
    ) {
        if (ctx.getP95LatencyMs() == null) return;
        double p95 = ctx.getP95LatencyMs();

        if (p95 >= MetricsConstants.LATENCY_CRITICAL_MS) {

            recs.add(build(sim,
                    RecommendationCategory.LATENCY,
                    RecommendationPriority.HIGH,
                    "Enable distributed caching for hot data paths",
                    String.format(
                            "p95 latency of %.0fms exceeds the %.0fms critical ceiling. " +
                                    "Distributed caching for frequently read data reduces database " +
                                    "round-trips, which typically account for 40–60%% of service latency " +
                                    "under concurrent load.",
                            p95, MetricsConstants.LATENCY_CRITICAL_MS),
                    "Add @Cacheable to high-read methods and configure a Redis TTL that matches " +
                            "your data staleness tolerance. Start with 60 seconds for reference data " +
                            "and 10 seconds for user-facing queries. Monitor cache hit rate via " +
                            "/actuator/metrics/cache.gets.",
                    0.85, 0.45));

            recs.add(build(sim,
                    RecommendationCategory.CONFIGURATION,
                    RecommendationPriority.HIGH,
                    "Tune connection pool to eliminate blocking waits",
                    "Critical p95 latency commonly indicates connection pool exhaustion. " +
                            "When all connections are in use, threads block and queue — inflating " +
                            "p95 and p99 far beyond the actual query execution time.",
                    "Set HikariCP maximumPoolSize = (number_of_cores × 2) + 1. " +
                            "For a 4-core host: maximumPoolSize=9. Set connectionTimeout=3000 " +
                            "(3 seconds) so blocked threads fail fast rather than cascading. " +
                            "Monitor pool contention at /actuator/metrics/hikaricp.connections.pending.",
                    0.80, 0.40));

            recs.add(build(sim,
                    RecommendationCategory.SCALING,
                    RecommendationPriority.MEDIUM,
                    "Add read replicas to distribute query load",
                    String.format(
                            "At %.0fms p95 with %d concurrent users, the primary database is " +
                                    "likely saturated. Read replicas separate read traffic from write " +
                                    "traffic and lower primary CPU utilisation.",
                            p95, sim.getConcurrentUsers()),
                    "Configure Spring's AbstractRoutingDataSource to route all " +
                            "@Transactional(readOnly=true) calls to a read replica. " +
                            "Ensure replica lag stays under 500ms. Do not route writes or " +
                            "read-after-write patterns to replicas.",
                    0.70, 0.30));

        } else if (p95 >= MetricsConstants.LATENCY_WARN_MS) {

            recs.add(build(sim,
                    RecommendationCategory.LATENCY,
                    RecommendationPriority.MEDIUM,
                    "Audit and resolve N+1 query patterns",
                    String.format(
                            "p95 latency of %.0fms is approaching the %.0fms warning threshold. " +
                                    "N+1 query patterns are the most common cause of latency growth under " +
                                    "concurrency — each additional user multiplies the query count.",
                            p95, MetricsConstants.LATENCY_WARN_MS),
                    "Enable slow-query logging at the database level (log_min_duration_statement=100ms " +
                            "in PostgreSQL). In Spring, use spring.jpa.show-sql=true to identify requests " +
                            "generating excessive queries. Replace with JOIN FETCH or @BatchSize(size=50).",
                    0.72, 0.28));

            recs.add(build(sim,
                    RecommendationCategory.CONFIGURATION,
                    RecommendationPriority.LOW,
                    "Add response compression for large payloads",
                    "Latency approaching the warning threshold is sometimes caused by large " +
                            "uncompressed response bodies, particularly for list endpoints under " +
                            "concurrent load. Compression reduces network transfer time by 60–80%%.",
                    "Add server.compression.enabled=true, server.compression.min-response-size=1024 " +
                            "to application.yml. Include application/json in mime-types. " +
                            "Verify that downstream consumers accept gzip encoding.",
                    0.55, 0.15));
        }
    }

    // ── Error rate rules

    private void addErrorRateRecommendations (
            Simulation sim,
            ScoringContext ctx,
            List<Recommendation> recs
    ) {
        if (ctx.getAvgErrorRate() == null) return;
        double rate      = ctx.getAvgErrorRate();
        double threshold = sim.getErrorRateThreshold();

        if (rate >= MetricsConstants.ERROR_RATE_CRITICAL) {

            recs.add(build(sim,
                    RecommendationCategory.ERROR_RATE,
                    RecommendationPriority.HIGH,
                    "Implement circuit breaker on all downstream calls",
                    String.format(
                            "Error rate of %.1f%% exceeds the %.0f%% critical threshold. " +
                                    "Without a circuit breaker, downstream failures cascade and thread pools " +
                                    "fill with blocked calls waiting on a failing dependency.",
                            rate * 100, MetricsConstants.ERROR_RATE_CRITICAL * 100),
                    "Annotate all outbound HTTP methods with @CircuitBreaker(name=\"service\", " +
                            "fallbackMethod=\"fallback\"). Configure: failure-rate-threshold=50, " +
                            "sliding-window-size=5, wait-duration-in-open-state=30s. " +
                            "The fallback must return a cached or default response, not throw.",
                    0.92, 0.55));

            recs.add(build(sim,
                    RecommendationCategory.ARCHITECTURE,
                    RecommendationPriority.HIGH,
                    "Add retry with exponential back-off for transient failures",
                    "High error rates under load include transient failures that succeed on " +
                            "retry. Without back-off, retries amplify load on an already-struggling " +
                            "service, worsening the error rate.",
                    "Add @Retry(name=\"service\") with max-attempts=3, wait-duration=1s, " +
                            "exponential-backoff-multiplier=2.0. Limit retryable exceptions to " +
                            "IOException and TimeoutException. Never retry 4xx responses — they " +
                            "indicate client errors that will not resolve on retry.",
                    0.88, 0.42));

        } else if (rate > threshold || rate >= MetricsConstants.ERROR_RATE_WARN) {

            recs.add(build(sim,
                    RecommendationCategory.ERROR_RATE,
                    RecommendationPriority.MEDIUM,
                    "Tighten and standardise timeout configuration",
                    String.format(
                            "Error rate of %.1f%% suggests misconfigured or inconsistent timeouts. " +
                                    "Overly tight timeouts create artificial failures; overly loose timeouts " +
                                    "cause thread exhaustion under concurrent load.",
                            rate * 100),
                    "Set connect-timeout=3000ms and read-timeout=10000ms as starting values. " +
                            "Use WebClient's .timeout(Duration.ofSeconds(10)) per-call rather than " +
                            "a global default — different endpoints have different expected latencies. " +
                            "Alert on p99 approaching 80%% of the configured timeout.",
                    0.68, 0.25));

            recs.add(build(sim,
                    RecommendationCategory.CONFIGURATION,
                    RecommendationPriority.LOW,
                    "Review and sanitise error handling in exception mappers",
                    "Inconsistent exception handling causes benign errors (validation failures, " +
                            "not-found lookups) to inflate the error rate metric alongside genuine " +
                            "failures, masking the true health signal.",
                    "Audit GlobalExceptionHandler. Ensure 4xx responses are not counted as " +
                            "errors in your error-rate metric — only 5xx responses represent actual " +
                            "service failures. Use Spring Actuator's metrics with proper " +
                            "outcome tags (outcome=SERVER_ERROR vs CLIENT_ERROR).",
                    0.58, 0.15));
        }
    }

    // ── Throughput rules

    private void addThroughputRecommendations(
            Simulation sim,
            ScoringContext ctx,
            List<Recommendation> recs
    ) {
        if (ctx.getThroughputRps() == null) return;
        double rps   = ctx.getThroughputRps();
        int    users = sim.getConcurrentUsers();

        if (rps < MetricsConstants.THROUGHPUT_LOW_RPS) {

            recs.add(build(sim,
                    RecommendationCategory.THROUGHPUT,
                    RecommendationPriority.MEDIUM,
                    "Enable async request processing for long-running operations",
                    String.format(
                            "Throughput of %.1f req/s with %d concurrent users is below the " +
                                    "%.0f req/s minimum threshold. Synchronous blocking on long operations " +
                                    "prevents threads from accepting new requests.",
                            rps, users, MetricsConstants.THROUGHPUT_LOW_RPS),
                    "Migrate slow endpoints to @Async with CompletableFuture, or use Spring " +
                            "WebFlux for fully reactive I/O. For batch endpoints, return a 202 " +
                            "Accepted with a job ID and stream results via SSE or polling.",
                    0.74, 0.35));
        }

        // Low throughput relative to user count indicates per-user bottleneck
        if (users >= 100 && rps > 0 && (rps / users) < 0.5) {

            recs.add(build(sim,
                    RecommendationCategory.SCALING,
                    RecommendationPriority.MEDIUM,
                    "Scale horizontally to increase per-user throughput",
                    String.format(
                            "%.1f req/s across %d concurrent users yields %.2f req/s per user — " +
                                    "below the 0.5 req/s/user healthy baseline. The service is CPU or " +
                                    "I/O bound in a way that does not benefit from a single larger instance.",
                            rps, users, rps / users),
                    "Deploy additional instances behind a load balancer. Before scaling, " +
                            "externalise session state to Redis (add Spring Session + spring-session-data-redis). " +
                            "Profile each instance with async-profiler to confirm the bottleneck " +
                            "is compute, not a shared external resource like the database.",
                    0.67, 0.30));
        }
    }

    // ── Health composite rules

    private void addHealthRecommendations (
            Simulation sim,
            ScoringContext ctx,
            List<Recommendation> recs
    ) {
        if (ctx.getHealthStatus() == null) return;

        if ("CRITICAL".equals(ctx.getHealthStatus())) {
            recs.add(build(sim,
                    RecommendationCategory.ARCHITECTURE,
                    RecommendationPriority.HIGH,
                    "Implement bulkhead isolation between service layers",
                    "A CRITICAL health score indicates multiple concurrent failure modes. " +
                            "Without bulkhead isolation, a failure in one dependency exhausts the " +
                            "shared thread pool and brings down all features simultaneously.",
                    "Configure separate Resilience4j ThreadPoolBulkhead instances per dependency. " +
                            "Assign core-thread-pool-size=4, max-thread-pool-size=8 per bulkhead. " +
                            "Use @Bulkhead(name=\"payments\", type=THREADPOOL) on each integration " +
                            "method so a slow payments service cannot impact the orders flow.",
                    0.82, 0.50));
        }

        // Suggest observability improvements when health is degraded or critical
        if ("DEGRADED".equals(ctx.getHealthStatus())
                || "CRITICAL".equals(ctx.getHealthStatus())) {
            recs.add(build(sim,
                    RecommendationCategory.CONFIGURATION,
                    RecommendationPriority.LOW,
                    "Add distributed tracing to identify hotspots",
                    "Degraded health with multiple contributing factors is difficult to diagnose " +
                            "without per-request trace data showing which operation contributes most " +
                            "to latency.",
                    "Add micrometer-tracing-bridge-otel and configure an OTLP exporter pointing " +
                            "to a local Jaeger instance (docker run -p 16686:16686 jaegertracing/all-in-one). " +
                            "Set management.tracing.sampling.probability=1.0 during investigation, " +
                            "then lower to 0.1 in production.",
                    0.60, 0.20));
        }
    }

    // ── Builder helper

    private Recommendation build (
            Simulation sim,
            RecommendationCategory category,
            RecommendationPriority priority,
            String title,
            String description,
            String action,
            double confidenceScore,
            double estimatedImpact
    ) {
        return Recommendation.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .category(category)
                .priority(priority)
                .source(RecommendationSource.RULE_BASED)
                .title(title)
                .description(description)
                .action(action)
                .confidenceScore(confidenceScore)
                .estimatedImpact(estimatedImpact)
                .compositeScore(0.0)  // set by RecommendationScorer
                .rank(0)              // set by RecommendationEngine.rank()
                .build();
    }
}
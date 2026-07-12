from app.core.logging import get_logger
from app.models.request import AnalysisRequest
from app.models.response import (
    AnomalyType,
    Recommendation,
    RecommendationCategory,
    RecommendationPriority,
)

logger = get_logger(__name__)


class RecommendationGenerator:
    """
    Generates recommendations driven by the detected anomaly type and
    feature importance. These are marked AI_MODEL source on the Spring
    Boot side and are merged with — not duplicated against — the
    Java-side rule-based generator.
    """

    def generate(
        self,
        request: AnalysisRequest,
        anomaly_type: AnomalyType,
        anomaly_score: float,
        feature_importance: dict[str, float],
    ) -> list[Recommendation]:
        generators = {
            AnomalyType.LATENCY_SPIKE: self._latency_spike_recs,
            AnomalyType.ERROR_BURST: self._error_burst_recs,
            AnomalyType.THROUGHPUT_DROP: self._throughput_drop_recs,
            AnomalyType.SATURATION: self._saturation_recs,
            AnomalyType.MEMORY_PRESSURE: self._memory_pressure_recs,
            AnomalyType.CASCADING_FAILURE: self._cascading_failure_recs,
        }

        generator = generators.get(anomaly_type)
        if generator is None:
            return []

        recs = generator(request, anomaly_score)

        logger.info(
            "recommendations_generated",
            simulation_id=request.simulation_id,
            anomaly_type=anomaly_type.value,
            count=len(recs),
        )

        return recs

    # ── Per-anomaly recommendation sets ───────────────────────

    def _latency_spike_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        m = request.metrics
        return [
            Recommendation(
                category=RecommendationCategory.LATENCY,
                priority=RecommendationPriority.HIGH,
                title="Profile and eliminate the dominant latency contributor",
                description=(
                    f"p99/p50 tail ratio of {m.tail_latency_ratio:.2f}x indicates a "
                    f"subset of requests are taking disproportionately long. This "
                    f"pattern typically traces to a single slow code path rather than "
                    f"uniform degradation across all requests."
                ),
                action=(
                    "Enable async-profiler with --event=wall to capture wall-clock "
                    "time across all threads during a representative load window. "
                    "Look for a single method consuming >30% of samples — that is "
                    "almost always the tail driver, not aggregate CPU usage."
                ),
                confidence_score=min(0.95, 0.6 + score * 0.4),
                estimated_impact=min(0.6, m.tail_latency_ratio / 10),
            ),
            Recommendation(
                category=RecommendationCategory.SCALING,
                priority=RecommendationPriority.MEDIUM,
                title="Pre-warm connection pools before load arrives",
                description=(
                    "Cold connection establishment (database, cache, downstream "
                    "services) adds fixed latency to the first requests in each "
                    "burst, which compounds the tail under bursty concurrent traffic."
                ),
                action=(
                    "Set spring.datasource.hikari.minimum-idle equal to "
                    "maximum-pool-size to keep the pool fully warm rather than "
                    "growing on demand. Add a @PostConstruct health probe that "
                    "issues a no-op query to force initial connection setup."
                ),
                confidence_score=0.7,
                estimated_impact=0.25,
            ),
        ]

    def _error_burst_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        m = request.metrics
        return [
            Recommendation(
                category=RecommendationCategory.ERROR_RATE,
                priority=RecommendationPriority.HIGH,
                title="Classify and isolate the dominant error type",
                description=(
                    f"Error rate of {m.avg_error_rate * 100:.1f}% with a peak of "
                    f"{m.max_error_rate * 100:.1f}% suggests a concentrated failure "
                    f"mode rather than baseline noise. Mixed error types respond to "
                    f"different fixes — timeouts need different handling than "
                    f"validation failures."
                ),
                action=(
                    "Add a custom Micrometer tag 'error_category' (TIMEOUT, "
                    "CONNECTION_REFUSED, VALIDATION, UPSTREAM_5XX) at the exception "
                    "handler. Query /actuator/metrics/http.server.requests grouped "
                    "by this tag to find the single largest contributor before "
                    "choosing a fix."
                ),
                confidence_score=min(0.92, 0.65 + score * 0.3),
                estimated_impact=min(0.55, m.avg_error_rate * 5),
            ),
        ]

    def _throughput_drop_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        m = request.metrics
        return [
            Recommendation(
                category=RecommendationCategory.THROUGHPUT,
                priority=RecommendationPriority.MEDIUM,
                title="Identify the binding resource constraint",
                description=(
                    f"Throughput of {m.avg_throughput_rps:.1f} rps with "
                    f"{m.throughput_variability:.2f} variability suggests requests "
                    f"are queueing behind a constrained resource — most commonly "
                    f"thread pool size, database connections, or external API "
                    f"rate limits."
                ),
                action=(
                    "Check thread pool queue depth via "
                    "/actuator/metrics/executor.queued. If consistently near "
                    "capacity, the bottleneck is compute-bound; if near zero with "
                    "low throughput, suspect an external dependency rate limit "
                    "or connection pool exhaustion instead."
                ),
                confidence_score=0.68,
                estimated_impact=0.4,
            ),
        ]

    def _saturation_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        return [
            Recommendation(
                category=RecommendationCategory.SCALING,
                priority=RecommendationPriority.HIGH,
                title="Add backpressure before the system saturates further",
                description=(
                    "Per-user throughput well below baseline at this concurrency "
                    "level indicates the service has crossed its effective capacity "
                    "ceiling. Without backpressure, queued requests continue "
                    "accumulating and latency grows unbounded rather than "
                    "throughput plateauing gracefully."
                ),
                action=(
                    "Implement a bounded request queue with explicit rejection "
                    "(HTTP 503 + Retry-After) once queue depth exceeds a "
                    "configured threshold, rather than allowing unbounded "
                    "queueing. This converts a slow death into a fast, "
                    "recoverable failure the client can retry against."
                ),
                confidence_score=0.78,
                estimated_impact=0.45,
            ),
        ]

    def _memory_pressure_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        return [
            Recommendation(
                category=RecommendationCategory.MEMORY,
                priority=RecommendationPriority.MEDIUM,
                title="Investigate GC pause contribution to latency variance",
                description=(
                    "High latency variance under moderate-to-low throughput is a "
                    "classic GC pause signature — stop-the-world collections add "
                    "unpredictable latency spikes independent of request complexity."
                ),
                action=(
                    "Enable -Xlog:gc*:file=gc.log and check for pause durations "
                    "correlating with your observed latency spikes. If using G1GC, "
                    "tune -XX:MaxGCPauseMillis to your p99 SLA target. Consider "
                    "ZGC for services requiring sub-10ms pause times."
                ),
                confidence_score=0.6,
                estimated_impact=0.3,
            ),
        ]

    def _cascading_failure_recs(
        self, request: AnalysisRequest, score: float
    ) -> list[Recommendation]:
        return [
            Recommendation(
                category=RecommendationCategory.ARCHITECTURE,
                priority=RecommendationPriority.HIGH,
                title="Add bulkhead isolation between failing subsystems",
                description=(
                    "Multiple independent anomaly signals firing together "
                    "indicates one subsystem's failure is propagating into others "
                    "through a shared resource — typically a shared thread pool "
                    "or connection pool exhausted by the originating failure."
                ),
                action=(
                    "Map which calls share a thread pool or connection pool. "
                    "Split into per-dependency Resilience4j bulkheads "
                    "(ThreadPoolBulkhead) so saturation in one downstream call "
                    "cannot starve resources needed by unrelated request paths."
                ),
                confidence_score=0.85,
                estimated_impact=0.55,
            ),
        ]
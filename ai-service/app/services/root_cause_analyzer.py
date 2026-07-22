"""
Deterministic, rule-based Root Cause Analyzer.

This engine NEVER hallucinates. Every finding is derived exclusively from
collected metrics via explicit threshold checks and correlation rules.
The output is a ranked list of RcaFinding objects, each containing:
  - bottleneck_type: classified from metrics
  - confidence: 0.0–1.0, computed from how far metrics deviate from healthy ranges
  - reasoning: human-readable explanation built from actual metric values
  - evidence: list of metric snapshots that support the conclusion
  - recommendations: actionable fixes derived from the identified bottleneck

The LLM is NOT involved in root cause determination. It may optionally
polish the reasoning text in a later pipeline step, but the structured
findings are always generated deterministically.
"""

from app.core.logging_config import get_logger
from app.models.rca import (
    BottleneckType,
    EvidenceItem,
    RcaRequest,
    RcaFinding,
    Recommendation,
)

logger = get_logger(__name__)

# ── Healthy-range thresholds ──────────────────────────────────────

CPU_NORMAL_MAX = 0.70
CPU_WARNING_MAX = 0.85
CPU_CRITICAL_THRESHOLD = 0.90

MEMORY_HEAP_WARNING_PERCENT = 75.0
MEMORY_HEAP_CRITICAL_PERCENT = 90.0

GC_PAUSE_WARNING_SECONDS = 0.5
GC_PAUSE_CRITICAL_SECONDS = 2.0
GC_PAUSE_COUNT_WARNING = 100
GC_LIVE_DATA_RATIO_WARNING = 0.80

THREAD_BUSY_WARNING_RATIO = 0.80
THREAD_BUSY_CRITICAL_RATIO = 0.95

CONNECTION_POOL_WARNING_RATIO = 0.80
CONNECTION_POOL_CRITICAL_RATIO = 0.95
CONNECTION_POOL_PENDING_THRESHOLD = 5

LATENCY_WARNING_SECONDS = 0.5
LATENCY_CRITICAL_SECONDS = 2.0
LATENCY_MAX_WARNING_SECONDS = 5.0

HTTP_ERROR_RATE_WARNING = 0.05
HTTP_ERROR_RATE_CRITICAL = 0.10

HEALTH_DEGRADED_STATUS = "DEGRADED"
HEALTH_OFFLINE_STATUS = "OFFLINE"


class RootCauseAnalyzer:
    """
    Rule-based root cause analysis engine. Evaluates metrics against
    defined thresholds and produces ranked findings with evidence.
    """

    def analyze(self, request: RcaRequest) -> list[RcaFinding]:
        findings: list[RcaFinding] = []

        findings.extend(self._check_connection_pool(request))
        findings.extend(self._check_cpu(request))
        findings.extend(self._check_memory(request))
        findings.extend(self._check_gc(request))
        findings.extend(self._check_threads(request))
        findings.extend(self._check_latency(request))
        findings.extend(self._check_http_errors(request))
        findings.extend(self._check_health_status(request))
        findings.extend(self._check_historical_trends(request))

        findings.sort(key=lambda f: f.confidence, reverse=True)

        if not findings:
            findings.append(self._no_bottleneck_finding(request))

        logger.info(
            "rca_analysis_complete",
            service_id=request.service_id,
            finding_count=len(findings),
            primary=findings[0].bottleneck_type.value if findings else "NONE",
        )

        return findings

    # ── Connection Pool ───────────────────────────────────────────

    def _check_connection_pool(self, req: RcaRequest) -> list[RcaFinding]:
        cp = req.connection_pool
        findings = []

        if cp.hikari_max_pool_size <= 0:
            return findings

        utilization = cp.hikari_active / cp.hikari_max_pool_size
        evidence = [
            EvidenceItem(
                metric="hikaricp.connections.active",
                value=cp.hikari_active,
                unit="connections",
                status=self._classify_ratio(utilization, 0.5, 0.8),
                description=f"{cp.hikari_active} of {cp.hikari_max_pool_size} pool connections active",
            ),
            EvidenceItem(
                metric="hikaricp.connections.idle",
                value=cp.hikari_idle,
                unit="connections",
                status="normal" if cp.hikari_idle > 0 else "warning",
                description=f"{cp.hikari_idle} idle connections available",
            ),
            EvidenceItem(
                metric="hikaricp.connections.pending",
                value=cp.hikari_pending,
                unit="threads",
                status=self._classify_value(cp.hikari_pending, 1, CONNECTION_POOL_PENDING_THRESHOLD),
                description=f"{cp.hikari_pending} threads waiting for a connection",
            ),
        ]

        if utilization >= CONNECTION_POOL_CRITICAL_RATIO:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.CONNECTION_POOL_EXHAUSTION,
                confidence=min(0.95, 0.7 + utilization * 0.25),
                reasoning=(
                    f"Connection pool is near exhaustion: {cp.hikari_active} of "
                    f"{cp.hikari_max_pool_size} connections in use "
                    f"({utilization:.0%} utilization). "
                    f"{cp.hikari_pending} threads are blocked waiting for a connection. "
                    f"This is the most likely cause of increased latency and errors."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Increase HikariCP maximum pool size",
                        description=f"Current max pool size is {cp.hikari_max_pool_size}. "
                                    f"Increase to {cp.hikari_max_pool_size * 2} to handle peak load.",
                        priority="HIGH",
                        category="CONFIGURATION",
                        confidence=min(0.9, utilization),
                    ),
                    Recommendation(
                        title="Enable Redis caching to reduce DB load",
                        description="Cache frequently accessed data in Redis to reduce "
                                    "the number of database connections required.",
                        priority="HIGH",
                        category="INFRASTRUCTURE",
                        confidence=0.75,
                    ),
                    Recommendation(
                        title="Optimize slow SQL queries",
                        description="Long-held connections due to slow queries exhaust the pool. "
                                    "Identify and optimize queries taking >100ms.",
                        priority="MEDIUM",
                        category="CODE",
                        confidence=0.7,
                    ),
                    Recommendation(
                        title="Scale the service horizontally",
                        description="Add more service instances to distribute connection load.",
                        priority="MEDIUM",
                        category="SCALING",
                        confidence=0.65,
                    ),
                ],
            ))
        elif utilization >= CONNECTION_POOL_WARNING_RATIO:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.CONNECTION_POOL_EXHAUSTION,
                confidence=min(0.75, 0.5 + utilization * 0.3),
                reasoning=(
                    f"Connection pool utilization is elevated: {cp.hikari_active} of "
                    f"{cp.hikari_max_pool_size} connections in use ({utilization:.0%}). "
                    f"At this rate, the pool will saturate under increased load."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Monitor connection pool saturation trend",
                        description="Pool is approaching capacity. Monitor and prepare to scale.",
                        priority="MEDIUM",
                        category="CONFIGURATION",
                        confidence=0.6,
                    ),
                ],
            ))

        return findings

    # ── CPU ───────────────────────────────────────────────────────

    def _check_cpu(self, req: RcaRequest) -> list[RcaFinding]:
        cpu = req.cpu
        findings = []

        evidence = [
            EvidenceItem(
                metric="system_cpu_usage",
                value=round(cpu.system_cpu_usage, 4),
                unit="ratio",
                status=self._classify_ratio(cpu.system_cpu_usage, CPU_NORMAL_MAX, CPU_CRITICAL_THRESHOLD),
                description=f"System CPU usage at {cpu.system_cpu_usage:.1%}",
            ),
            EvidenceItem(
                metric="process_cpu_usage",
                value=round(cpu.process_cpu_usage, 4),
                unit="ratio",
                status=self._classify_ratio(cpu.process_cpu_usage, CPU_NORMAL_MAX, CPU_CRITICAL_THRESHOLD),
                description=f"Process CPU usage at {cpu.process_cpu_usage:.1%}",
            ),
        ]

        if cpu.system_cpu_usage >= CPU_CRITICAL_THRESHOLD:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.CPU_SATURATION,
                confidence=min(0.95, 0.6 + cpu.system_cpu_usage * 0.4),
                reasoning=(
                    f"System CPU is critically high at {cpu.system_cpu_usage:.1%}. "
                    f"The service is CPU-bound and cannot process requests fast enough. "
                    f"Process-level CPU is {cpu.process_cpu_usage:.1%}."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Profile CPU-hot methods",
                        description="Use async-profiler to identify the hottest code paths.",
                        priority="HIGH",
                        category="CODE",
                        confidence=0.85,
                    ),
                    Recommendation(
                        title="Scale horizontally",
                        description="Add more instances to distribute CPU load.",
                        priority="HIGH",
                        category="SCALING",
                        confidence=0.8,
                    ),
                ],
            ))
        elif cpu.system_cpu_usage >= CPU_WARNING_MAX:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.CPU_SATURATION,
                confidence=min(0.7, 0.4 + cpu.system_cpu_usage * 0.3),
                reasoning=(
                    f"System CPU is elevated at {cpu.system_cpu_usage:.1%}. "
                    f"Approaching saturation threshold."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Monitor CPU trend",
                        description="CPU is elevated. Monitor for continued increase.",
                        priority="MEDIUM",
                        category="CONFIGURATION",
                        confidence=0.5,
                    ),
                ],
            ))

        return findings

    # ── Memory ────────────────────────────────────────────────────

    def _check_memory(self, req: RcaRequest) -> list[RcaFinding]:
        mem = req.memory
        findings = []

        heap_percent = mem.heap_usage_percent
        if heap_percent == 0 and mem.heap_max_bytes > 0:
            heap_percent = (mem.heap_used_bytes / mem.heap_max_bytes) * 100

        evidence = [
            EvidenceItem(
                metric="jvm.heap.usage.percent",
                value=round(heap_percent, 1),
                unit="%",
                status=self._classify_ratio(heap_percent / 100, MEMORY_HEAP_WARNING_PERCENT / 100, MEMORY_HEAP_CRITICAL_PERCENT / 100),
                description=f"Heap usage at {heap_percent:.1f}% of max",
            ),
            EvidenceItem(
                metric="jvm.heap.used",
                value=round(mem.heap_used_bytes),
                unit="bytes",
                status="normal",
                description=f"Heap used: {self._format_bytes(mem.heap_used_bytes)}",
            ),
            EvidenceItem(
                metric="jvm.heap.max",
                value=round(mem.heap_max_bytes),
                unit="bytes",
                status="normal",
                description=f"Heap max: {self._format_bytes(mem.heap_max_bytes)}",
            ),
        ]

        if heap_percent >= MEMORY_HEAP_CRITICAL_PERCENT:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.MEMORY_PRESSURE,
                confidence=min(0.95, 0.65 + (heap_percent / 100) * 0.3),
                reasoning=(
                    f"JVM heap usage is critically high at {heap_percent:.1f}% "
                    f"({self._format_bytes(mem.heap_used_bytes)} of "
                    f"{self._format_bytes(mem.heap_max_bytes)}). "
                    f"This will trigger frequent GC cycles and may lead to OutOfMemoryError."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Increase JVM heap allocation",
                        description=f"Increase -Xmx from current max of "
                                    f"{self._format_bytes(mem.heap_max_bytes)}.",
                        priority="HIGH",
                        category="CONFIGURATION",
                        confidence=0.85,
                    ),
                    Recommendation(
                        title="Investigate memory leak",
                        description="Profile heap with MAT or VisualVM to find retaining objects.",
                        priority="HIGH",
                        category="CODE",
                        confidence=0.75,
                    ),
                ],
            ))
        elif heap_percent >= MEMORY_HEAP_WARNING_PERCENT:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.MEMORY_PRESSURE,
                confidence=min(0.7, 0.4 + (heap_percent / 100) * 0.3),
                reasoning=(
                    f"JVM heap usage is elevated at {heap_percent:.1f}%. "
                    f"Monitor for continued growth."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Monitor heap usage trend",
                        description="Heap is approaching warning threshold.",
                        priority="MEDIUM",
                        category="CONFIGURATION",
                        confidence=0.5,
                    ),
                ],
            ))

        return findings

    # ── GC ────────────────────────────────────────────────────────

    def _check_gc(self, req: RcaRequest) -> list[RcaFinding]:
        gc = req.gc
        findings = []

        evidence = [
            EvidenceItem(
                metric="jvm.gc.pause.sum",
                value=round(gc.gc_pause_sum_seconds, 3),
                unit="seconds",
                status=self._classify_value(gc.gc_pause_sum_seconds, GC_PAUSE_WARNING_SECONDS, GC_PAUSE_CRITICAL_SECONDS),
                description=f"Total GC pause time: {gc.gc_pause_sum_seconds:.3f}s",
            ),
            EvidenceItem(
                metric="jvm.gc.pause.count",
                value=gc.gc_pause_count,
                unit="collections",
                status=self._classify_value(gc.gc_pause_count, 50, GC_PAUSE_COUNT_WARNING),
                description=f"GC collection count: {gc.gc_pause_count}",
            ),
        ]

        if gc.gc_pause_sum_seconds >= GC_PAUSE_CRITICAL_SECONDS:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.GC_PRESSURE,
                confidence=min(0.9, 0.6 + (gc.gc_pause_sum_seconds / 5) * 0.3),
                reasoning=(
                    f"GC pause time is critically high at {gc.gc_pause_sum_seconds:.3f}s "
                    f"across {gc.gc_pause_count} collections. Stop-the-world pauses "
                    f"are causing unpredictable latency spikes."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Tune GC configuration",
                        description="Consider switching to ZGC or tuning G1GC MaxGCPauseMillis.",
                        priority="HIGH",
                        category="CONFIGURATION",
                        confidence=0.8,
                    ),
                    Recommendation(
                        title="Reduce object allocation rate",
                        description="Profile allocation hotspots to reduce GC pressure.",
                        priority="MEDIUM",
                        category="CODE",
                        confidence=0.7,
                    ),
                ],
            ))
        elif gc.gc_pause_sum_seconds >= GC_PAUSE_WARNING_SECONDS:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.GC_PRESSURE,
                confidence=min(0.65, 0.35 + gc.gc_pause_sum_seconds * 0.3),
                reasoning=(
                    f"GC pause time is elevated at {gc.gc_pause_sum_seconds:.3f}s. "
                    f"This may contribute to latency variance."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Monitor GC pause trend",
                        description="GC pauses are increasing. Monitor before they become critical.",
                        priority="MEDIUM",
                        category="CONFIGURATION",
                        confidence=0.5,
                    ),
                ],
            ))

        return findings

    # ── Threads ───────────────────────────────────────────────────

    def _check_threads(self, req: RcaRequest) -> list[RcaFinding]:
        t = req.threads
        findings = []

        if t.tomcat_threads_current <= 0:
            return findings

        busy_ratio = t.tomcat_threads_busy / t.tomcat_threads_current

        evidence = [
            EvidenceItem(
                metric="tomcat.threads.busy",
                value=t.tomcat_threads_busy,
                unit="threads",
                status=self._classify_ratio(busy_ratio, 0.7, THREAD_BUSY_CRITICAL_RATIO),
                description=f"{t.tomcat_threads_busy} of {t.tomcat_threads_current} threads busy",
            ),
            EvidenceItem(
                metric="tomcat.threads.current",
                value=t.tomcat_threads_current,
                unit="threads",
                status="normal",
                description=f"Total thread pool size: {t.tomcat_threads_current}",
            ),
        ]

        if busy_ratio >= THREAD_BUSY_CRITICAL_RATIO:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.THREAD_EXHAUSTION,
                confidence=min(0.9, 0.6 + busy_ratio * 0.35),
                reasoning=(
                    f"Thread pool is near exhaustion: {t.tomcat_threads_busy} of "
                    f"{t.tomcat_threads_current} threads busy ({busy_ratio:.0%}). "
                    f"New requests will queue or be rejected."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Increase thread pool size",
                        description=f"Current pool size is {t.tomcat_threads_current}. "
                                    f"Increase server.tomcat.threads.max.",
                        priority="HIGH",
                        category="CONFIGURATION",
                        confidence=0.8,
                    ),
                    Recommendation(
                        title="Identify slow/blocking threads",
                        description="Use thread dumps to find threads stuck in blocking I/O.",
                        priority="HIGH",
                        category="CODE",
                        confidence=0.75,
                    ),
                ],
            ))
        elif busy_ratio >= THREAD_BUSY_WARNING_RATIO:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.THREAD_EXHAUSTION,
                confidence=min(0.7, 0.4 + busy_ratio * 0.3),
                reasoning=(
                    f"Thread pool utilization is elevated: {t.tomcat_threads_busy} of "
                    f"{t.tomcat_threads_current} threads busy ({busy_ratio:.0%})."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Monitor thread pool saturation",
                        description="Thread pool is approaching capacity.",
                        priority="MEDIUM",
                        category="CONFIGURATION",
                        confidence=0.55,
                    ),
                ],
            ))

        return findings

    # ── Latency ───────────────────────────────────────────────────

    def _check_latency(self, req: RcaRequest) -> list[RcaFinding]:
        lat = req.latency
        findings = []

        evidence = [
            EvidenceItem(
                metric="http.request.duration.avg",
                value=round(lat.avg_duration_seconds, 4),
                unit="seconds",
                status=self._classify_value(lat.avg_duration_seconds, LATENCY_WARNING_SECONDS, LATENCY_CRITICAL_SECONDS),
                description=f"Average response time: {lat.avg_duration_seconds * 1000:.0f}ms",
            ),
            EvidenceItem(
                metric="http.request.duration.max",
                value=round(lat.max_duration_seconds, 4),
                unit="seconds",
                status=self._classify_value(lat.max_duration_seconds, LATENCY_MAX_WARNING_SECONDS, 10.0),
                description=f"Max response time: {lat.max_duration_seconds * 1000:.0f}ms",
            ),
        ]

        if lat.avg_duration_seconds >= LATENCY_CRITICAL_SECONDS:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.NETWORK_LATENCY,
                confidence=min(0.85, 0.55 + lat.avg_duration_seconds * 0.15),
                reasoning=(
                    f"Average response time is critically high at "
                    f"{lat.avg_duration_seconds * 1000:.0f}ms (max: "
                    f"{lat.max_duration_seconds * 1000:.0f}ms). "
                    f"This indicates a downstream dependency or processing bottleneck."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Profile slow endpoints",
                        description="Identify which endpoints have the highest latency.",
                        priority="HIGH",
                        category="CODE",
                        confidence=0.8,
                    ),
                    Recommendation(
                        title="Check downstream dependencies",
                        description="Verify database, cache, and external API response times.",
                        priority="HIGH",
                        category="INFRASTRUCTURE",
                        confidence=0.75,
                    ),
                ],
            ))

        return findings

    # ── HTTP Errors ───────────────────────────────────────────────

    def _check_http_errors(self, req: RcaRequest) -> list[RcaFinding]:
        http = req.http_errors
        findings = []

        error_rate = http.error_rate
        if error_rate == 0 and http.total_requests > 0:
            error_rate = http.error_requests / http.total_requests

        evidence = [
            EvidenceItem(
                metric="http.error_rate",
                value=round(error_rate, 4),
                unit="ratio",
                status=self._classify_ratio(error_rate, HTTP_ERROR_RATE_WARNING, HTTP_ERROR_RATE_CRITICAL),
                description=f"HTTP error rate: {error_rate:.2%} "
                            f"({http.error_requests} of {http.total_requests} requests)",
            ),
        ]

        if error_rate >= HTTP_ERROR_RATE_CRITICAL:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.DOWNSTREAM_DEPENDENCY,
                confidence=min(0.85, 0.55 + error_rate * 3),
                reasoning=(
                    f"HTTP error rate is critically high at {error_rate:.2%} "
                    f"({http.error_requests} failed out of {http.total_requests} total). "
                    f"This typically indicates a failing downstream dependency."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Check downstream service health",
                        description="Verify all downstream services are healthy and responding.",
                        priority="HIGH",
                        category="INFRASTRUCTURE",
                        confidence=0.8,
                    ),
                    Recommendation(
                        title="Add circuit breaker protection",
                        description="Use Resilience4j circuit breakers to prevent cascade failures.",
                        priority="MEDIUM",
                        category="INFRASTRUCTURE",
                        confidence=0.7,
                    ),
                ],
            ))
        elif error_rate >= HTTP_ERROR_RATE_WARNING:
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.DOWNSTREAM_DEPENDENCY,
                confidence=min(0.65, 0.4 + error_rate * 3),
                reasoning=(
                    f"HTTP error rate is elevated at {error_rate:.2%}. "
                    f"Investigate error types for root cause."
                ),
                evidence=evidence,
                recommendations=[
                    Recommendation(
                        title="Classify error types",
                        description="Categorize errors by type (timeout, 5xx, validation).",
                        priority="MEDIUM",
                        category="CODE",
                        confidence=0.6,
                    ),
                ],
            ))

        return findings

    # ── Health Status ─────────────────────────────────────────────

    def _check_health_status(self, req: RcaRequest) -> list[RcaFinding]:
        health = req.health
        findings = []

        if health.status in (HEALTH_DEGRADED_STATUS, HEALTH_OFFLINE_STATUS):
            findings.append(RcaFinding(
                bottleneck_type=BottleneckType.DOWNSTREAM_DEPENDENCY,
                confidence=0.8 if health.status == HEALTH_OFFLINE_STATUS else 0.65,
                reasoning=(
                    f"Service health status is {health.status} "
                    f"(latency: {health.latency_ms:.0f}ms, "
                    f"response code: {health.response_code})."
                ),
                evidence=[
                    EvidenceItem(
                        metric="health.status",
                        value=health.status,
                        status="critical" if health.status == HEALTH_OFFLINE_STATUS else "warning",
                        description=f"Health check returned {health.status}",
                    ),
                    EvidenceItem(
                        metric="health.latency_ms",
                        value=round(health.latency_ms, 1),
                        unit="ms",
                        status="normal" if health.latency_ms < 500 else "warning",
                        description=f"Health check latency: {health.latency_ms:.0f}ms",
                    ),
                ],
                recommendations=[
                    Recommendation(
                        title="Investigate health check failures",
                        description=f"Health endpoint returned {health.status}.",
                        priority="HIGH",
                        category="INFRASTRUCTURE",
                        confidence=0.75,
                    ),
                ],
            ))

        return findings

    # ── Historical Trends ─────────────────────────────────────────

    def _check_historical_trends(self, req: RcaRequest) -> list[RcaFinding]:
        if not req.historical_metrics:
            return []

        findings = []
        cpu_values = [m.value for m in req.historical_metrics if m.metric_name == "system.cpu.usage"]
        heap_values = [m.value for m in req.historical_metrics if m.metric_name == "jvm.heap.usage.percent"]
        latency_values = [m.value for m in req.historical_metrics if m.metric_name == "http.request.duration.avg"]

        if len(cpu_values) >= 3:
            trend = self._compute_trend(cpu_values)
            if trend > 0.1:
                findings.append(RcaFinding(
                    bottleneck_type=BottleneckType.CPU_SATURATION,
                    confidence=min(0.7, 0.3 + trend),
                    reasoning=(
                        f"CPU usage has been trending upward over the last "
                        f"{req.hours_back} hours (trend: +{trend:.2f}). "
                        f"Current: {cpu_values[-1]:.1%}, earlier: {cpu_values[0]:.1%}."
                    ),
                    evidence=[
                        EvidenceItem(
                            metric="cpu_trend",
                            value=round(trend, 4),
                            unit="slope",
                            status="warning",
                            description=f"CPU trending up: {cpu_values[0]:.1%} → {cpu_values[-1]:.1%}",
                        ),
                    ],
                    recommendations=[
                        Recommendation(
                            title="Investigate growing CPU demand",
                            description="CPU is steadily increasing. Identify the source.",
                            priority="MEDIUM",
                            category="CODE",
                            confidence=0.6,
                        ),
                    ],
                ))

        if len(heap_values) >= 3:
            trend = self._compute_trend(heap_values)
            if trend > 0.15:
                findings.append(RcaFinding(
                    bottleneck_type=BottleneckType.MEMORY_PRESSURE,
                    confidence=min(0.7, 0.3 + trend),
                    reasoning=(
                        f"Heap usage has been trending upward over the last "
                        f"{req.hours_back} hours (trend: +{trend:.2f}%/sample). "
                        f"Possible memory leak."
                    ),
                    evidence=[
                        EvidenceItem(
                            metric="heap_trend",
                            value=round(trend, 4),
                            unit="slope",
                            status="warning",
                            description=f"Heap trending up: {heap_values[0]:.1f}% → {heap_values[-1]:.1f}%",
                        ),
                    ],
                    recommendations=[
                        Recommendation(
                            title="Investigate potential memory leak",
                            description="Heap usage is steadily growing. Profile with MAT.",
                            priority="HIGH",
                            category="CODE",
                            confidence=0.65,
                        ),
                    ],
                ))

        return findings

    # ── No bottleneck ─────────────────────────────────────────────

    def _no_bottleneck_finding(self, req: RcaRequest) -> RcaFinding:
        return RcaFinding(
            bottleneck_type=BottleneckType.NO_BOTTLENECK_DETECTED,
            confidence=0.7,
            reasoning=(
                f"All metrics for {req.service_name} are within normal ranges. "
                f"CPU at {req.cpu.system_cpu_usage:.1%}, heap at "
                f"{req.memory.heap_usage_percent:.1f}%, no connection pool "
                f"saturation detected."
            ),
            evidence=[
                EvidenceItem(
                    metric="overall_health",
                    value="healthy",
                    status="normal",
                    description="All checked metrics are within normal thresholds",
                ),
            ],
            recommendations=[
                Recommendation(
                    title="Continue monitoring",
                    description="No immediate action required. Continue periodic monitoring.",
                    priority="LOW",
                    category="CONFIGURATION",
                    confidence=0.6,
                ),
            ],
        )

    # ── Helpers ────────────────────────────────────────────────────

    def _classify_ratio(self, value: float, warning: float, critical: float) -> str:
        if value >= critical:
            return "critical"
        if value >= warning:
            return "warning"
        return "normal"

    def _classify_value(self, value: float, warning: float, critical: float) -> str:
        if value >= critical:
            return "critical"
        if value >= warning:
            return "warning"
        return "normal"

    def _format_bytes(self, bytes_val: float) -> str:
        if bytes_val >= 1_073_741_824:
            return f"{bytes_val / 1_073_741_824:.1f} GB"
        if bytes_val >= 1_048_576:
            return f"{bytes_val / 1_048_576:.1f} MB"
        if bytes_val >= 1024:
            return f"{bytes_val / 1024:.1f} KB"
        return f"{bytes_val:.0f} B"

    def _compute_trend(self, values: list[float]) -> float:
        if len(values) < 2:
            return 0.0
        n = len(values)
        x_mean = (n - 1) / 2
        y_mean = sum(values) / n
        numerator = sum((i - x_mean) * (v - y_mean) for i, v in enumerate(values))
        denominator = sum((i - x_mean) ** 2 for i in range(n))
        if denominator == 0:
            return 0.0
        return numerator / denominator

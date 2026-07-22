"""
RCA (Root Cause Analysis) request and response models.

Design principle: the RCA engine NEVER hallucinates. Every finding,
evidence item, and recommendation is derived exclusively from collected
metrics. The LLM is only used to polish reasoning text — never to
determine the root cause itself.
"""

from enum import Enum

from pydantic import BaseModel, Field


# ── Input models ────────────────────────────────────────────────


class CpuMetrics(BaseModel):
    system_cpu_usage: float = 0.0
    process_cpu_usage: float = 0.0


class MemoryMetrics(BaseModel):
    heap_used_bytes: float = 0.0
    heap_max_bytes: float = 0.0
    heap_committed_bytes: float = 0.0
    heap_usage_percent: float = 0.0


class LatencyMetrics(BaseModel):
    avg_duration_seconds: float = 0.0
    max_duration_seconds: float = 0.0
    total_requests: int = 0


class GcMetrics(BaseModel):
    gc_pause_sum_seconds: float = 0.0
    gc_pause_count: int = 0
    gc_live_data_bytes: float = 0.0
    gc_max_data_bytes: float = 0.0


class ThreadMetrics(BaseModel):
    tomcat_threads_busy: int = 0
    tomcat_threads_current: int = 0


class HttpErrorMetrics(BaseModel):
    total_requests: int = 0
    error_requests: int = 0
    error_rate: float = 0.0


class ConnectionPoolMetrics(BaseModel):
    hikari_active: int = 0
    hikari_idle: int = 0
    hikari_pending: int = 0
    hikari_max_pool_size: int = 0


class HealthStatus(BaseModel):
    status: str = "UNKNOWN"
    latency_ms: float = 0.0
    response_code: int = 0


class HistoricalMetricPoint(BaseModel):
    timestamp: str
    metric_name: str
    value: float


class RcaRequest(BaseModel):
    service_id: str
    service_name: str

    cpu: CpuMetrics = Field(default_factory=CpuMetrics)
    memory: MemoryMetrics = Field(default_factory=MemoryMetrics)
    latency: LatencyMetrics = Field(default_factory=LatencyMetrics)
    gc: GcMetrics = Field(default_factory=GcMetrics)
    threads: ThreadMetrics = Field(default_factory=ThreadMetrics)
    http_errors: HttpErrorMetrics = Field(default_factory=HttpErrorMetrics)
    connection_pool: ConnectionPoolMetrics = Field(default_factory=ConnectionPoolMetrics)
    health: HealthStatus = Field(default_factory=HealthStatus)

    historical_metrics: list[HistoricalMetricPoint] = Field(default_factory=list)
    hours_back: int = 24


# ── Output models ───────────────────────────────────────────────


class BottleneckType(str, Enum):
    CPU_SATURATION = "CPU_SATURATION"
    MEMORY_PRESSURE = "MEMORY_PRESSURE"
    CONNECTION_POOL_EXHAUSTION = "CONNECTION_POOL_EXHAUSTION"
    THREAD_EXHAUSTION = "THREAD_EXHAUSTION"
    GC_PRESSURE = "GC_PRESSURE"
    NETWORK_LATENCY = "NETWORK_LATENCY"
    DISK_IO = "DISK_IO"
    DOWNSTREAM_DEPENDENCY = "DOWNSTREAM_DEPENDENCY"
    CONFIGURATION_ISSUE = "CONFIGURATION_ISSUE"
    NO_BOTTLENECK_DETECTED = "NO_BOTTLENECK_DETECTED"
    UNKNOWN = "UNKNOWN"


class EvidenceItem(BaseModel):
    metric: str
    value: float | str
    unit: str = ""
    status: str  # "normal", "warning", "critical"
    description: str


class Recommendation(BaseModel):
    title: str
    description: str
    priority: str  # "HIGH", "MEDIUM", "LOW"
    category: str  # "SCALING", "CONFIGURATION", "CODE", "INFRASTRUCTURE"
    confidence: float = Field(ge=0.0, le=1.0)


class RcaFinding(BaseModel):
    bottleneck_type: BottleneckType
    confidence: float = Field(ge=0.0, le=1.0)
    reasoning: str
    evidence: list[EvidenceItem] = Field(default_factory=list)
    recommendations: list[Recommendation] = Field(default_factory=list)


class RcaResponse(BaseModel):
    service_id: str
    service_name: str
    findings: list[RcaFinding] = Field(default_factory=list)
    primary_finding: RcaFinding | None = None
    summary: str = ""
    model_version: str = "1.0.0"
    analyzed_at: str = ""
    metrics_snapshot: dict = Field(default_factory=dict)

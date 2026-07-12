from pydantic import BaseModel, Field


class MetricsPayload(BaseModel):
    avg_latency_ms: float
    p50_latency_ms: float
    p95_latency_ms: float
    p99_latency_ms: float
    max_latency_ms: float
    std_dev_latency_ms: float
    tail_latency_ratio: float

    avg_throughput_rps: float
    peak_throughput_rps: float
    throughput_variability: float

    avg_error_rate: float
    max_error_rate: float
    total_requests: int
    total_failed_requests: int

    health_score: float
    health_status: str
    worst_severity: str


class LogsPayload(BaseModel):
    total_count: int
    error_count: int
    warn_count: int
    has_errors: bool
    threshold_breached: bool
    count_by_level: dict[str, int] = Field(default_factory=dict)
    count_by_category: dict[str, int] = Field(default_factory=dict)


class SimConfigPayload(BaseModel):
    duration_seconds: int
    concurrent_users: int
    error_rate_threshold: float
    scenario_name: str


class AnalysisRequest(BaseModel):
    simulation_id: int
    service_name: str
    metrics: MetricsPayload
    logs: LogsPayload
    simulation_config: SimConfigPayload
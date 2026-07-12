from enum import Enum

from pydantic import BaseModel, Field


class AnomalyType(str, Enum):
    NONE = "NONE"
    LATENCY_SPIKE = "LATENCY_SPIKE"
    ERROR_BURST = "ERROR_BURST"
    THROUGHPUT_DROP = "THROUGHPUT_DROP"
    SATURATION = "SATURATION"
    MEMORY_PRESSURE = "MEMORY_PRESSURE"
    CASCADING_FAILURE = "CASCADING_FAILURE"
    UNKNOWN = "UNKNOWN"


class PredictedTrend(str, Enum):
    IMPROVING = "IMPROVING"
    STABLE = "STABLE"
    DEGRADING = "DEGRADING"


class RecommendationCategory(str, Enum):
    LATENCY = "LATENCY"
    ERROR_RATE = "ERROR_RATE"
    THROUGHPUT = "THROUGHPUT"
    SCALING = "SCALING"
    MEMORY = "MEMORY"
    CONFIGURATION = "CONFIGURATION"
    ARCHITECTURE = "ARCHITECTURE"


class RecommendationPriority(str, Enum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class Recommendation(BaseModel):
    category: RecommendationCategory
    priority: RecommendationPriority
    title: str
    description: str
    action: str
    confidence_score: float = Field(ge=0.0, le=1.0)
    estimated_impact: float = Field(ge=0.0, le=1.0)


class AnalysisResponse(BaseModel):
    simulation_id: int
    summary: str
    anomaly_type: AnomalyType
    anomaly_score: float = Field(ge=0.0, le=1.0)
    predicted_trend: PredictedTrend
    predicted_p95_ms: float
    model_version: str

    detected_patterns: list[str] = Field(default_factory=list)
    feature_importance: dict[str, float] = Field(default_factory=dict)
    recommendations: list[Recommendation] = Field(default_factory=list)

    degraded: bool = False
    degraded_reason: str | None = None


class HealthResponse(BaseModel):
    status: str
    model_version: str
    llm_provider: str
    llm_reachable: bool
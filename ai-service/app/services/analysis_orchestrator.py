import time

from app.core.config import get_settings
from app.core.logging import get_logger
from app.models.request import AnalysisRequest
from app.models.response import AnalysisResponse, AnomalyType
from app.services.anomaly_detector import AnomalyDetector
from app.services.forecaster import LatencyForecaster
from app.services.llm_client import LLMClient
from app.services.recommendation_generator import RecommendationGenerator

logger = get_logger(__name__)


class AnalysisOrchestrator:
    """
    The single entry point the /api/analyze route calls. Runs every
    sub-service in sequence and assembles the final AnalysisResponse
    that Spring Boot's FastAPIAdapter deserialises.
    """

    def __init__(self):
        settings = get_settings()
        self._anomaly_detector = AnomalyDetector(
            contamination=settings.isolation_forest_contamination
        )
        self._forecaster = LatencyForecaster()
        self._llm_client = LLMClient()
        self._rec_generator = RecommendationGenerator()
        self._model_version = settings.model_version

    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        start = time.monotonic()
        logger.info(
            "analysis_started",
            simulation_id=request.simulation_id,
            service_name=request.service_name,
        )

        # ── Step 1: Anomaly detection ──────────────────────────
        anomaly_type, anomaly_score, patterns = self._anomaly_detector.detect(request)

        # ── Step 2: Forecasting ────────────────────────────────
        trend, predicted_p95 = self._forecaster.forecast(request)

        # ── Step 3: Feature importance (for explainability) ───
        feature_importance = self._compute_feature_importance(request, anomaly_type)

        # ── Step 4: Recommendations ────────────────────────────
        recommendations = self._rec_generator.generate(
            request, anomaly_type, anomaly_score, feature_importance
        )

        # ── Step 5: LLM summary (runs last — uses all prior outputs) ──
        summary = await self._llm_client.generate_summary(
            request, anomaly_type, anomaly_score, trend
        )

        elapsed_ms = (time.monotonic() - start) * 1000
        logger.info(
            "analysis_completed",
            simulation_id=request.simulation_id,
            anomaly_type=anomaly_type.value,
            elapsed_ms=round(elapsed_ms, 1),
        )

        return AnalysisResponse(
            simulation_id=request.simulation_id,
            summary=summary,
            anomaly_type=anomaly_type,
            anomaly_score=anomaly_score,
            predicted_trend=trend,
            predicted_p95_ms=predicted_p95,
            model_version=self._model_version,
            detected_patterns=patterns,
            feature_importance=feature_importance,
            recommendations=recommendations,
            degraded=False,
        )

    # ── Feature importance ─────────────────────────────────────

    def _compute_feature_importance(
        self, request: AnalysisRequest, anomaly_type: AnomalyType
    ) -> dict[str, float]:
        """
        A lightweight, interpretable importance score per feature —
        normalised contribution of each metric to the anomaly classification.
        Not derived from the IsolationForest internals directly (which don't
        expose per-feature attribution cleanly); instead computed from how
        far each feature deviates from its expected healthy range, which is
        more directly interpretable for the recommendation engine on the
        Java side to consume.
        """
        m = request.metrics
        cfg = request.simulation_config

        raw = {
            "p95_latency_ms": max(0.0, (m.p95_latency_ms - 200) / 1000),
            "tail_latency_ratio": max(0.0, (m.tail_latency_ratio - 2.0) / 5),
            "avg_error_rate": max(0.0, (m.avg_error_rate - cfg.error_rate_threshold) * 10),
            "throughput_variability": max(0.0, m.throughput_variability),
            "std_dev_latency_ms": max(0.0, (m.std_dev_latency_ms - 50) / 200),
        }

        total = sum(raw.values())
        if total == 0:
            return {k: 0.0 for k in raw}

        return {k: round(v / total, 4) for k, v in raw.items()}
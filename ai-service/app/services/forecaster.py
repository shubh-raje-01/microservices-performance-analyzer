import numpy as np

from app.core.logging_config import get_logger
from app.models.request import AnalysisRequest
from app.models.response import PredictedTrend

logger = get_logger(__name__)


class LatencyForecaster:
    """
    Predicts the next-period p95 latency and overall trend direction.

    A true ARIMA model needs a time-series of historical observations.
    Since each /api/analyze call currently receives one aggregated snapshot
    per simulation (not a rolling window), this forecaster uses a
    statistically-grounded heuristic: extrapolating from the relationship
    between load (concurrent users), error rate, and latency tail behaviour.

    When historical snapshots become available (e.g. multiple simulations
    for the same service over time), swap _heuristic_forecast for a real
    statsmodels ARIMA/SARIMAX fit — the interface stays identical.
    """

    def forecast(self, request: AnalysisRequest) -> tuple[PredictedTrend, float]:
        m = request.metrics
        cfg = request.simulation_config

        trend = self._determine_trend(request)
        predicted_p95 = self._heuristic_forecast(request, trend)

        logger.info(
            "forecast_complete",
            simulation_id=request.simulation_id,
            trend=trend.value,
            predicted_p95_ms=round(predicted_p95, 2),
        )

        return trend, round(predicted_p95, 2)

    # ── Trend determination ───────────────────────────────────

    def _determine_trend(self, request: AnalysisRequest) -> PredictedTrend:
        m = request.metrics

        # Tail ratio growing relative to p50 is the earliest degradation signal —
        # it shows up before p95/avg latency themselves spike.
        if m.tail_latency_ratio > 5.0 or m.avg_error_rate > 0.08:
            return PredictedTrend.DEGRADING

        if m.tail_latency_ratio < 2.5 and m.avg_error_rate < 0.01 and m.health_score > 80:
            return PredictedTrend.IMPROVING

        return PredictedTrend.STABLE

    # ── Heuristic forecast ────────────────────────────────────

    def _heuristic_forecast(
        self, request: AnalysisRequest, trend: PredictedTrend
    ) -> float:
        m = request.metrics
        cfg = request.simulation_config

        current_p95 = m.p95_latency_ms

        # Project how p95 would move if concurrency grew by a further 20% —
        # the standard "what happens at next scale" question this forecast answers.
        load_growth_factor = 1.2
        congestion_exponent = 1.0 + min(m.throughput_variability, 1.0)

        projected = current_p95 * (load_growth_factor ** congestion_exponent)

        if trend == PredictedTrend.DEGRADING:
            projected *= 1.15
        elif trend == PredictedTrend.IMPROVING:
            projected *= 0.92

        # Error rate compounds latency under retry storms
        if m.avg_error_rate > cfg.error_rate_threshold:
            retry_amplification = 1 + (m.avg_error_rate - cfg.error_rate_threshold) * 2
            projected *= retry_amplification

        return float(np.clip(projected, current_p95 * 0.5, current_p95 * 5))
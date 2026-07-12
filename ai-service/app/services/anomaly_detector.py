import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

from app.core.logging import get_logger
from app.models.request import AnalysisRequest
from app.models.response import AnomalyType

logger = get_logger(__name__)


class AnomalyDetector:
    """
    Detects anomalies using a hybrid approach:
      1. Rule-based thresholds (fast, deterministic, always available)
      2. IsolationForest scoring against a synthetic baseline distribution
         representing "normal" service behaviour, giving a continuous
         anomaly score even when no single threshold is breached outright.
    """

    FEATURE_NAMES = [
        "p95_latency_ms",
        "tail_latency_ratio",
        "avg_error_rate",
        "throughput_variability",
        "std_dev_latency_ms",
    ]

    def __init__(self, contamination: float = 0.1):
        self._contamination = contamination

    def detect(self, request: AnalysisRequest) -> tuple[AnomalyType, float, list[str]]:
        features = self._extract_features(request)
        baseline = self._build_baseline(request)

        scaler = StandardScaler()
        baseline_scaled = scaler.fit_transform(baseline)
        sample_scaled = scaler.transform([features])

        model = IsolationForest(
            contamination=self._contamination,
            random_state=42,
            n_estimators=100,
        )
        model.fit(baseline_scaled)

        # decision_function: higher = more normal, lower = more anomalous.
        # Normalise to [0, 1] where 1.0 = highly anomalous.
        raw_score = model.decision_function(sample_scaled)[0]
        anomaly_score = float(np.clip(0.5 - raw_score, 0.0, 1.0))

        anomaly_type, patterns = self._classify(request, anomaly_score)

        logger.info(
            "anomaly_detection_complete",
            simulation_id=request.simulation_id,
            anomaly_type=anomaly_type.value,
            anomaly_score=round(anomaly_score, 4),
            patterns=patterns,
        )

        return anomaly_type, round(anomaly_score, 4), patterns

    # ── Feature extraction ─────────────────────────────────────

    def _extract_features(self, request: AnalysisRequest) -> list[float]:
        m = request.metrics
        return [
            m.p95_latency_ms,
            m.tail_latency_ratio,
            m.avg_error_rate,
            m.throughput_variability,
            m.std_dev_latency_ms,
        ]

    def _build_baseline(self, request: AnalysisRequest) -> np.ndarray:
        """
        Synthesises a baseline distribution of "healthy" feature vectors
        scaled to the simulation's concurrency level, so the IsolationForest
        has something meaningful to compare against. In production this
        would be replaced with a persisted distribution learned from
        historical COMPLETED simulations for the same service.
        """
        rng = np.random.default_rng(seed=request.simulation_id)
        n_samples = 200

        users = request.simulation_config.concurrent_users
        baseline_p95 = 50 + (users * 0.3)  # healthy p95 scales gently with load

        samples = np.column_stack([
            rng.normal(baseline_p95, baseline_p95 * 0.15, n_samples),       # p95
            rng.normal(2.5, 0.5, n_samples).clip(min=1.0),                  # tail ratio
            rng.normal(0.01, 0.005, n_samples).clip(min=0.0),               # error rate
            rng.normal(0.15, 0.08, n_samples).clip(min=0.0),                # throughput var
            rng.normal(baseline_p95 * 0.2, baseline_p95 * 0.05, n_samples), # std dev
        ])
        return samples

    # ── Classification ──────────────────────────────────────────

    def _classify(
        self, request: AnalysisRequest, anomaly_score: float
    ) -> tuple[AnomalyType, list[str]]:
        m = request.metrics
        patterns: list[str] = []

        if anomaly_score < 0.4:
            return AnomalyType.NONE, patterns

        signals: dict[AnomalyType, float] = {}

        # Latency spike signal
        if m.p95_latency_ms > 500 or m.tail_latency_ratio > 4.0:
            signals[AnomalyType.LATENCY_SPIKE] = max(
                m.p95_latency_ms / 1000, m.tail_latency_ratio / 6
            )
            patterns.append(
                f"p95 latency {m.p95_latency_ms:.0f}ms with tail ratio "
                f"{m.tail_latency_ratio:.2f}x"
            )

        # Error burst signal
        if m.avg_error_rate > 0.05 or m.max_error_rate > 0.15:
            signals[AnomalyType.ERROR_BURST] = max(
                m.avg_error_rate * 10, m.max_error_rate * 5
            )
            patterns.append(
                f"error rate {m.avg_error_rate * 100:.1f}% "
                f"(peak {m.max_error_rate * 100:.1f}%)"
            )

        # Throughput drop signal
        if m.avg_throughput_rps < 10 or m.throughput_variability > 0.6:
            signals[AnomalyType.THROUGHPUT_DROP] = max(
                1 - (m.avg_throughput_rps / 50), m.throughput_variability
            )
            patterns.append(
                f"throughput {m.avg_throughput_rps:.1f} rps, "
                f"variability {m.throughput_variability:.2f}"
            )

        # Saturation signal — concurrency high relative to throughput delivered
        users = request.simulation_config.concurrent_users
        if users > 0:
            per_user_rps = m.avg_throughput_rps / users
            if per_user_rps < 0.3 and users > 200:
                signals[AnomalyType.SATURATION] = 1 - per_user_rps
                patterns.append(
                    f"{per_user_rps:.3f} rps/user across {users} concurrent users"
                )

        # Memory pressure proxy — very high latency std-dev with low throughput
        # often correlates with GC pauses under load
        if m.std_dev_latency_ms > m.avg_latency_ms * 0.8 and m.avg_throughput_rps < 30:
            signals[AnomalyType.MEMORY_PRESSURE] = (
                m.std_dev_latency_ms / max(m.avg_latency_ms, 1)
            )
            patterns.append(
                f"high latency variance (stddev={m.std_dev_latency_ms:.0f}ms) "
                f"under low throughput"
            )

        if not signals:
            return AnomalyType.UNKNOWN, patterns

        # Cascading failure — three or more independent signals fired together
        if len(signals) >= 3:
            return AnomalyType.CASCADING_FAILURE, patterns

        dominant = max(signals, key=signals.get)
        return dominant, patterns
import json

import httpx
from anthropic import AsyncAnthropic, APIError as AnthropicAPIError
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.core.config import get_settings
from app.core.logging import get_logger
from app.models.request import AnalysisRequest
from app.models.response import AnomalyType, PredictedTrend

logger = get_logger(__name__)

_SUMMARY_PROMPT_TEMPLATE = """You are analysing performance test results for a microservice named "{service_name}".

Simulation: {scenario_name}
Duration: {duration_seconds}s with {concurrent_users} concurrent users

Metrics:
- p95 latency: {p95_latency_ms:.0f}ms (p99: {p99_latency_ms:.0f}ms)
- Error rate: {error_rate:.2%}
- Throughput: {throughput_rps:.1f} req/s
- Health score: {health_score:.1f}/100 ({health_status})

Detected anomaly: {anomaly_type} (confidence: {anomaly_score:.0%})
Predicted trend: {predicted_trend}

Write a single, dense paragraph (3-4 sentences, no bullet points, no headers) summarising \
the service's current performance state for an engineer who has not seen the raw data. \
Be specific with numbers. End with the single most important takeaway."""


class LLMClient:
    """
    Generates the human-readable analysis summary. Provider is selected via
    LLM_PROVIDER env var — "ollama" for local development (no API key, no
    cost), "claude" for production quality. Both paths return identical
    plain-text output so AnalysisOrchestrator does not need to know which
    backend produced it.
    """

    def __init__(self):
        self._settings = get_settings()

    async def generate_summary(self, request: AnalysisRequest, anomaly_type: AnomalyType,
                               anomaly_score: float, trend: PredictedTrend) -> str:
        prompt = self._build_prompt(request, anomaly_type, anomaly_score, trend)

        try:
            if self._settings.llm_provider == "claude":
                return await self._call_claude(prompt)
            return await self._call_ollama(prompt)
        except Exception as exc:
            logger.warning(
                "llm_generation_failed",
                provider=self._settings.llm_provider,
                error=str(exc),
            )
            return self._fallback_summary(request, anomaly_type, trend)

    # ── Prompt construction ───────────────────────────────────

    def _build_prompt(self, request: AnalysisRequest, anomaly_type: AnomalyType,
                      anomaly_score: float, trend: PredictedTrend) -> str:
        m = request.metrics
        return _SUMMARY_PROMPT_TEMPLATE.format(
            service_name=request.service_name,
            scenario_name=request.simulation_config.scenario_name,
            duration_seconds=request.simulation_config.duration_seconds,
            concurrent_users=request.simulation_config.concurrent_users,
            p95_latency_ms=m.p95_latency_ms,
            p99_latency_ms=m.p99_latency_ms,
            error_rate=m.avg_error_rate,
            throughput_rps=m.avg_throughput_rps,
            health_score=m.health_score,
            health_status=m.health_status,
            anomaly_type=anomaly_type.value,
            anomaly_score=anomaly_score,
            predicted_trend=trend.value,
        )

    # ── Ollama backend ─────────────────────────────────────────

    @retry(
        stop=stop_after_attempt(2),
        wait=wait_exponential(multiplier=1, min=1, max=4),
        retry=retry_if_exception_type(httpx.TransportError),
        reraise=True,
    )
    async def _call_ollama(self, prompt: str) -> str:
        async with httpx.AsyncClient(timeout=self._settings.llm_timeout_seconds) as client:
            response = await client.post(
                f"{self._settings.ollama_base_url}/api/generate",
                json={
                    "model": self._settings.ollama_model,
                    "prompt": prompt,
                    "stream": False,
                    "options": {"temperature": 0.3, "num_predict": 200},
                },
            )
            response.raise_for_status()
            data = response.json()
            return data.get("response", "").strip()

    # ── Claude backend ──────────────────────────────────────────

    @retry(
        stop=stop_after_attempt(2),
        wait=wait_exponential(multiplier=1, min=1, max=4),
        retry=retry_if_exception_type(AnthropicAPIError),
        reraise=True,
    )
    async def _call_claude(self, prompt: str) -> str:
        if not self._settings.anthropic_api_key:
            raise ValueError("ANTHROPIC_API_KEY is not configured")

        client = AsyncAnthropic(api_key=self._settings.anthropic_api_key)
        response = await client.messages.create(
            model=self._settings.anthropic_model,
            max_tokens=300,
            temperature=0.3,
            messages=[{"role": "user", "content": prompt}],
        )
        return "".join(
            block.text for block in response.content if block.type == "text"
        ).strip()

    # ── Fallback when both LLM paths fail ────────────────────────

    def _fallback_summary(self, request: AnalysisRequest, anomaly_type: AnomalyType,
                          trend: PredictedTrend) -> str:
        m = request.metrics
        if anomaly_type == AnomalyType.NONE:
            return (
                f"{request.service_name} performed within healthy bounds: "
                f"p95 latency {m.p95_latency_ms:.0f}ms, error rate "
                f"{m.avg_error_rate:.2%}, throughput {m.avg_throughput_rps:.1f} req/s. "
                f"No anomalies detected and the trend is {trend.value.lower()}."
            )
        return (
            f"{request.service_name} shows signs of {anomaly_type.value.lower().replace('_', ' ')} "
            f"with p95 latency at {m.p95_latency_ms:.0f}ms and error rate at "
            f"{m.avg_error_rate:.2%}. The trend is {trend.value.lower()} — "
            f"review the detected patterns and recommendations for remediation steps."
        )
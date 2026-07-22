"""
RCA Orchestrator — coordinates the full root cause analysis pipeline.

Pipeline:
  1. Rule engine evaluates metrics deterministically → list[RcaFinding]
  2. LLM optionally refines reasoning text (does NOT determine root cause)
  3. Summary is generated from all findings
  4. Response is assembled with all findings ranked by confidence

The rule engine output is the source of truth. The LLM only polishes text.
"""

import time
from datetime import datetime, timezone

from app.core.config import get_settings
from app.core.logging_config import get_logger
from app.models.rca import RcaRequest, RcaResponse, RcaFinding
from app.services.root_cause_analyzer import RootCauseAnalyzer
from app.services.rca_llm_client import RcaLlmClient

logger = get_logger(__name__)


class RcaOrchestrator:
    """
    Single entry point for the /api/rca endpoint. Coordinates the
    deterministic rule engine and optional LLM refinement.
    """

    def __init__(self):
        settings = get_settings()
        self._analyzer = RootCauseAnalyzer()
        self._llm_client = RcaLlmClient()
        self._model_version = settings.model_version

    async def analyze(self, request: RcaRequest) -> RcaResponse:
        start = time.monotonic()
        logger.info(
            "rca_started",
            service_id=request.service_id,
            service_name=request.service_name,
        )

        # Step 1: Deterministic rule-based analysis
        findings = self._analyzer.analyze(request)

        # Step 2: Optionally refine reasoning via LLM (best-effort)
        if findings and findings[0].bottleneck_type.value != "NO_BOTTLENECK_DETECTED":
            findings = await self._refine_findings(request, findings)

        # Step 3: Generate summary
        summary = await self._llm_client.generate_summary(request, findings)

        # Step 4: Assemble response
        primary = findings[0] if findings else None
        metrics_snapshot = self._build_metrics_snapshot(request)

        elapsed_ms = (time.monotonic() - start) * 1000
        logger.info(
            "rca_completed",
            service_id=request.service_id,
            finding_count=len(findings),
            primary=primary.bottleneck_type.value if primary else "NONE",
            elapsed_ms=round(elapsed_ms, 1),
        )

        return RcaResponse(
            service_id=request.service_id,
            service_name=request.service_name,
            findings=findings,
            primary_finding=primary,
            summary=summary,
            model_version=self._model_version,
            analyzed_at=datetime.now(timezone.utc).isoformat(),
            metrics_snapshot=metrics_snapshot,
        )

    async def _refine_findings(
        self, request: RcaRequest, findings: list[RcaFinding]
    ) -> list[RcaFinding]:
        """Refine reasoning text for top findings via LLM (best-effort)."""
        refined = []
        for i, finding in enumerate(findings):
            if i < 3:
                try:
                    refined_text = await self._llm_client.refine_reasoning(
                        request, finding
                    )
                    refined.append(
                        finding.model_copy(update={"reasoning": refined_text})
                    )
                except Exception:
                    refined.append(finding)
            else:
                refined.append(finding)
        return refined

    def _build_metrics_snapshot(self, request: RcaRequest) -> dict:
        """Build a snapshot of key metrics for the response."""
        return {
            "cpu": {
                "system": request.cpu.system_cpu_usage,
                "process": request.cpu.process_cpu_usage,
            },
            "memory": {
                "heap_used_bytes": request.memory.heap_used_bytes,
                "heap_max_bytes": request.memory.heap_max_bytes,
                "heap_usage_percent": request.memory.heap_usage_percent,
            },
            "latency": {
                "avg_seconds": request.latency.avg_duration_seconds,
                "max_seconds": request.latency.max_duration_seconds,
            },
            "gc": {
                "pause_sum_seconds": request.gc.gc_pause_sum_seconds,
                "pause_count": request.gc.gc_pause_count,
            },
            "threads": {
                "busy": request.threads.tomcat_threads_busy,
                "current": request.threads.tomcat_threads_current,
            },
            "connection_pool": {
                "active": request.connection_pool.hikari_active,
                "idle": request.connection_pool.hikari_idle,
                "pending": request.connection_pool.hikari_pending,
                "max": request.connection_pool.hikari_max_pool_size,
            },
            "health": {
                "status": request.health.status,
            },
        }

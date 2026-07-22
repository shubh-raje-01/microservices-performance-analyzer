from fastapi import APIRouter, HTTPException, status

from app.core.logging_config import get_logger
from app.models.rca import RcaRequest, RcaResponse
from app.services.rca_orchestrator import RcaOrchestrator

router = APIRouter(prefix="/api", tags=["rca"])
logger = get_logger(__name__)

_orchestrator = RcaOrchestrator()


@router.post("/rca", response_model=RcaResponse)
async def analyze_root_cause(request: RcaRequest) -> RcaResponse:
    """
    Perform root cause analysis on collected metrics.

    The analysis is deterministic and rule-based. The LLM is only used
    to optionally polish reasoning text — never to determine root causes.
    """
    try:
        return await _orchestrator.analyze(request)
    except Exception as exc:
        logger.error(
            "rca_failed",
            service_id=request.service_id,
            error=str(exc),
            exc_info=True,
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"RCA pipeline failed: {exc}",
        ) from exc

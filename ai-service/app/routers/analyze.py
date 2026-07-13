from fastapi import APIRouter, HTTPException, status

from app.core.logging_config import get_logger
from app.models.request import AnalysisRequest
from app.models.response import AnalysisResponse
from app.services.analysis_orchestrator import AnalysisOrchestrator

router = APIRouter(prefix="/api", tags=["analysis"])
logger = get_logger(__name__)

_orchestrator = AnalysisOrchestrator()


@router.post("/analyze", response_model=AnalysisResponse)
async def analyze(request: AnalysisRequest) -> AnalysisResponse:
    try:
        return await _orchestrator.analyze(request)
    except Exception as exc:
        logger.error(
            "analysis_failed",
            simulation_id=request.simulation_id,
            error=str(exc),
            exc_info=True,
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Analysis pipeline failed: {exc}",
        ) from exc
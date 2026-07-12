from fastapi import APIRouter

import httpx

from app.core.config import get_settings
from app.core.logging import get_logger
from app.models.response import HealthResponse

router = APIRouter(tags=["health"])
logger = get_logger(__name__)


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    settings = get_settings()
    llm_reachable = await _check_llm_reachable(settings)

    return HealthResponse(
        status="UP",
        model_version=settings.model_version,
        llm_provider=settings.llm_provider,
        llm_reachable=llm_reachable,
    )


async def _check_llm_reachable(settings) -> bool:
    try:
        if settings.llm_provider == "ollama":
            async with httpx.AsyncClient(timeout=3) as client:
                resp = await client.get(f"{settings.ollama_base_url}/api/tags")
                return resp.status_code == 200
        # For Claude, presence of an API key is the practical signal —
        # a live API call on every health check would be wasteful.
        return settings.anthropic_api_key is not None
    except Exception as exc:
        logger.warning("llm_health_check_failed", error=str(exc))
        return False
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # App 
    app_name: str = "Microservices Performance Analyzer — AI Service"
    environment: str = "development"
    log_level: str = "INFO"

    # Server
    host: str = "0.0.0.0"
    port: int = 8000

    # CORS 
    # Spring Boot calls this service server-to-server
    cors_origins: list[str] = ["http://localhost:5173", "http://localhost:8080"]

    # LLM backend 
    # "ollama" for local dev, "claude" for production
    llm_provider: str = "ollama"

    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.1"

    anthropic_api_key: str | None = None
    anthropic_model: str = "claude-sonnet-4-6"

    llm_timeout_seconds: int = 20
    llm_max_retries: int = 2

    # Anomaly detection 
    isolation_forest_contamination: float = 0.1
    anomaly_score_threshold: float = 0.6

    # Redis (optional caching)
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_enabled: bool = False

    # Model versioning
    model_version: str = "1.0.0"


@lru_cache
def get_settings() -> Settings:
    return Settings()
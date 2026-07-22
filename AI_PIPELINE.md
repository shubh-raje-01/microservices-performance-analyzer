# AI Pipeline Documentation

## Architecture Overview

The Microservice Performance Analyzer uses a **dual-service architecture** for AI-powered analysis:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend (Java)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  AIService    │  │  RcaService  │  │  FastAPIAdapter /    │  │
│  │  (simulate    │  │  (root cause │  │  RcaAdapter          │  │
│  │   analysis)   │  │   analysis)  │  │  (WebClient HTTP)    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
│         │                  │                      │              │
│         └──────────────────┼──────────────────────┘              │
│                            │                                     │
└────────────────────────────┼─────────────────────────────────────┘
                             │ HTTP POST
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Python FastAPI AI Service                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ /api/analyze      │  │ /api/rca         │  │ /health      │  │
│  │ (simulation)      │  │ (root cause)     │  │              │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────────┘  │
│           │                      │                               │
│  ┌────────▼─────────┐  ┌────────▼─────────┐                    │
│  │AnalysisOrchestrator│  │RcaOrchestrator   │                    │
│  │  1. AnomalyDetect │  │  1. RuleEngine   │                    │
│  │  2. Forecast      │  │  2. LLM Refine   │                    │
│  │  3. Features      │  │  3. Summary      │                    │
│  │  4. Recommendations│  └──────────────────┘                    │
│  │  5. LLM Summary   │                                          │
│  └──────────────────┘                                           │
└─────────────────────────────────────────────────────────────────┘
```

## Pipeline 1: Simulation Analysis (`/api/analyze`)

### Purpose
Analyzes completed performance simulation runs to detect anomalies, forecast trends, and generate recommendations.

### Flow
```
Simulation Completed → AIService.analyzeSimulation(id)
  → AIFeatureBuilder.build() → AIFeatureSet
  → FastAPIAdapter.analyze(request) → HTTP POST /api/analyze
    → AnalysisOrchestrator.analyze()
      1. AnomalyDetector.detect()     → (AnomalyType, score, patterns)
      2. LatencyForecaster.forecast() → (PredictedTrend, p95)
      3. Feature importance scoring    → dict[str, float]
      4. RecommendationGenerator      → list[Recommendation]
      5. LLMClient.generate_summary() → str
    ← AnalysisResponse
  → AIInsight entity persisted
  → AIAnalysisCompletedEvent published
```

### Anomaly Detection
- **Hybrid approach**: IsolationForest (ML) + rule-based classification
- **Features**: p95_latency_ms, tail_latency_ratio, avg_error_rate, throughput_variability, std_dev_latency_ms
- **Baseline**: Synthetic "healthy" distribution scaled by concurrency level
- **Classification rules**: Latency spike, error burst, throughput drop, saturation, memory pressure, cascading failure

### LLM Integration
- **Provider**: Ollama (local) or Claude (production)
- **Purpose**: Generate human-readable summary paragraph
- **Temperature**: 0.3 (deterministic)
- **Fallback**: Template-based summary when both LLM paths fail
- **Never determines root cause** — only summarizes existing findings

## Pipeline 2: Root Cause Analysis (`/api/rca`)

### Purpose
Analyzes collected Prometheus metrics to identify bottlenecks, provide evidence, and recommend fixes. **Never hallucinates** — all findings are derived exclusively from collected metrics.

### Flow
```
RCA Request → RcaService.analyze(serviceId, serviceName, hours)
  → Fetch latest metrics from ServiceMetricsRepository
  → Fetch historical metrics for trend analysis
  → Build RcaRequest with all metric categories
  → RcaAdapter.analyze(request) → HTTP POST /api/rca
    → RcaOrchestrator.analyze()
      1. RootCauseAnalyzer.analyze()    → list[RcaFinding] (deterministic)
      2. RcaLlmClient.refine_reasoning() → polished text (best-effort)
      3. RcaLlmClient.generate_summary() → overall summary
    ← RcaResponse
  → RcaFinding entities persisted
  ← Structured JSON response
```

### Root Cause Classification
The rule engine evaluates these bottleneck types:

| Bottleneck Type | Key Indicators | Confidence Source |
|----------------|----------------|-------------------|
| `CONNECTION_POOL_EXHAUSTION` | HikariCP utilization >80%, pending threads | Pool utilization ratio |
| `CPU_SATURATION` | System CPU >85%, process CPU >90% | CPU usage percentage |
| `MEMORY_PRESSURE` | Heap usage >75%, growing trend | Heap utilization ratio |
| `GC_PRESSURE` | GC pause >0.5s, high collection count | Pause time duration |
| `THREAD_EXHAUSTION` | Tomcat threads >80% busy | Busy/total thread ratio |
| `NETWORK_LATENCY` | Avg response >500ms, max >5s | Latency duration |
| `DOWNSTREAM_DEPENDENCY` | HTTP error rate >5%, health degraded | Error rate magnitude |
| `NO_BOTTLENECK_DETECTED` | All metrics within normal ranges | Inverse of all thresholds |

### Evidence-Based Analysis
Every finding contains:
- **Evidence**: List of metric snapshots with status (normal/warning/critical)
- **Reasoning**: Built from actual metric values, not speculated
- **Recommendations**: Derived from the identified bottleneck type
- **Confidence**: Computed from how far metrics deviate from healthy ranges

### Anti-Hallucination Design
1. **Rule engine is deterministic** — same inputs always produce same outputs
2. **LLM only refines text** — never determines root cause
3. **All evidence is from metrics** — no external knowledge injected
4. **Prompts contain only structured data** — no open-ended generation
5. **Fallback returns original reasoning** — LLM failure doesn't degrade analysis

## API Endpoints

### Simulation Analysis
```
POST /api/v1/analyze/{simulationId}
  → Triggers full AI analysis pipeline
  → Returns AIInsight with anomaly detection + recommendations

GET /api/v1/analyze/{simulationId}/insights
  → Returns cached analysis results

GET /api/v1/analyze/anomalies/recent?hoursBack=24
  → Returns recent anomaly detections across all services

GET /api/v1/analyze/health
  → Health check for the Python AI service
```

### Root Cause Analysis
```
POST /api/v1/rca/{serviceId}?serviceName=payment&hours=24
  → Triggers RCA on collected Prometheus metrics
  → Returns structured findings with evidence + recommendations

GET /api/v1/rca/{serviceId}/history?page=0&size=20
  → Returns paginated RCA history for a service

GET /api/v1/rca/{serviceId}/recent?hours=24
  → Returns recent RCA findings

GET /api/v1/rca/{serviceId}/stats?hours=24
  → Returns bottleneck type distribution statistics
```

## Data Models

### RCA Request (Java → Python)
```json
{
  "service_id": "uuid",
  "service_name": "payment-service",
  "cpu": { "system_cpu_usage": 0.45, "process_cpu_usage": 0.38 },
  "memory": { "heap_used_bytes": 536870912, "heap_max_bytes": 1073741824, "heap_usage_percent": 50.0 },
  "latency": { "avg_duration_seconds": 0.12, "max_duration_seconds": 2.5, "total_requests": 15000 },
  "gc": { "gc_pause_sum_seconds": 0.3, "gc_pause_count": 45 },
  "threads": { "tomcat_threads_busy": 18, "tomcat_threads_current": 25 },
  "connection_pool": { "hikari_active": 8, "hikari_idle": 2, "hikari_pending": 3, "hikari_max_pool_size": 10 },
  "health": { "status": "ONLINE", "latency_ms": 25.0, "response_code": 200 },
  "historical_metrics": [...],
  "hours_back": 24
}
```

### RCA Response (Python → Java)
```json
{
  "service_id": "uuid",
  "service_name": "payment-service",
  "findings": [
    {
      "bottleneck_type": "CONNECTION_POOL_EXHAUSTION",
      "confidence": 0.87,
      "reasoning": "Connection pool is near exhaustion: 8 of 10 connections in use (80% utilization). 3 threads are blocked waiting for a connection.",
      "evidence": [
        { "metric": "hikaricp.connections.active", "value": 8, "unit": "connections", "status": "warning", "description": "8 of 10 pool connections active" }
      ],
      "recommendations": [
        { "title": "Increase HikariCP maximum pool size", "description": "Current max pool size is 10. Increase to 20.", "priority": "HIGH", "category": "CONFIGURATION", "confidence": 0.8 }
      ]
    }
  ],
  "primary_finding": { ... },
  "summary": "Root cause analysis identified connection pool exhaustion as the primary bottleneck...",
  "model_version": "1.0.0",
  "analyzed_at": "2026-07-21T22:00:00Z",
  "metrics_snapshot": { ... }
}
```

## Configuration

### Python AI Service
```yaml
# ai-service/.env
LLM_PROVIDER=ollama          # "ollama" or "claude"
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.1
ANTHROPIC_API_KEY=            # required for claude provider
LLM_TIMEOUT_SECONDS=20
MODEL_VERSION=1.0.0
```

### Spring Boot Backend
```yaml
# application-local.yaml
ai-service:
  base-url: http://localhost:8000
  timeout-seconds: 30
  connect-timeout-ms: 5000

resilience4j:
  circuit-breaker:
    instances:
      fastapi:
        failure-rate-threshold: 50
        sliding-window-size: 5
        wait-duration-in-open-state: 30s
  retry:
    instances:
      fastapi:
        max-attempts: 3
        wait-duration: 1s
        enable-exponential-backoff: true
```

## Resilience

### Circuit Breaker
- Opens after 50% failure rate over 5 calls
- Also opens at 80% slow call rate (>10s)
- Stays open 30s, then HALF_OPEN with 2 test calls

### Retry
- 3 attempts with exponential backoff (1s, 2s, 4s)
- Only retries transient errors (IOException, TimeoutException)
- Ignores business exceptions

### Fallbacks
- **Java side**: 4 fallback methods return DEGRADED responses
- **Python side**: Template-based summary when LLM fails
- **RCA side**: Original deterministic reasoning returned unchanged

## Running the Services

```bash
# Start Python AI Service
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Start Spring Boot Backend
cd backend
mvn spring-boot:run

# Docker Compose (with infrastructure)
docker-compose up -d
```

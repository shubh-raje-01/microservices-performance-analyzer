package com.analyzer.modules.ai.adapter;

import com.analyzer.common.constants.ApiConstants;
import com.analyzer.common.exceptions.AIServiceException;
import com.analyzer.common.utils.JsonUtils;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastAPIAdapter {

    private final WebClient aiServiceWebClient;

    @Value("${ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    // ── Primary call

    /**
     * Posts the feature set to the Python FastAPI service and returns
     * the structured insight response.
     *
     * Decorated with:
     *  - @CircuitBreaker — opens after 50% failure rate over 5 calls
     *  - @Retry — up to 3 attempts with exponential back-off
     *
     * The fallback activates when the circuit is open or all retries are
     * exhausted, returning a DEGRADED response so the rest of the pipeline
     * can still complete.
     */
    @CircuitBreaker(name = "fastapi", fallbackMethod = "analyzeFallback")
    @Retry(name = "fastapi")
    public FastAPIResponse analyze(FastAPIRequest request) {
        log.info("FastAPIAdapter → POST {} simulationId={}",
                ApiConstants.AI_ANALYZE, request.getSimulationId());

        return aiServiceWebClient
                .post()
                .uri(ApiConstants.AI_ANALYZE)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AIServiceException(
                                        "FastAPI rejected the request", response.statusCode().value()))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AIServiceException(
                                        "FastAPI internal error — " + body,
                                        response.statusCode().value()))
                )
                .bodyToMono(FastAPIResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(r -> log.info(
                        "FastAPIAdapter ← response simulationId={} anomaly={} score={}",
                        r.getSimulationId(), r.getAnomalyType(), r.getAnomalyScore()))
                .doOnError(ex -> log.error(
                        "FastAPIAdapter ← error simulationId={}: {}",
                        request.getSimulationId(), ex.getMessage()))
                .block();
    }

    // ── Health check

    public boolean isHealthy() {
        try {
            String status = aiServiceWebClient
                    .get()
                    .uri(ApiConstants.AI_HEALTH)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.debug("FastAPI health: {}", status);
            return true;
        } catch (Exception ex) {
            log.warn("FastAPI health check failed: {}", ex.getMessage());
            return false;
        }
    }

    // ── Fallbacks

    /**
     * Invoked when the circuit is OPEN — all recent calls have failed.
     * Returns a structured DEGRADED response so the AI module can persist
     * a fallback insight rather than leaving the simulation with no analysis.
     */
    public FastAPIResponse analyzeFallback (FastAPIRequest request, CallNotPermittedException ex) {
        log.warn("FastAPIAdapter: circuit OPEN for simulationId={} — returning degraded response",
                request.getSimulationId());
        return buildDegradedResponse(request,
                "AI service circuit breaker open — too many recent failures");
    }

    /**
     * Invoked when the connection is refused (Python service is down).
     */
    public FastAPIResponse analyzeFallback (FastAPIRequest request, WebClientRequestException ex) {
        log.warn("FastAPIAdapter: connection refused for simulationId={} — {}",
                request.getSimulationId(), ex.getMessage());
        return buildDegradedResponse(request,
                "AI service unreachable: " + ex.getMessage());
    }

    /**
     * Invoked when all @Retry attempts are exhausted.
     */
    public FastAPIResponse analyzeFallback (FastAPIRequest request, WebClientResponseException ex) {
        log.warn("FastAPIAdapter: HTTP {} after retries for simulationId={}",
                ex.getStatusCode(), request.getSimulationId());
        return buildDegradedResponse(request,
                "AI service returned HTTP " + ex.getStatusCode() + " after retries");
    }

    /**
     * Catch-all fallback for unexpected exceptions.
     */
    public FastAPIResponse analyzeFallback (FastAPIRequest request, Exception ex) {
        log.error("FastAPIAdapter: unexpected failure for simulationId={} — {}",
                request.getSimulationId(), ex.getMessage(), ex);
        return buildDegradedResponse(request,
                "Unexpected adapter failure: " + ex.getMessage());
    }

    // ── Helpers

    private FastAPIResponse buildDegradedResponse (FastAPIRequest request, String reason) {
        FastAPIResponse degraded = new FastAPIResponse();
        // Use JsonUtils to reconstruct a minimal valid response
        String json = JsonUtils.toJson(java.util.Map.of(
                "simulation_id", request.getSimulationId(),
                "summary", "Analysis unavailable — " + reason,
                "anomaly_type", "UNKNOWN",
                "anomaly_score", 0.0,
                "predicted_trend", "STABLE",
                "degraded", true,
                "degraded_reason", reason,
                "detected_patterns", java.util.List.of(),
                "recommendations", java.util.List.of()
        ));
        return JsonUtils.fromJson(json, FastAPIResponse.class).orElse(degraded);
    }
}
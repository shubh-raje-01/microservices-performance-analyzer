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
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class RcaAdapter {

    private final WebClient aiServiceWebClient;

    @Value("${ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    @CircuitBreaker(name = "fastapi", fallbackMethod = "rcaFallback")
    @Retry(name = "fastapi")
    public RcaResponse analyze(RcaRequest request) {
        log.info("RcaAdapter → POST /api/rca serviceId={}", request.getServiceId());

        return aiServiceWebClient
                .post()
                .uri("/api/rca")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AIServiceException(
                                        "FastAPI rejected RCA request", response.statusCode().value()))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AIServiceException(
                                        "FastAPI RCA internal error — " + body,
                                        response.statusCode().value()))
                )
                .bodyToMono(RcaResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(r -> log.info(
                        "RcaAdapter ← response serviceId={} primary={}",
                        r.getServiceId(),
                        r.getPrimaryFinding() != null ? r.getPrimaryFinding().getBottleneckType() : "NONE"))
                .doOnError(ex -> log.error(
                        "RcaAdapter ← error serviceId={}: {}",
                        request.getServiceId(), ex.getMessage()))
                .block();
    }

    public RcaResponse rcaFallback(RcaRequest request, CallNotPermittedException ex) {
        log.warn("RcaAdapter: circuit OPEN for serviceId={}", request.getServiceId());
        return buildDegradedResponse(request, "AI service circuit breaker open");
    }

    public RcaResponse rcaFallback(RcaRequest request, WebClientRequestException ex) {
        log.warn("RcaAdapter: connection refused for serviceId={}", request.getServiceId());
        return buildDegradedResponse(request, "AI service unreachable: " + ex.getMessage());
    }

    public RcaResponse rcaFallback(RcaRequest request, WebClientResponseException ex) {
        log.warn("RcaAdapter: HTTP {} for serviceId={}", ex.getStatusCode(), request.getServiceId());
        return buildDegradedResponse(request, "AI service returned HTTP " + ex.getStatusCode());
    }

    public RcaResponse rcaFallback(RcaRequest request, Exception ex) {
        log.error("RcaAdapter: unexpected failure for serviceId={}", request.getServiceId(), ex);
        return buildDegradedResponse(request, "Unexpected failure: " + ex.getMessage());
    }

    private RcaResponse buildDegradedResponse(RcaRequest request, String reason) {
        String json = JsonUtils.toJson(java.util.Map.of(
                "service_id", request.getServiceId(),
                "service_name", request.getServiceName(),
                "summary", "RCA unavailable — " + reason,
                "findings", Collections.emptyList(),
                "metrics_snapshot", Collections.emptyMap()
        ));
        return JsonUtils.fromJson(json, RcaResponse.class).orElse(new RcaResponse());
    }
}

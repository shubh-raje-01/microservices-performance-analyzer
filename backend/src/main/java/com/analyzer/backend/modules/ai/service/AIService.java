package com.analyzer.backend.modules.ai.service;

import com.analyzer.backend.common.constants.CacheConstants;
import com.analyzer.backend.common.dto.response.AIInsightDto;
import com.analyzer.backend.common.dto.response.RecommendationDto;
import com.analyzer.backend.common.exceptions.AIServiceException;
import com.analyzer.backend.common.exceptions.AnalyzerException;
import com.analyzer.backend.common.utils.JsonUtils;
import com.analyzer.backend.modules.ai.adapter.FastAPIAdapter;
import com.analyzer.backend.modules.ai.adapter.FastAPIRequest;
import com.analyzer.backend.modules.ai.adapter.FastAPIResponse;
import com.analyzer.backend.modules.ai.events.AIAnalysisCompletedEvent;
import com.analyzer.backend.modules.ai.ml.AIFeatureBuilder;
import com.analyzer.backend.modules.ai.ml.AIFeatureSet;
import com.analyzer.backend.modules.ai.model.AIAnalysisStatus;
import com.analyzer.backend.modules.ai.model.AIInsight;
import com.analyzer.backend.modules.ai.model.AnomalyType;
import com.analyzer.backend.modules.ai.repository.AIInsightRepository;
import com.analyzer.backend.modules.logging.controller.LogSummaryDto;
import com.analyzer.backend.modules.logging.service.LoggingService;
import com.analyzer.backend.modules.metrics.model.AggregatedMetrics;
import com.analyzer.backend.modules.metrics.service.MetricsService;
import com.analyzer.backend.modules.simulation.model.Simulation;
import com.analyzer.backend.modules.simulation.model.SimulationStatus;
import com.analyzer.backend.modules.simulation.repository.SimulationRepository;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AIInsightRepository insightRepository;
    private final SimulationRepository simulationRepository;
    private final MetricsService metricsService;
    private final LoggingService loggingService;
    private final AIFeatureBuilder featureBuilder;
    private final FastAPIAdapter fastAPIAdapter;
    private final ApplicationEventPublisher eventPublisher;

    // ── Primary analysis flow

    /**
     * End-to-end analysis pipeline:
     *  1. Validate simulation is COMPLETED
     *  2. Fetch aggregated metrics + log summary
     *  3. Build feature set
     *  4. POST to FastAPI
     *  5. Persist AIInsight entity
     *  6. Publish AIAnalysisCompletedEvent (recommendation module listens)
     *  7. Return AIInsightDto
     *
     * Idempotent — re-running on a simulation that already has a COMPLETED
     * insight returns the cached result without calling FastAPI again.
     */
    @Transactional
    @CacheEvict(value = CacheConstants.AI_INSIGHT, key = "#simulationId")
    public AIInsightDto analyzeSimulation(Long simulationId) {
        log.info("AIService.analyzeSimulation — simulationId={}", simulationId);

        Simulation simulation = guardSimulationCompleted(simulationId);

        // Idempotency — return cached result if already analysed successfully
        if (insightRepository.existsBySimulationIdAndStatus(
                simulationId, AIAnalysisStatus.COMPLETED)) {
            log.info("AIService: insight already exists for simulationId={} — returning cached",
                    simulationId);
            return getCachedInsight(simulationId);
        }

        // ── Step 1: Persist a PROCESSING placeholder
        AIInsight insight = createProcessingPlaceholder(simulation);

        try {
            // ── Step 2: Gather features
            AggregatedMetrics metrics = fetchMetrics(simulationId);
            LogSummaryDto     logs    = fetchLogs(simulationId);

            AIFeatureSet    features = featureBuilder.build(simulation, metrics, logs);
            FastAPIRequest  request  = featureBuilder.toRequest(features);

            log.info("AIService: feature set built — p95={}ms errorRate={} healthScore={}",
                    features.getP95LatencyMs(),
                    features.getAvgErrorRate(),
                    features.getHealthScore());

            // ── Step 3: Call FastAPI
            FastAPIResponse response = fastAPIAdapter.analyze(request);

            // ── Step 4: Populate and save insight
            populateInsight(insight, response);
            AIInsight saved = insightRepository.save(insight);

            log.info("AIService: insight persisted — id={} anomaly={} score={}",
                    saved.getId(), saved.getAnomalyType(), saved.getAnomalyScore());

            // ── Step 5: Notify downstream modules
            eventPublisher.publishEvent(
                    new AIAnalysisCompletedEvent(this, saved, response));

            return toDto(saved, response);

        } catch (Exception ex) {
            log.error("AIService: analysis failed for simulationId={} — {}",
                    simulationId, ex.getMessage(), ex);
            markFailed(insight, ex.getMessage());
            throw new AIServiceException("Analysis pipeline failed: " + ex.getMessage());
        }
    }

    // ── Reads =>

    @Cacheable(value = CacheConstants.AI_INSIGHT, key = "#simulationId")
    public AIInsightDto getInsight(Long simulationId) {
        AIInsight insight = insightRepository.findTopBySimulationIdOrderByCreatedAtDesc(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("AIInsight", simulationId));
        return toDto(insight, null);
    }

    public List<AIInsight> getRecentAnomalies(int hoursBack) {
        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);
        return insightRepository.findRecentAnomalies(since);
    }

    public boolean hasCompletedAnalysis(Long simulationId) {
        return insightRepository.existsBySimulationIdAndStatus(
                simulationId, AIAnalysisStatus.COMPLETED);
    }

    // ── Delete =>

    @Transactional
    @CacheEvict(value = CacheConstants.AI_INSIGHT, key = "#simulationId")
    public void deleteForSimulation(Long simulationId) {
        insightRepository.deleteBySimulationId(simulationId);
        log.info("AI insights deleted for simulation {}", simulationId);
    }

    // ── Private — lifecycle helpers

    private AIInsight createProcessingPlaceholder(Simulation simulation) {
        AIInsight insight = AIInsight.builder()
                .simulation(simulation)
                .simulationId(simulation.getId())
                .serviceName(simulation.getTargetService())
                .status(AIAnalysisStatus.PROCESSING)
                .build();
        return insightRepository.save(insight);
    }

    private void populateInsight(AIInsight insight, FastAPIResponse response) {
        boolean degraded = response.isDegraded();

        insight.setStatus(degraded
                ? AIAnalysisStatus.DEGRADED : AIAnalysisStatus.COMPLETED);
        insight.setSummary(response.getSummary());
        insight.setAnomalyType(parseAnomalyType(response.getAnomalyType()));
        insight.setAnomalyScore(response.getAnomalyScore());
        insight.setPredictedTrend(response.getPredictedTrend());
        insight.setPredictedP95Ms(response.getPredictedP95Ms());
        insight.setDetectedPatterns(response.getDetectedPatterns());
        insight.setModelVersion(response.getModelVersion());
        insight.setAnalyzedAt(Instant.now());

        // Feature importance — convert Map<String, Double> to Map<String, Object>
        if (response.getFeatureImportance() != null) {
            insight.setFeatureImportance(
                    response.getFeatureImportance().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> (Object) e.getValue()
                            ))
            );
        }

        // Store raw recommendations JSON for the recommendation module
        if (response.getRecommendations() != null) {
            insight.setRawRecommendations(
                    JsonUtils.toJson(response.getRecommendations()));
        }

        // Full response for debugging
        insight.setRawResponse(JsonUtils.toJson(response));
    }

    @Transactional
    private void markFailed(AIInsight insight, String reason) {
        insight.setStatus(AIAnalysisStatus.FAILED);
        insight.setFailureReason(reason);
        insight.setAnalyzedAt(Instant.now());
        insightRepository.save(insight);
    }

    // ── Private — data fetching

    private AggregatedMetrics fetchMetrics(Long simulationId) {
        try {
            return metricsService.getAggregated(simulationId);
        } catch (Exception ex) {
            log.error("AIService: failed to fetch metrics for simulationId={} — {}",
                    simulationId, ex.getMessage());
            throw AnalyzerException.internalError(
                    "Metrics unavailable for simulation " + simulationId);
        }
    }

    private LogSummaryDto fetchLogs(Long simulationId) {
        try {
            return loggingService.getSummary(simulationId);
        } catch (Exception ex) {
            log.warn("AIService: could not fetch log summary for simulationId={} — " +
                    "proceeding with empty summary. Cause: {}", simulationId, ex.getMessage());
            // Logs are non-critical — analysis can proceed without them
            return LogSummaryDto.builder()
                    .simulationId(simulationId)
                    .totalCount(0).errorCount(0).warnCount(0)
                    .hasErrors(false).thresholdBreached(false)
                    .build();
        }
    }

    // ── Private — guards

    private Simulation guardSimulationCompleted(Long simulationId) {
        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

        if (sim.getStatus() != SimulationStatus.COMPLETED) {
            throw AnalyzerException.badRequest(
                    "Simulation " + simulationId + " is not yet COMPLETED — " +
                            "current status: " + sim.getStatus());
        }

        return sim;
    }

    // ── Private — mapping

    private AIInsightDto getCachedInsight(Long simulationId) {
        AIInsight existing = insightRepository
                .findTopBySimulationIdOrderByCreatedAtDesc(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("AIInsight", simulationId));
        return toDto(existing, null);
    }

    private AIInsightDto toDto(AIInsight insight, FastAPIResponse response) {
        List<RecommendationDto> recs = parseRecommendations(insight.getRawRecommendations());

        // Feature importance: convert Map<String, Object> → Map<String, Double>
        Map<String, Double> featureImportance = null;
        if (insight.getFeatureImportance() != null) {
            featureImportance = insight.getFeatureImportance().entrySet().stream()
                    .filter(e -> e.getValue() instanceof Number)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> ((Number) e.getValue()).doubleValue()
                    ));
        }

        return AIInsightDto.builder()
                .simulationId(insight.getSimulationId())
                .summary(insight.getSummary())
                .anomalyType(insight.getAnomalyType() != null
                        ? insight.getAnomalyType().name() : null)
                .anomalyScore(insight.getAnomalyScore())
                .predictedTrend(insight.getPredictedTrend())
                .predictedP95Ms(insight.getPredictedP95Ms())
                .detectedPatterns(insight.getDetectedPatterns())
                .featureImportance(featureImportance)
                .recommendations(recs)
                .build();
    }

    private AnomalyType parseAnomalyType(String raw) {
        if (raw == null) return AnomalyType.UNKNOWN;
        try {
            return AnomalyType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("AIService: unrecognised anomaly type '{}' — defaulting to UNKNOWN", raw);
            return AnomalyType.UNKNOWN;
        }
    }

    private List<RecommendationDto> parseRecommendations(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        return JsonUtils.fromJson(rawJson,
                        new TypeReference<List<RecommendationDto>>() {})
                .orElse(List.of());
    }
}

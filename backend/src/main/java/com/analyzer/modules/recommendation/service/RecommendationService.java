package com.analyzer.modules.recommendation.service;

import com.analyzer.common.constants.CacheConstants;
import com.analyzer.common.dto.response.RecommendationDto;
import com.analyzer.common.exceptions.AnalyzerException;
import com.analyzer.modules.ai.events.AIAnalysisCompletedEvent;
import com.analyzer.modules.ai.model.AIInsight;
import com.analyzer.modules.logging.controller.LogSummaryDto;
import com.analyzer.modules.logging.service.LoggingService;
import com.analyzer.modules.metrics.model.AggregatedMetrics;
import com.analyzer.modules.metrics.service.MetricsService;
import com.analyzer.modules.recommendation.engine.RecommendationEngine;
import com.analyzer.modules.recommendation.model.Recommendation;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import com.analyzer.modules.recommendation.repository.RecommendationRepository;
import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.repository.SimulationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final RecommendationRepository repository;
    private final RecommendationEngine engine;
    private final MetricsService metricsService;
    private final LoggingService loggingService;
    private final SimulationRepository simulationRepository;

    // ── Event listener

    @Async
    @EventListener
    @Transactional
    @CacheEvict(value = CacheConstants.RECOMMENDATIONS, key = "#event.simulationId")
    public void onAIAnalysisCompleted (AIAnalysisCompletedEvent event) {
        Long simulationId = event.getSimulationId();
        log.info("RecommendationService ← AIAnalysisCompletedEvent simulationId={}",
                simulationId);

        if (repository.existsBySimulationId(simulationId)) {
            log.info("Recommendations already exist for simulationId={} — skipping", simulationId);
            return;
        }

        try {
            AIInsight  insight = event.getInsight();
            Simulation sim     = simulationRepository.findById(simulationId)
                    .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

            AggregatedMetrics metrics = metricsService.getAggregated(simulationId);
            LogSummaryDto     logs    = safeGetLogs(simulationId);

            List<Recommendation> recommendations =
                    engine.process(sim, insight, metrics, logs);

            List<Recommendation> saved = repository.saveAll(recommendations);

            log.info("RecommendationService: persisted {} recommendations for simulationId={} " +
                            "(high={} medium={} low={})",
                    saved.size(), simulationId,
                    countByPriority(saved, RecommendationPriority.HIGH),
                    countByPriority(saved, RecommendationPriority.MEDIUM),
                    countByPriority(saved, RecommendationPriority.LOW));

        } catch (Exception ex) {
            // Non-fatal — recommendation failure must not break the analysis pipeline
            log.error("RecommendationService: failed for simulationId={} — {}",
                    simulationId, ex.getMessage(), ex);
        }
    }

    // ── Reads

    @Cacheable(value = CacheConstants.RECOMMENDATIONS, key = "#simulationId")
    public List<RecommendationDto> getForSimulation (Long simulationId) {
        guardSimulationExists(simulationId);
        return repository.findBySimulationIdOrderByRankAsc(simulationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<RecommendationDto> getHighPriority (Long simulationId) {
        return repository.findHighPriorityBySimulation(simulationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<RecommendationDto> getByCategory (Long simulationId,
                                                 RecommendationCategory category) {
        return repository.findBySimulationIdAndCategoryOrderByRankAsc(simulationId, category)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<RecommendationDto> getTopN (Long simulationId, int n) {
        return repository.findByScoreDesc(simulationId)
                .stream()
                .limit(Math.min(n, 20))
                .map(this::toDto)
                .toList();
    }

    public Map<String, Long> getCountsByPriority (Long simulationId) {
        return Map.of(
                "total",  repository.countBySimulationId(simulationId),
                "high",   repository.countBySimulationIdAndPriority(
                        simulationId, RecommendationPriority.HIGH),
                "medium", repository.countBySimulationIdAndPriority(
                        simulationId, RecommendationPriority.MEDIUM),
                "low",    repository.countBySimulationIdAndPriority(
                        simulationId, RecommendationPriority.LOW)
        );
    }

    // ── Delete

    @Transactional
    @CacheEvict(value = CacheConstants.RECOMMENDATIONS, key = "#simulationId")
    public void deleteForSimulation (Long simulationId) {
        repository.deleteBySimulationId(simulationId);
        log.info("Recommendations deleted for simulation {}", simulationId);
    }

    // ── Mapping

    private RecommendationDto toDto (Recommendation r) {
        return RecommendationDto.builder()
                .id(r.getId())
                .simulationId(r.getSimulationId())
                .category(r.getCategory().name())
                .priority(r.getPriority().name())
                .title(r.getTitle())
                .description(r.getDescription())
                .action(r.getAction())
                .confidenceScore(r.getConfidenceScore())
                .estimatedImpact(r.getEstimatedImpact())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ── Helpers

    private LogSummaryDto safeGetLogs (Long simulationId) {
        try {
            return loggingService.getSummary(simulationId);
        } catch (Exception ex) {
            log.warn("RecommendationService: log summary unavailable for {} — " +
                    "using empty. Cause: {}", simulationId, ex.getMessage());
            return LogSummaryDto.builder()
                    .simulationId(simulationId)
                    .totalCount(0).errorCount(0).warnCount(0)
                    .hasErrors(false).thresholdBreached(false)
                    .build();
        }
    }

    private void guardSimulationExists (Long simulationId) {
        if (!simulationRepository.existsById(simulationId)) {
            throw AnalyzerException.notFound("Simulation", simulationId);
        }
    }

    private long countByPriority (List<Recommendation> recs, RecommendationPriority priority) {
        return recs.stream().filter(r -> r.getPriority() == priority).count();
    }
}
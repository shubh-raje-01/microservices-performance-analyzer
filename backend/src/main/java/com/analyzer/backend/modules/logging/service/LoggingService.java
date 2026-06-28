package com.analyzer.backend.modules.logging.service;

import com.analyzer.backend.common.constants.CacheConstants;
import com.analyzer.backend.common.exceptions.AnalyzerException;
import com.analyzer.backend.modules.logging.controller.LogSummaryDto;
import com.analyzer.backend.modules.logging.model.LogCategory;
import com.analyzer.backend.modules.logging.model.LogEntry;
import com.analyzer.backend.modules.logging.model.LogLevel;
import com.analyzer.backend.modules.logging.processor.LogSpecification;
import com.analyzer.backend.modules.logging.repository.LogRepository;
import com.analyzer.backend.modules.simulation.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoggingService {

    private final LogRepository repository;
    private final SimulationRepository simulationRepository;

    // ── Programmatic logging API =>
    // Other services can call this to emit structured log entries
    // without going through the event system.

    @Transactional
    public LogEntry log(Long simulationId,
                        LogLevel level,
                        LogCategory category,
                        String serviceName,
                        String source,
                        String message,
                        Map<String, Object> context) {
        LogEntry entry = LogEntry.builder()
                .simulationId(simulationId)
                .level(level)
                .category(category)
                .serviceName(serviceName)
                .source(source)
                .message(message)
                .context(context)
                .occurredAt(Instant.now())
                .build();
        return repository.save(entry);
    }

    @Transactional
    public LogEntry logSystem(LogLevel level,
                              String source,
                              String message,
                              Map<String, Object> context) {
        return log(null, level, LogCategory.SYSTEM, "system", source, message, context);
    }

    // ── Reads =>

    public Page<LogEntry> getPagedForSimulation(Long simulationId,
                                                int page, int size) {
        guardSimulationExists(simulationId);
        return repository.findBySimulationIdOrderByOccurredAtDesc(
                simulationId,
                PageRequest.of(page, Math.min(size, 200))
        );
    }

    public List<LogEntry> getAllForSimulation(Long simulationId) {
        guardSimulationExists(simulationId);
        return repository.findBySimulationIdOrderByOccurredAtAsc(simulationId);
    }

    public List<LogEntry> getErrors(Long simulationId) {
        return repository.findErrorsBySimulation(simulationId);
    }

    public List<LogEntry> getProblematic(Long simulationId) {
        return repository.findProblematicBySimulation(simulationId);
    }

    public List<LogEntry> getByLevel(Long simulationId, LogLevel level) {
        return repository.findBySimulationIdAndLevel(simulationId, level);
    }

    public List<LogEntry> getByCategory(Long simulationId, LogCategory category) {
        return repository.findBySimulationIdAndCategory(simulationId, category);
    }

    public List<LogEntry> getRecentForService(String serviceName, int hoursBack) {
        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);
        return repository.findRecentByService(serviceName, since);
    }

    public List<LogEntry> getRecentSystemLogs(int limit) {
        Instant since = Instant.now().minusSeconds(86_400); // last 24 hours
        return repository.findRecentSystemLogs(
                since,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
    }

    // ── Dynamic search =>

    public Page<LogEntry> search(LogSearchCriteria criteria) {
        PageRequest pageable = PageRequest.of(
                criteria.getPage(),
                Math.min(criteria.getSize(), 200),
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );
        return repository.findAll(LogSpecification.build(criteria), pageable);
    }

    // ── Summary (cached — used by dashboard and AI module) ────

    @Cacheable(value = CacheConstants.LOG_SUMMARY, key = "#simulationId")
    public LogSummaryDto getSummary(Long simulationId) {
        log.debug("Building LogSummaryDto for simulation {}", simulationId);
        guardSimulationExists(simulationId);

        long total = repository.countBySimulationId(simulationId);

        // Build level breakdown from grouped query
        Map<String, Long> byLevel = new LinkedHashMap<>();
        repository.countGroupedByLevel(simulationId)
                .forEach(row -> byLevel.put(row[0].toString(), (Long) row[1]));

        // Build category breakdown
        Map<String, Long> byCategory = new LinkedHashMap<>();
        repository.countGroupedByCategory(simulationId)
                .forEach(row -> byCategory.put(row[0].toString(), (Long) row[1]));

        long errorCount = byLevel.getOrDefault("ERROR", 0L)
                + byLevel.getOrDefault("FATAL", 0L);
        long warnCount  = byLevel.getOrDefault("WARN",  0L);

        boolean thresholdBreached = repository
                .findProblematicBySimulation(simulationId)
                .stream()
                .anyMatch(l -> l.getCategory() == LogCategory.PERFORMANCE
                        && l.isProblematic());

        return LogSummaryDto.builder()
                .simulationId(simulationId)
                .totalCount(total)
                .countByLevel(byLevel)
                .countByCategory(byCategory)
                .errorCount(errorCount)
                .warnCount(warnCount)
                .hasErrors(errorCount > 0)
                .thresholdBreached(thresholdBreached)
                .build();
    }

    // ── Distinct values for UI filters =>

    public List<String> getDistinctServiceNames() {
        return repository.findDistinctServiceNames();
    }

    // ── Cascade delete =>

    @Transactional
    @CacheEvict(value = CacheConstants.LOG_SUMMARY, key = "#simulationId")
    public void deleteForSimulation(Long simulationId) {
        repository.deleteBySimulationId(simulationId);
        log.info("Log entries deleted for simulation {}", simulationId);
    }

    // ── Guard =>

    private void guardSimulationExists(Long simulationId) {
        if (!simulationRepository.existsById(simulationId)) {
            throw AnalyzerException.notFound("Simulation", simulationId);
        }
    }
}
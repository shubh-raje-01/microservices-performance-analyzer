package com.analyzer.modules.logging.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.PagedResponse;
import com.analyzer.modules.logging.model.LogCategory;
import com.analyzer.modules.logging.model.LogEntry;
import com.analyzer.modules.logging.model.LogLevel;
import com.analyzer.modules.logging.service.LogSearchCriteria;
import com.analyzer.modules.logging.service.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoggingController {

    private final LoggingService service;

    // ── Called by GlobalController =>

    public ResponseEntity<ApiResponse<?>> getLogsForSimulation(
            Long simulationId, int page, int size) {
        Page<LogEntry> paged = service.getPagedForSimulation(simulationId, page, size);
        PagedResponse<LogEntryDto> response = PagedResponse.of(
                paged,
                paged.getContent().stream().map(this::toDto).toList()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    public ResponseEntity<ApiResponse<?>> getLogSummary(Long simulationId) {
        LogSummaryDto summary = service.getSummary(simulationId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    public ResponseEntity<ApiResponse<?>> getErrors(Long simulationId) {
        List<LogEntryDto> dtos = service.getErrors(simulationId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    public ResponseEntity<ApiResponse<?>> getProblematic(Long simulationId) {
        List<LogEntryDto> dtos = service.getProblematic(simulationId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    public ResponseEntity<ApiResponse<?>> search(
            Long simulationId,
            String level,
            String category,
            String keyword,
            String from,
            String to,
            int page,
            int size) {

        LogSearchCriteria criteria = LogSearchCriteria.builder()
                .simulationId(simulationId)
                .level(level != null ? LogLevel.valueOf(level.toUpperCase()) : null)
                .category(category != null ? LogCategory.valueOf(category.toUpperCase()) : null)
                .keyword(keyword)
                .from(from != null ? Instant.parse(from) : null)
                .to(to != null ? Instant.parse(to)   : null)
                .page(page)
                .size(size)
                .build();

        Page<LogEntry> result = service.search(criteria);
        PagedResponse<LogEntryDto> response = PagedResponse.of(
                result,
                result.getContent().stream().map(this::toDto).toList()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── DTO projection =>

    private LogEntryDto toDto(LogEntry e) {
        return LogEntryDto.builder()
                .id(e.getId())
                .simulationId(e.getSimulationId())
                .level(e.getLevel().name())
                .category(e.getCategory().name())
                .serviceName(e.getServiceName())
                .source(e.getSource())
                .message(e.getMessage())
                .context(e.getContext())
                .stackTrace(e.getStackTrace())
                .durationMs(e.getDurationMs())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
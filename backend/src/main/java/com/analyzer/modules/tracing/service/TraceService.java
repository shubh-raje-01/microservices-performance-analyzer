package com.analyzer.modules.tracing.service;

import com.analyzer.common.exceptions.SimulationNotFoundException;
import com.analyzer.modules.tracing.dto.TraceDependencyGraphDto;
import com.analyzer.modules.tracing.dto.TraceDetailDto;
import com.analyzer.modules.tracing.dto.TraceSpanDto;
import com.analyzer.modules.tracing.dto.TraceSummaryDto;
import com.analyzer.modules.tracing.model.TraceSpan;
import com.analyzer.modules.tracing.repository.TraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceSpanRepository traceSpanRepository;

    public Page<TraceSummaryDto> getTraces(int page, int size, String serviceName, int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<String> traceIds;
        if (serviceName != null && !serviceName.isBlank()) {
            traceIds = traceSpanRepository.findDistinctTraceIdsByService(serviceName, since, pageRequest);
        } else {
            traceIds = traceSpanRepository.findDistinctTraceIds(since, pageRequest);
        }

        return traceIds.map(this::buildSummary);
    }

    public TraceDetailDto getTraceById(String traceId) {
        List<TraceSpan> spans = traceSpanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            throw new SimulationNotFoundException("Trace not found: " + traceId);
        }
        return buildDetail(traceId, spans);
    }

    public TraceDependencyGraphDto getDependencyGraph(String traceId) {
        List<TraceSpan> spans = traceSpanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            throw new SimulationNotFoundException("Trace not found: " + traceId);
        }
        return buildDependencyGraph(traceId, spans);
    }

    private TraceSummaryDto buildSummary(String traceId) {
        List<TraceSpan> spans = traceSpanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            return null;
        }

        TraceSpan root = spans.getFirst();
        List<String> serviceNames = spans.stream()
                .map(TraceSpan::getServiceName)
                .distinct()
                .collect(Collectors.toList());

        boolean hasError = spans.stream()
                .anyMatch(s -> "ERROR".equals(s.getStatusCode()));

        return TraceSummaryDto.builder()
                .traceId(traceId)
                .rootOperation(root.getOperationName())
                .serviceName(root.getServiceName())
                .startTime(root.getStartTime())
                .durationMs(spans.getLast().getEndTime().toEpochMilli()
                        - root.getStartTime().toEpochMilli())
                .spanCount(spans.size())
                .serviceNames(serviceNames)
                .status(hasError ? "ERROR" : "OK")
                .build();
    }

    private TraceDetailDto buildDetail(String traceId, List<TraceSpan> spans) {
        TraceSpan root = spans.getFirst();
        List<String> serviceNames = spans.stream()
                .map(TraceSpan::getServiceName)
                .distinct()
                .collect(Collectors.toList());

        boolean hasError = spans.stream()
                .anyMatch(s -> "ERROR".equals(s.getStatusCode()));

        List<TraceSpanDto> spanDtos = spans.stream()
                .map(this::toDto)
                .toList();

        return TraceDetailDto.builder()
                .traceId(traceId)
                .rootOperation(root.getOperationName())
                .rootService(root.getServiceName())
                .startTime(root.getStartTime())
                .durationMs(spans.getLast().getEndTime().toEpochMilli()
                        - root.getStartTime().toEpochMilli())
                .spanCount(spans.size())
                .serviceNames(serviceNames)
                .status(hasError ? "ERROR" : "OK")
                .spans(spanDtos)
                .build();
    }

    private TraceDependencyGraphDto buildDependencyGraph(String traceId, List<TraceSpan> spans) {
        Map<String, List<TraceSpan>> byService = spans.stream()
                .collect(Collectors.groupingBy(TraceSpan::getServiceName));

        Map<String, String> spanIdToService = new HashMap<>();
        spans.forEach(s -> spanIdToService.put(s.getSpanId(), s.getServiceName()));

        List<TraceDependencyGraphDto.Node> nodes = new ArrayList<>();
        for (Map.Entry<String, List<TraceSpan>> entry : byService.entrySet()) {
            List<TraceSpan> serviceSpans = entry.getValue();
            double avgDuration = serviceSpans.stream()
                    .mapToLong(TraceSpan::getDurationMs)
                    .average()
                    .orElse(0.0);
            boolean hasError = serviceSpans.stream()
                    .anyMatch(s -> "ERROR".equals(s.getStatusCode()));

            nodes.add(TraceDependencyGraphDto.Node.builder()
                    .serviceName(entry.getKey())
                    .operationCount(serviceSpans.size())
                    .avgDurationMs(avgDuration)
                    .status(hasError ? "ERROR" : "OK")
                    .build());
        }

        Map<String, TraceDependencyGraphDto.Edge> edgeMap = new LinkedHashMap<>();
        for (TraceSpan span : spans) {
            if (span.getParentSpanId() == null) continue;

            String targetService = span.getServiceName();
            String sourceService = spanIdToService.get(span.getParentSpanId());

            if (sourceService == null || sourceService.equals(targetService)) continue;

            String edgeKey = sourceService + "->" + targetService;
            edgeMap.computeIfAbsent(edgeKey, k ->
                    TraceDependencyGraphDto.Edge.builder()
                            .source(sourceService)
                            .target(targetService)
                            .callCount(0)
                            .avgDurationMs(0)
                            .operationName(span.getOperationName())
                            .build()
            );

            TraceDependencyGraphDto.Edge edge = edgeMap.get(edgeKey);
            edge.setCallCount(edge.getCallCount() + 1);
            edge.setAvgDurationMs(
                    (edge.getAvgDurationMs() * (edge.getCallCount() - 1) + span.getDurationMs())
                            / edge.getCallCount());
        }

        return TraceDependencyGraphDto.builder()
                .traceId(traceId)
                .nodes(nodes)
                .edges(new ArrayList<>(edgeMap.values()))
                .build();
    }

    private TraceSpanDto toDto(TraceSpan span) {
        return TraceSpanDto.builder()
                .traceId(span.getTraceId())
                .spanId(span.getSpanId())
                .parentSpanId(span.getParentSpanId())
                .serviceName(span.getServiceName())
                .operationName(span.getOperationName())
                .spanKind(span.getSpanKind())
                .startTime(span.getStartTime())
                .durationMs(span.getDurationMs())
                .statusCode(span.getStatusCode())
                .statusMessage(span.getStatusMessage())
                .attributes(span.getAttributes())
                .build();
    }
}

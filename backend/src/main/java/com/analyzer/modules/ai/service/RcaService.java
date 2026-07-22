package com.analyzer.modules.ai.service;

import com.analyzer.common.utils.JsonUtils;
import com.analyzer.modules.ai.adapter.RcaAdapter;
import com.analyzer.modules.ai.adapter.RcaRequest;
import com.analyzer.modules.ai.adapter.RcaResponse;
import com.analyzer.modules.ai.model.RcaFindingEntity;
import com.analyzer.modules.ai.repository.RcaFindingRepository;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.repository.ServiceMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcaService {

    private final RcaAdapter rcaAdapter;
    private final RcaFindingRepository rcaFindingRepository;
    private final ServiceMetricsRepository serviceMetricsRepository;

    @Transactional
    public RcaResponse analyze(String serviceId, String serviceName, int hours) {
        log.info("RCA analysis requested for service={} ({}h)", serviceName, serviceId);

        Map<String, Double> latestMetrics = getLatestMetrics(serviceId);
        List<ServiceMetrics> historical = getHistoricalMetrics(serviceId, hours);

        RcaRequest request = buildRcaRequest(serviceId, serviceName, latestMetrics, historical, hours);

        RcaResponse response = rcaAdapter.analyze(request);

        persistFindings(response);

        return response;
    }

    public Page<RcaFindingEntity> getHistory(String serviceId, int page, int size) {
        return rcaFindingRepository.findByServiceIdOrderByAnalyzedAtDesc(serviceId, PageRequest.of(page, size));
    }

    public List<RcaFindingEntity> getRecent(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return rcaFindingRepository.findByServiceAndSince(serviceId, since);
    }

    public List<Object[]> getBottleneckStats(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return rcaFindingRepository.countByBottleneckType(serviceId, since);
    }

    private Map<String, Double> getLatestMetrics(String serviceId) {
        Instant since = Instant.now().minusSeconds(300);
        List<Object[]> averages = serviceMetricsRepository.findMetricAverages(serviceId, since);
        return averages.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Double) row[1],
                        (a, b) -> b
                ));
    }

    private List<ServiceMetrics> getHistoricalMetrics(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return serviceMetricsRepository.findTimeSeriesByService(serviceId, since);
    }

    private RcaRequest buildRcaRequest(
            String serviceId, String serviceName,
            Map<String, Double> metrics,
            List<ServiceMetrics> historical,
            int hours) {

        List<RcaRequest.HistoricalMetricPoint> history = historical.stream()
                .map(m -> RcaRequest.HistoricalMetricPoint.builder()
                        .timestamp(m.getTimestamp().toString())
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .build())
                .collect(Collectors.toList());

        return RcaRequest.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .cpu(RcaRequest.CpuMetrics.builder()
                        .systemCpuUsage(getMetric(metrics, "system.cpu.usage"))
                        .processCpuUsage(getMetric(metrics, "process.cpu.usage"))
                        .build())
                .memory(RcaRequest.MemoryMetrics.builder()
                        .heapUsedBytes(getMetric(metrics, "jvm.heap.used"))
                        .heapMaxBytes(getMetric(metrics, "jvm.heap.max"))
                        .heapCommittedBytes(getMetric(metrics, "jvm.heap.committed"))
                        .heapUsagePercent(getMetric(metrics, "jvm.heap.usage.percent"))
                        .build())
                .latency(RcaRequest.LatencyMetrics.builder()
                        .avgDurationSeconds(getMetric(metrics, "http.request.duration.avg"))
                        .maxDurationSeconds(getMetric(metrics, "http.request.duration.max"))
                        .totalRequests((int) getMetric(metrics, "http.request.count"))
                        .build())
                .gc(RcaRequest.GcMetrics.builder()
                        .gcPauseSumSeconds(getMetric(metrics, "jvm.gc.pause.sum"))
                        .gcPauseCount((int) getMetric(metrics, "jvm.gc.pause.count"))
                        .gcLiveDataBytes(getMetric(metrics, "jvm.gc.liveDataSize"))
                        .gcMaxDataBytes(getMetric(metrics, "jvm.gc.maxDataSize"))
                        .build())
                .threads(RcaRequest.ThreadMetrics.builder()
                        .tomcatThreadsBusy((int) getMetric(metrics, "tomcat.threads.busy"))
                        .tomcatThreadsCurrent((int) getMetric(metrics, "tomcat.threads.current"))
                        .build())
                .httpErrors(RcaRequest.HttpErrorMetrics.builder()
                        .totalRequests((int) getMetric(metrics, "http.request.count"))
                        .errorRequests(0)
                        .errorRate(0.0)
                        .build())
                .connectionPool(RcaRequest.ConnectionPoolMetrics.builder()
                        .hikariActive((int) getMetric(metrics, "hikaricp.connections.active"))
                        .hikariIdle((int) getMetric(metrics, "hikaricp.connections.idle"))
                        .hikariPending((int) getMetric(metrics, "hikaricp.connections.pending"))
                        .hikariMaxPoolSize(10)
                        .build())
                .health(RcaRequest.HealthStatus.builder()
                        .status("UNKNOWN")
                        .latencyMs(0)
                        .responseCode(200)
                        .build())
                .historicalMetrics(history)
                .hoursBack(hours)
                .build();
    }

    private void persistFindings(RcaResponse response) {
        if (response.getFindings() == null || response.getFindings().isEmpty()) {
            return;
        }

        Instant analyzedAt = Instant.now();

        for (RcaResponse.RcaFinding finding : response.getFindings()) {
            RcaFindingEntity entity = new RcaFindingEntity();
            entity.setServiceId(response.getServiceId());
            entity.setServiceName(response.getServiceName());
            entity.setBottleneckType(finding.getBottleneckType());
            entity.setConfidence(finding.getConfidence());
            entity.setReasoning(finding.getReasoning());
            entity.setEvidenceJson(JsonUtils.toJson(finding.getEvidence()));
            entity.setRecommendationsJson(JsonUtils.toJson(finding.getRecommendations()));
            entity.setSummary(response.getSummary());
            entity.setMetricsSnapshotJson(JsonUtils.toJson(response.getMetricsSnapshot()));
            entity.setModelVersion(response.getModelVersion());
            entity.setAnalyzedAt(analyzedAt);
            rcaFindingRepository.save(entity);
        }

        log.info("Persisted {} RCA findings for service={}", response.getFindings().size(), response.getServiceId());
    }

    private double getMetric(Map<String, Double> metrics, String key) {
        return metrics.getOrDefault(key, 0.0);
    }
}

package com.analyzer.service_registry.service;

import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.repository.ServiceMetricsRepository;
import com.analyzer.service_registry.repository.ServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class ExternalMetricsCollectorService {

    private final ServiceRepository serviceRepository;
    private final ServiceMetricsRepository metricsRepository;
    private final PrometheusMetricsParser parser;
    private final WebClient webClient;

    public ExternalMetricsCollectorService(
            ServiceRepository serviceRepository,
            ServiceMetricsRepository metricsRepository,
            PrometheusMetricsParser parser,
            @Qualifier("monitoringWebClient") WebClient webClient) {
        this.serviceRepository = serviceRepository;
        this.metricsRepository = metricsRepository;
        this.parser = parser;
        this.webClient = webClient;
    }

    @Scheduled(fixedDelayString = "${monitoring.metrics.interval:30000}", initialDelay = 10000)
    public void collectAllMetrics() {
        List<Service> enabledServices = serviceRepository.findAllEnabled();
        if (enabledServices.isEmpty()) {
            return;
        }

        log.debug("Metrics collection cycle — collecting from {} services", enabledServices.size());

        for (Service service : enabledServices) {
            if (service.getMetricsEndpoint() == null || service.getMetricsEndpoint().isBlank()) {
                continue;
            }
            try {
                collectMetrics(service);
            } catch (Exception ex) {
                log.error("Metrics collection failed for {} — {}", service.getName(), ex.getMessage());
            }
        }
    }

    public void collectMetrics(Service service) {
        String metricsUrl = buildMetricsUrl(service);

        String rawMetrics = webClient.get()
                .uri(metricsUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (rawMetrics == null || rawMetrics.isBlank()) {
            log.debug("No metrics returned from {}", service.getName());
            return;
        }

        PrometheusMetricsParser.ParsedMetrics parsed = parser.parse(rawMetrics);
        Instant now = Instant.now();

        Map<String, Double> keyMetrics = parsed.getKeyMetrics();

        for (Map.Entry<String, Double> entry : keyMetrics.entrySet()) {
            ServiceMetrics metric = new ServiceMetrics();
            metric.setService(service);
            metric.setMetricName(entry.getKey());
            metric.setMetricValue(entry.getValue());
            metric.setMetricType(inferMetricType(entry.getKey()));
            metric.setTimestamp(now);
            metricsRepository.save(metric);
        }

        log.debug("Collected {} metrics from {}", keyMetrics.size(), service.getName());
    }

    private String buildMetricsUrl(Service service) {
        String base = service.getBaseUrl().replaceAll("/+$", "");
        String endpoint = service.getMetricsEndpoint();
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    private String inferMetricType(String metricName) {
        if (metricName.contains("cpu")) return "CPU";
        if (metricName.contains("memory") || metricName.contains("heap")) return "MEMORY";
        if (metricName.contains("gc")) return "GC";
        if (metricName.contains("thread")) return "THREAD";
        if (metricName.contains("http") || metricName.contains("request")) return "HTTP";
        if (metricName.contains("hikaricp")) return "CONNECTION_POOL";
        if (metricName.contains("tomcat")) return "TOMCAT";
        return "OTHER";
    }

    public List<ServiceMetrics> getMetricTrend(String serviceId, String metricName, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findMetricTrend(serviceId, metricName, since);
    }

    public Map<String, Double> getLatestMetrics(String serviceId) {
        Instant since = Instant.now().minusSeconds(300);
        List<Object[]> averages = metricsRepository.findMetricAverages(serviceId, since);
        return averages.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Double) row[1]
                ));
    }
}

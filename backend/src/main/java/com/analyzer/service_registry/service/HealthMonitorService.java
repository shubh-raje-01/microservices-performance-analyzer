package com.analyzer.service_registry.service;

import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceHealthHistory;
import com.analyzer.service_registry.model.ServiceStatus;
import com.analyzer.service_registry.repository.ServiceHealthHistoryRepository;
import com.analyzer.service_registry.repository.ServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;

@Slf4j
@org.springframework.stereotype.Service
public class HealthMonitorService {

    private final ServiceRepository serviceRepository;
    private final ServiceHealthHistoryRepository healthHistoryRepository;
    private final WebClient webClient;

    public HealthMonitorService(
            ServiceRepository serviceRepository,
            ServiceHealthHistoryRepository healthHistoryRepository,
            @Qualifier("monitoringWebClient") WebClient webClient) {
        this.serviceRepository = serviceRepository;
        this.healthHistoryRepository = healthHistoryRepository;
        this.webClient = webClient;
    }

    @Scheduled(fixedDelayString = "${monitoring.health.interval:15000}", initialDelay = 5000)
    public void checkAllServices() {
        List<Service> enabledServices = serviceRepository.findAllEnabled();
        if (enabledServices.isEmpty()) {
            return;
        }

        log.debug("Health check cycle — checking {} services", enabledServices.size());

        for (Service service : enabledServices) {
            try {
                checkServiceHealth(service);
            } catch (Exception ex) {
                log.error("Health check failed for service {} — {}", service.getName(), ex.getMessage());
                recordFailure(service, ex, null);
            }
        }
    }

    public void checkServiceHealth(Service service) {
        String healthUrl = buildHealthUrl(service);
        long startMs = System.currentTimeMillis();

        try {
            String body = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            long latencyMs = System.currentTimeMillis() - startMs;
            String bodyLower = body != null ? body.toLowerCase() : "";

            ServiceStatus status;
            if (bodyLower.contains("\"status\":\"down\"") || bodyLower.contains("\"status\":\"out_of_service\"")) {
                status = ServiceStatus.OFFLINE;
            } else if (latencyMs > 5000) {
                status = ServiceStatus.DEGRADED;
            } else {
                status = ServiceStatus.ONLINE;
            }

            recordHealthResult(service, status, latencyMs, 200, null);
            updateServiceStatus(service, status);

        } catch (WebClientResponseException ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            int statusCode = ex.getStatusCode().value();
            ServiceStatus status = statusCode >= 500 ? ServiceStatus.OFFLINE : ServiceStatus.DEGRADED;

            recordHealthResult(service, status, latencyMs, statusCode, truncateMessage(ex.getMessage()));
            updateServiceStatus(service, status);

        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            recordFailure(service, ex, latencyMs);
        }
    }

    private void recordHealthResult(Service service, ServiceStatus status, Long latencyMs,
                                     Integer responseCode, String errorMessage) {
        ServiceHealthHistory history = new ServiceHealthHistory();
        history.setService(service);
        history.setStatus(status);
        history.setLatencyMs(latencyMs);
        history.setResponseCode(responseCode);
        history.setCheckTime(Instant.now());
        history.setErrorMessage(errorMessage);
        healthHistoryRepository.save(history);

        log.debug("Health check: {} → {} ({}ms)", service.getName(), status, latencyMs);
    }

    private void recordFailure(Service service, Exception ex, Long latencyMs) {
        ServiceHealthHistory history = new ServiceHealthHistory();
        history.setService(service);
        history.setStatus(ServiceStatus.OFFLINE);
        history.setLatencyMs(latencyMs);
        history.setResponseCode(null);
        history.setCheckTime(Instant.now());
        history.setErrorMessage(truncateMessage(ex.getMessage()));
        healthHistoryRepository.save(history);

        updateServiceStatus(service, ServiceStatus.OFFLINE);
        log.warn("Health check FAILED: {} — {}", service.getName(), ex.getMessage());
    }

    private void updateServiceStatus(Service service, ServiceStatus newStatus) {
        if (service.getStatus() != newStatus) {
            ServiceStatus oldStatus = service.getStatus();
            service.setStatus(newStatus);
            service.setLastHeartbeat(Instant.now());
            serviceRepository.save(service);
            log.info("Service {} status: {} → {}", service.getName(), oldStatus, newStatus);
        } else {
            service.setLastHeartbeat(Instant.now());
            serviceRepository.save(service);
        }
    }

    private String buildHealthUrl(Service service) {
        String base = service.getBaseUrl().replaceAll("/+$", "");
        String endpoint = service.getHealthEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return base + "/actuator/health";
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }

    public List<ServiceHealthHistory> getRecentHealthHistory(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return healthHistoryRepository.findRecentByService(serviceId, since);
    }

    public List<ServiceHealthHistory> getRecentHealthHistoryAll(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return healthHistoryRepository.findRecentAll(since);
    }

    public Double getAverageLatency(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return healthHistoryRepository.averageLatencyByService(serviceId, since);
    }

    public long getFailureCount(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return healthHistoryRepository.countFailuresByService(serviceId, since);
    }
}

package com.analyzer.service_registry.service;

import com.analyzer.common.constants.CacheConstants;
import com.analyzer.common.exceptions.AnalyzerException;
import com.analyzer.service_registry.dto.ServiceRequestDto;
import com.analyzer.service_registry.dto.ServiceResponseDto;
import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceStatus;
import com.analyzer.service_registry.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceRegistryService {

    private final ServiceRepository repository;

    @Transactional
    @CacheEvict(value = CacheConstants.SERVICES_LIST, allEntries = true)
    public ServiceResponseDto create(ServiceRequestDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw AnalyzerException.conflict("Service with name '" + dto.getName() + "' already exists");
        }

        Service service = new Service();
        service.setName(dto.getName());
        service.setBaseUrl(dto.getBaseUrl());
        service.setHealthEndpoint(dto.getHealthEndpoint());
        service.setMetricsEndpoint(dto.getMetricsEndpoint());
        service.setDescription(dto.getDescription());
        service.setTags(dto.getTags());
        service.setStatus(ServiceStatus.UNKNOWN);

        Service saved = repository.save(service);
        log.info("Service registered — id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SERVICES_LIST, allEntries = true)
    public ServiceResponseDto update(String id, ServiceRequestDto dto) {
        Service service = getById(id);

        if (repository.existsByNameAndIdNot(dto.getName(), id)) {
            throw AnalyzerException.conflict("Service with name '" + dto.getName() + "' already exists");
        }

        service.setName(dto.getName());
        service.setBaseUrl(dto.getBaseUrl());
        service.setHealthEndpoint(dto.getHealthEndpoint());
        service.setMetricsEndpoint(dto.getMetricsEndpoint());
        service.setDescription(dto.getDescription());
        service.setTags(dto.getTags());

        Service saved = repository.save(service);
        log.info("Service updated — id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SERVICES_LIST, allEntries = true)
    public void delete(String id) {
        Service service = getById(id);
        repository.delete(service);
        log.info("Service deleted — id={} name={}", id, service.getName());
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SERVICES_LIST, allEntries = true)
    public ServiceResponseDto enable(String id) {
        Service service = getById(id);
        service.setDisabledAt(null);
        service.setStatus(ServiceStatus.UNKNOWN);
        Service saved = repository.save(service);
        log.info("Service enabled — id={}", id);
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SERVICES_LIST, allEntries = true)
    public ServiceResponseDto disable(String id) {
        Service service = getById(id);
        service.setDisabledAt(Instant.now());
        service.setStatus(ServiceStatus.DISABLED);
        Service saved = repository.save(service);
        log.info("Service disabled — id={}", id);
        return toDto(saved);
    }

    @Cacheable(value = CacheConstants.SERVICE_BY_ID, key = "#id")
    public Service getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> AnalyzerException.notFound("Service", id));
    }

    public ServiceResponseDto getByIdAsDto(String id) {
        return toDto(getById(id));
    }

    public List<ServiceResponseDto> listAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ServiceResponseDto> listEnabled() {
        return repository.findAllEnabled().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<ServiceResponseDto> listPaged(int page, int size) {
        return repository.findAll(PageRequest.of(page, Math.min(size, 100)))
                .map(this::toDto);
    }

    public List<ServiceResponseDto> search(String name) {
        return repository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public long countEnabled() {
        return repository.findAllEnabled().size();
    }

    public ServiceResponseDto toDto(Service s) {
        return ServiceResponseDto.builder()
                .id(s.getId())
                .name(s.getName())
                .baseUrl(s.getBaseUrl())
                .healthEndpoint(s.getHealthEndpoint())
                .metricsEndpoint(s.getMetricsEndpoint())
                .description(s.getDescription())
                .tags(s.getTags())
                .status(s.getStatus())
                .enabled(s.isEnabled())
                .lastHeartbeat(s.getLastHeartbeat())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}

package com.analyzer.service_registry.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.PagedResponse;
import com.analyzer.service_registry.dto.ServiceRequestDto;
import com.analyzer.service_registry.dto.ServiceResponseDto;
import com.analyzer.service_registry.service.ServiceRegistryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ServiceRegistryController {

    private final ServiceRegistryService service;

    public ResponseEntity<ApiResponse<?>> create(
            @Valid @RequestBody ServiceRequestDto dto) {
        ServiceResponseDto created = service.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service registered", created));
    }

    public ResponseEntity<ApiResponse<?>> update(
            String id, @Valid @RequestBody ServiceRequestDto dto) {
        ServiceResponseDto updated = service.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Service updated", updated));
    }

    public ResponseEntity<ApiResponse<?>> delete(String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
    }

    public ResponseEntity<ApiResponse<?>> getById(String id) {
        ServiceResponseDto dto = service.getByIdAsDto(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    public ResponseEntity<ApiResponse<?>> listAll() {
        List<ServiceResponseDto> services = service.listAll();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    public ResponseEntity<ApiResponse<?>> listPaged(int page, int size) {
        Page<ServiceResponseDto> pageResult = service.listPaged(page, size);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(pageResult)));
    }

    public ResponseEntity<ApiResponse<?>> search(String name) {
        List<ServiceResponseDto> results = service.search(name);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    public ResponseEntity<ApiResponse<?>> enable(String id) {
        ServiceResponseDto dto = service.enable(id);
        return ResponseEntity.ok(ApiResponse.success("Service enabled", dto));
    }

    public ResponseEntity<ApiResponse<?>> disable(String id) {
        ServiceResponseDto dto = service.disable(id);
        return ResponseEntity.ok(ApiResponse.success("Service disabled", dto));
    }
}

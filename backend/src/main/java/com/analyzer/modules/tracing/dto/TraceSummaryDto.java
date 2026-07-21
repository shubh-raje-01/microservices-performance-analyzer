package com.analyzer.modules.tracing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceSummaryDto {

    private String traceId;
    private String rootOperation;
    private String serviceName;
    private Instant startTime;
    private long durationMs;
    private int spanCount;
    private List<String> serviceNames;
    private String status;
}

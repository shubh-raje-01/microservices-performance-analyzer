package com.analyzer.modules.tracing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceDependencyGraphDto {

    private String traceId;
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Node {
        private String serviceName;
        private int operationCount;
        private double avgDurationMs;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Edge {
        private String source;
        private String target;
        private int callCount;
        private double avgDurationMs;
        private String operationName;
    }
}

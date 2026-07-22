package com.analyzer.service_registry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PrometheusMetricsParser {

    private static final Pattern HELP_PATTERN = Pattern.compile("^# HELP (\\S+) (.+)$");
    private static final Pattern TYPE_PATTERN = Pattern.compile("^# TYPE (\\S+) (.+)$");
    private static final Pattern METRIC_LINE_PATTERN = Pattern.compile("^(\\S+)(\\{[^}]*\\})?\\s+(.+?)(?:\\s+(\\d+))?$");

    private static final Set<String> RELEVANT_METRICS = Set.of(
            "jvm_memory_used_bytes",
            "jvm_memory_max_bytes",
            "jvm_memory_committed_bytes",
            "jvm_gc_pause_seconds_sum",
            "jvm_gc_pause_seconds_count",
            "jvm_gc_live_data_size_bytes",
            "jvm_gc_max_data_size_bytes",
            "system_cpu_usage",
            "process_cpu_usage",
            "tomcat_threads_busy",
            "tomcat_threads_current",
            "http_server_requests_seconds_sum",
            "http_server_requests_seconds_count",
            "http_server_requests_seconds_max",
            "http_server_requests_seconds_bucket",
            "hikaricp_connections_active",
            "hikaricp_connections_idle",
            "hikaricp_connections_pending"
    );

    public static final String CATEGORY_CPU = "CPU";
    public static final String CATEGORY_MEMORY = "MEMORY";
    public static final String CATEGORY_LATENCY = "LATENCY";
    public static final String CATEGORY_REQUESTS = "REQUESTS";
    public static final String CATEGORY_GC = "GC";
    public static final String CATEGORY_THREAD = "THREAD";
    public static final String CATEGORY_CONNECTION_POOL = "CONNECTION_POOL";
    public static final String CATEGORY_TOMCAT = "TOMCAT";
    public static final String CATEGORY_OTHER = "OTHER";

    public ParsedMetrics parse(String prometheusText) {
        if (prometheusText == null || prometheusText.isBlank()) {
            return new ParsedMetrics(Map.of(), Map.of());
        }

        Map<String, MetricInfo> metricDefs = new LinkedHashMap<>();
        Map<String, List<MetricValue>> rawMetrics = new LinkedHashMap<>();

        for (String line : prometheusText.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                Matcher helpMatcher = HELP_PATTERN.matcher(line);
                Matcher typeMatcher = TYPE_PATTERN.matcher(line);
                if (helpMatcher.matches()) {
                    String metricName = helpMatcher.group(1);
                    metricDefs.computeIfAbsent(metricName,
                            k -> new MetricInfo()).help = helpMatcher.group(2);
                } else if (typeMatcher.matches()) {
                    String metricName = typeMatcher.group(1);
                    metricDefs.computeIfAbsent(metricName,
                            k -> new MetricInfo()).type = typeMatcher.group(2);
                }
                continue;
            }

            Matcher metricMatcher = METRIC_LINE_PATTERN.matcher(line);
            if (metricMatcher.matches()) {
                String fullName = metricMatcher.group(1);
                String labels = metricMatcher.group(2);
                String valueStr = metricMatcher.group(3);

                String metricName = extractBaseMetricName(fullName);
                if (!isRelevantMetric(metricName)) {
                    continue;
                }

                try {
                    double value = Double.parseDouble(valueStr);
                    Map<String, String> parsedLabels = parseLabels(labels);
                    rawMetrics.computeIfAbsent(metricName, k -> new ArrayList<>())
                            .add(new MetricValue(value, parsedLabels));
                } catch (NumberFormatException e) {
                    log.debug("Skipping unparseable metric value: {}", valueStr);
                }
            }
        }

        Map<String, Double> extracted = extractKeyMetrics(rawMetrics);
        Map<String, Object> detailed = buildDetailedMetrics(rawMetrics, metricDefs);

        return new ParsedMetrics(extracted, detailed);
    }

    private Map<String, Double> extractKeyMetrics(Map<String, List<MetricValue>> raw) {
        Map<String, Double> result = new LinkedHashMap<>();

        extractSum(raw, result, "jvm_memory_used_bytes", "jvm.heap.used");
        extractSum(raw, result, "jvm_memory_max_bytes", "jvm.heap.max");
        extractSum(raw, result, "jvm_memory_committed_bytes", "jvm.heap.committed");
        extractSum(raw, result, "system_cpu_usage", "system.cpu.usage");
        extractSum(raw, result, "process_cpu_usage", "process.cpu.usage");
        extractSum(raw, result, "jvm_gc_pause_seconds_sum", "jvm.gc.pause.sum");
        extractSum(raw, result, "jvm_gc_pause_seconds_count", "jvm.gc.pause.count");
        extractSum(raw, result, "jvm_gc_live_data_size_bytes", "jvm.gc.liveDataSize");
        extractSum(raw, result, "jvm_gc_max_data_size_bytes", "jvm.gc.maxDataSize");
        extractMax(raw, result, "http_server_requests_seconds_max", "http.request.duration.max");
        extractSum(raw, result, "http_server_requests_seconds_sum", "http.request.duration.sum");
        extractSum(raw, result, "http_server_requests_seconds_count", "http.request.count");
        extractSum(raw, result, "tomcat_threads_busy", "tomcat.threads.busy");
        extractSum(raw, result, "tomcat_threads_current", "tomcat.threads.current");
        extractSum(raw, result, "hikaricp_connections_active", "hikaricp.connections.active");
        extractSum(raw, result, "hikaricp_connections_idle", "hikaricp.connections.idle");
        extractSum(raw, result, "hikaricp_connections_pending", "hikaricp.connections.pending");

        if (result.containsKey("jvm.heap.used") && result.containsKey("jvm.heap.max")) {
            double used = result.get("jvm.heap.used");
            double max = result.get("jvm.heap.max");
            if (max > 0) {
                result.put("jvm.heap.usage.percent", (used / max) * 100.0);
            }
        }

        if (result.containsKey("http.request.duration.sum") && result.containsKey("http.request.count")) {
            double sum = result.get("http.request.duration.sum");
            long count = result.get("http.request.count").longValue();
            if (count > 0) {
                result.put("http.request.duration.avg", sum / count);
            }
        }

        return result;
    }

    private Map<String, Object> buildDetailedMetrics(
            Map<String, List<MetricValue>> raw,
            Map<String, MetricInfo> defs) {

        Map<String, Object> detailed = new LinkedHashMap<>();

        for (Map.Entry<String, List<MetricValue>> entry : raw.entrySet()) {
            String name = entry.getKey();
            List<MetricValue> values = entry.getValue();

            MetricInfo info = defs.getOrDefault(name, new MetricInfo());

            if ("counter".equals(info.type)) {
                double total = values.stream().mapToDouble(v -> v.value).sum();
                detailed.put(name, Map.of("type", "counter", "value", total));
            } else if ("gauge".equals(info.type)) {
                double avg = values.stream().mapToDouble(v -> v.value).average().orElse(0);
                detailed.put(name, Map.of("type", "gauge", "value", avg, "samples", values.size()));
            } else if ("summary".equals(info.type)) {
                double avg = values.stream().mapToDouble(v -> v.value).average().orElse(0);
                detailed.put(name, Map.of("type", "summary", "avg", avg, "samples", values.size()));
            } else {
                double avg = values.stream().mapToDouble(v -> v.value).average().orElse(0);
                detailed.put(name, Map.of("type", info.type != null ? info.type : "unknown", "value", avg));
            }
        }

        return detailed;
    }

    private void extractSum(Map<String, List<MetricValue>> raw,
                            Map<String, Double> result,
                            String metricName, String key) {
        List<MetricValue> values = raw.get(metricName);
        if (values != null && !values.isEmpty()) {
            result.put(key, values.stream().mapToDouble(v -> v.value).sum());
        }
    }

    private void extractMax(Map<String, List<MetricValue>> raw,
                            Map<String, Double> result,
                            String metricName, String key) {
        List<MetricValue> values = raw.get(metricName);
        if (values != null && !values.isEmpty()) {
            result.put(key, values.stream().mapToDouble(v -> v.value).max().orElse(0));
        }
    }

    private boolean isRelevantMetric(String metricName) {
        return RELEVANT_METRICS.stream().anyMatch(metricName::startsWith);
    }

    private String extractBaseMetricName(String fullName) {
        int braceIdx = fullName.indexOf('{');
        if (braceIdx > 0) {
            return fullName.substring(0, braceIdx);
        }
        return fullName;
    }

    private Map<String, String> parseLabels(String labelsStr) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (labelsStr == null || labelsStr.isEmpty()) {
            return labels;
        }

        String clean = labelsStr.replaceAll("^\\{|\\}$", "");
        String[] pairs = clean.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                labels.put(kv[0].trim(), kv[1].trim().replace("\"", ""));
            }
        }
        return labels;
    }

    public static String categorizeMetric(String metricName) {
        if (metricName == null) return CATEGORY_OTHER;
        String lower = metricName.toLowerCase();
        if (lower.contains("cpu")) return CATEGORY_CPU;
        if (lower.contains("memory") || lower.contains("heap")) return CATEGORY_MEMORY;
        if (lower.contains("gc")) return CATEGORY_GC;
        if (lower.contains("thread")) return CATEGORY_THREAD;
        if (lower.contains("http") || lower.contains("request") || lower.contains("latency")
                || lower.contains("duration")) return CATEGORY_LATENCY;
        if (lower.contains("hikaricp") || lower.contains("connection")) return CATEGORY_CONNECTION_POOL;
        if (lower.contains("tomcat")) return CATEGORY_TOMCAT;
        return CATEGORY_OTHER;
    }

    public static class ParsedMetrics {
        private final Map<String, Double> keyMetrics;
        private final Map<String, Object> detailedMetrics;

        public ParsedMetrics(Map<String, Double> keyMetrics, Map<String, Object> detailedMetrics) {
            this.keyMetrics = keyMetrics;
            this.detailedMetrics = detailedMetrics;
        }

        public Map<String, Double> getKeyMetrics() { return keyMetrics; }
        public Map<String, Object> getDetailedMetrics() { return detailedMetrics; }
    }

    private static class MetricInfo {
        String help = "";
        String type = "unknown";
    }

    private static class MetricValue {
        final double value;
        final Map<String, String> labels;

        MetricValue(double value, Map<String, String> labels) {
            this.value = value;
            this.labels = labels;
        }
    }
}

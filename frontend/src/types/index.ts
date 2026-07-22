export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
  error: string | null
  timestamp: string
}

export type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type SimulationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export type SimulationResponse = {
  id: number
  scenarioName: string
  targetService: string
  durationSeconds: number
  concurrentUsers: number
  errorRateThreshold: number
  status: SimulationStatus
  avgLatencyMs: number | null
  p95LatencyMs: number | null
  p99LatencyMs: number | null
  throughputRps: number | null
  actualErrorRate: number | null
  totalRequests: number | null
  failedRequests: number | null
  failureReason: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export type AnalysisRequestDto = {
  scenarioName: string
  targetService: string
  durationSeconds: number
  concurrentUsers: number
  errorRateThreshold: number
  customParams?: Record<string, unknown>
}

export type MetricType = 'LATENCY' | 'THROUGHPUT' | 'ERROR_RATE' | 'CONCURRENCY' | 'SATURATION' | 'AVAILABILITY'

export type MetricSeverity = 'NORMAL' | 'WARNING' | 'CRITICAL'

export type MetricSnapshotDto = {
  id: number
  simulationId: number
  serviceName: string
  metricType: MetricType
  value: number
  unit: string
  avgMs: number | null
  p95Ms: number | null
  p99Ms: number | null
  maxMs: number | null
  totalRequests: number | null
  failedRequests: number | null
  severity: MetricSeverity
  notes: string | null
  recordedAt: string
}

export type MetricsSummaryDto = {
  simulationId: number
  targetService: string
  avgLatencyMs: number | null
  p50LatencyMs: number | null
  p95LatencyMs: number | null
  p99LatencyMs: number | null
  maxLatencyMs: number | null
  throughputRps: number | null
  totalRequests: number | null
  failedRequests: number | null
  errorRate: number | null
  healthScore: number | null
  healthStatus: string | null
}

export type LogLevel = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR' | 'FATAL'

export type LogCategory = 'SIMULATION' | 'PERFORMANCE' | 'METRICS' | 'AI_ANALYSIS' | 'RECOMMENDATION' | 'SYSTEM' | 'AUDIT'

export type LogEntryDto = {
  id: number
  simulationId: number
  level: LogLevel
  category: LogCategory
  serviceName: string
  source: string
  message: string
  context: string | null
  stackTrace: string | null
  durationMs: number | null
  occurredAt: string
}

export type LogSummaryDto = {
  simulationId: number
  totalCount: number
  countByLevel: Record<LogLevel, number>
  countByCategory: Record<LogCategory, number>
  errorCount: number
  warnCount: number
  hasErrors: boolean
  thresholdBreached: boolean
}

export type AnomalyType = 'NONE' | 'LATENCY_SPIKE' | 'ERROR_BURST' | 'THROUGHPUT_DROP' | 'SATURATION' | 'MEMORY_PRESSURE' | 'CASCADING_FAILURE' | 'UNKNOWN'

export type PredictedTrend = 'IMPROVING' | 'STABLE' | 'DEGRADING'

export type AIInsightDto = {
  simulationId: number
  summary: string
  anomalyType: AnomalyType
  anomalyScore: number
  predictedTrend: PredictedTrend
  predictedP95Ms: number
  detectedPatterns: string[]
  featureImportance: Record<string, number>
  recommendations: string[]
}

export type RecommendationCategory = 'LATENCY' | 'ERROR_RATE' | 'THROUGHPUT' | 'SCALING' | 'MEMORY' | 'CONFIGURATION' | 'ARCHITECTURE'

export type RecommendationPriority = 'HIGH' | 'MEDIUM' | 'LOW'

export type RecommendationDto = {
  id: number
  simulationId: number
  category: RecommendationCategory
  priority: RecommendationPriority
  title: string
  description: string
  action: string
  confidenceScore: number
  estimatedImpact: number
  createdAt: string
}

export type RecommendationStats = {
  total: number
  high: number
  medium: number
  low: number
}

export type DashboardSummaryDto = {
  simulationId: number
  scenarioName: string
  targetService: string
  simulationStatus: SimulationStatus
  metrics: MetricsSummaryDto | null
  aiInsight: AIInsightDto | null
  recommendations: RecommendationDto[]
  simulationStartedAt: string | null
  simulationCompletedAt: string | null
  generatedAt: string
}

export type DashboardOverviewDto = {
  simulationId: number
  scenarioName: string
  targetService: string
  simulationStatus: SimulationStatus
  p95LatencyMs: number | null
  errorRate: number | null
  throughputRps: number | null
  healthStatus: string | null
  anomalyType: AnomalyType
  hasAnomaly: boolean
  highPriorityRecommendationCount: number
  totalRecommendationCount: number
  completedAt: string | null
}

export type SystemHealthDto = {
  totalServicesAnalysed: number
  healthyCount: number
  degradedCount: number
  criticalCount: number
  criticalServiceNames: string[]
  degradedServiceNames: string[]
  averageHealthScore: number
}

// ─── Distributed Tracing ─────────────────────────────────────────

export type TraceSpanDto = {
  traceId: string
  spanId: string
  parentSpanId: string | null
  serviceName: string
  operationName: string
  spanKind: string
  startTime: string
  durationMs: number
  statusCode: string
  statusMessage: string | null
  attributes: Record<string, unknown> | null
}

export type TraceSummaryDto = {
  traceId: string
  rootOperation: string
  serviceName: string
  startTime: string
  durationMs: number
  spanCount: number
  serviceNames: string[]
  status: string
}

export type TraceDetailDto = {
  traceId: string
  rootOperation: string
  rootService: string
  startTime: string
  durationMs: number
  spanCount: number
  serviceNames: string[]
  status: string
  spans: TraceSpanDto[]
}

export type TraceDependencyNode = {
  serviceName: string
  operationCount: number
  avgDurationMs: number
  status: string
}

export type TraceDependencyEdge = {
  source: string
  target: string
  callCount: number
  avgDurationMs: number
  operationName: string
}

export type TraceDependencyGraphDto = {
  traceId: string
  nodes: TraceDependencyNode[]
  edges: TraceDependencyEdge[]
}

// ─── Grafana Monitoring ───────────────────────────────────────────

export type GrafanaServiceHealthEntry = {
  serviceId: string
  serviceName: string
  status: string
  latencyMs: number | null
  lastHeartbeat: string | null
}

export type GrafanaServiceHealthWidget = {
  totalServices: number
  onlineCount: number
  degradedCount: number
  offlineCount: number
  services: GrafanaServiceHealthEntry[]
}

export type GrafanaMetricTrendPoint = {
  time: string
  value: number
}

export type GrafanaServiceCpuEntry = {
  serviceId: string
  serviceName: string
  usagePercent: number
}

export type GrafanaCpuWidget = {
  averageUsagePercent: number
  maxUsagePercent: number
  trend: GrafanaMetricTrendPoint[]
  perService: GrafanaServiceCpuEntry[]
}

export type GrafanaServiceMemoryEntry = {
  serviceId: string
  serviceName: string
  usagePercent: number
  usedBytes: number
  maxBytes: number
}

export type GrafanaMemoryWidget = {
  averageUsagePercent: number
  maxUsagePercent: number
  totalUsedBytes: number
  totalMaxBytes: number
  trend: GrafanaMetricTrendPoint[]
  perService: GrafanaServiceMemoryEntry[]
}

export type GrafanaLatencyWidget = {
  averageMs: number
  p50Ms: number
  p95Ms: number
  p99Ms: number
  maxMs: number
  trend: GrafanaMetricTrendPoint[]
}

export type GrafanaRequestRateWidget = {
  totalRps: number
  totalRequests: number
  successfulRequests: number
  trend: GrafanaMetricTrendPoint[]
}

export type GrafanaErrorRateWidget = {
  errorRatePercent: number
  totalErrors: number
  totalRequests: number
  trend: GrafanaMetricTrendPoint[]
}

export type GrafanaTopSlowestEntry = {
  serviceId: string
  serviceName: string
  averageLatencyMs: number
  checkCount: number
}

export type GrafanaTopSlowestWidget = {
  services: GrafanaTopSlowestEntry[]
}

export type GrafanaFailureEntry = {
  serviceId: string
  serviceName: string
  errorMessage: string
  occurredAt: string
}

export type GrafanaRecentFailuresWidget = {
  failures: GrafanaFailureEntry[]
  totalFailures: number
}

export type GrafanaGraphNode = {
  serviceName: string
  status: string
  avgLatencyMs: number
  spanCount: number
}

export type GrafanaGraphEdge = {
  source: string
  target: string
  callCount: number
  avgDurationMs: number
}

export type GrafanaDependencyGraphWidget = {
  nodes: GrafanaGraphNode[]
  edges: GrafanaGraphEdge[]
}

export type GrafanaWidgetData = {
  serviceHealth: GrafanaServiceHealthWidget | null
  cpu: GrafanaCpuWidget | null
  memory: GrafanaMemoryWidget | null
  latency: GrafanaLatencyWidget | null
  requestRate: GrafanaRequestRateWidget | null
  errorRate: GrafanaErrorRateWidget | null
  topSlowest: GrafanaTopSlowestWidget | null
  recentFailures: GrafanaRecentFailuresWidget | null
  dependencyGraph: GrafanaDependencyGraphWidget | null
  generatedAt: string
}

export type GrafanaWebSocketMessage = {
  topic: string
  data: GrafanaWidgetData
}

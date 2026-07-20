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

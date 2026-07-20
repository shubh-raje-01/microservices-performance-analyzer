import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, CheckCircle, AlertCircle, XCircle, Eye } from 'lucide-react'
import { useRecentDashboards, useSystemHealth, useDashboardSummary } from '../hooks/useDashboard'
import { useAnalyzerStore } from '../store'
import { StatusBadge } from '../components/ui/StatusBadge'
import { HealthBadge } from '../components/ui/HealthBadge'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { formatLatency, formatErrorRate, formatThroughput, timeAgo, anomalyColour } from '../lib/utils'
import type { DashboardOverviewDto } from '../types'

export default function Dashboard() {
  const { data: healthData, isLoading: healthLoading, error: healthError } = useSystemHealth()
  const { data: recentData, isLoading: recentLoading, error: recentError } = useRecentDashboards(10)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const navigate = useNavigate()
  const setActiveSimulationId = useAnalyzerStore((s) => s.setActiveSimulationId)

  const health = healthData?.data
  const recent = recentData?.data

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-gray-900">Dashboard</h2>

      {/* System Health */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">System Health</h3>
        {healthLoading ? (
          <LoadingSpinner size="sm" />
        ) : healthError ? (
          <ErrorCard message={healthError.message} />
        ) : health ? (
          <div>
            <div className="grid grid-cols-3 gap-4 mb-4">
              <div className="rounded-xl border border-gray-200 p-4 bg-green-50">
                <div className="flex items-center gap-2 mb-1">
                  <CheckCircle className="h-4 w-4 text-green-600" />
                  <span className="text-xs font-medium text-green-700">Healthy</span>
                </div>
                <p className="text-2xl font-bold text-green-800">{health.healthyCount}</p>
              </div>
              <div className="rounded-xl border border-gray-200 p-4 bg-amber-50">
                <div className="flex items-center gap-2 mb-1">
                  <AlertCircle className="h-4 w-4 text-amber-600" />
                  <span className="text-xs font-medium text-amber-700">Degraded</span>
                </div>
                <p className="text-2xl font-bold text-amber-800">{health.degradedCount}</p>
              </div>
              <div className="rounded-xl border border-gray-200 p-4 bg-red-50">
                <div className="flex items-center gap-2 mb-1">
                  <XCircle className="h-4 w-4 text-red-600" />
                  <span className="text-xs font-medium text-red-700">Critical</span>
                </div>
                <p className="text-2xl font-bold text-red-800">{health.criticalCount}</p>
              </div>
            </div>

            {health.criticalServiceNames.length > 0 && (
              <p className="text-xs text-red-600 mb-2">
                <strong>Critical:</strong> {health.criticalServiceNames.join(', ')}
              </p>
            )}
            {health.degradedServiceNames.length > 0 && (
              <p className="text-xs text-amber-600">
                <strong>Degraded:</strong> {health.degradedServiceNames.join(', ')}
              </p>
            )}
          </div>
        ) : (
          <p className="text-sm text-gray-500">No health data available</p>
        )}
      </div>

      {/* Recent Simulations */}
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Recent Simulations</h3>
        {recentLoading ? (
          <LoadingSpinner label="Loading recent simulations..." />
        ) : recentError ? (
          <ErrorCard message={recentError.message} />
        ) : !recent || recent.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 bg-white rounded-xl border border-gray-200">
            <AlertTriangle className="h-10 w-10 text-gray-300 mb-2" />
            <p className="text-gray-500 mb-3">No simulations yet</p>
            <button
              onClick={() => navigate('/simulate')}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Run your first simulation
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {recent.map((dto) => (
              <DashboardCard
                key={dto.simulationId}
                dto={dto}
                onViewDashboard={() => {
                  setActiveSimulationId(dto.simulationId)
                  setSelectedId(dto.simulationId)
                }}
                onViewMetrics={() => navigate(`/metrics/${dto.simulationId}`)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Summary Drawer */}
      {selectedId && <SummaryDrawer simulationId={selectedId} onClose={() => setSelectedId(null)} />}
    </div>
  )
}

function DashboardCard({
  dto,
  onViewDashboard,
  onViewMetrics,
}: {
  dto: DashboardOverviewDto
  onViewDashboard: () => void
  onViewMetrics: () => void
}) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-start justify-between mb-3">
        <div>
          <p className="font-medium text-gray-900">{dto.scenarioName}</p>
          <p className="text-xs text-gray-500">{dto.targetService}</p>
        </div>
        <StatusBadge status={dto.simulationStatus} />
      </div>

      <div className="space-y-2 text-sm mb-4">
        <div className="flex justify-between">
          <span className="text-gray-500">p95 Latency</span>
          <span className="text-gray-900 font-medium">{formatLatency(dto.p95LatencyMs)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Error Rate</span>
          <span className="text-gray-900 font-medium">{formatErrorRate(dto.errorRate)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Throughput</span>
          <span className="text-gray-900 font-medium">{formatThroughput(dto.throughputRps)}</span>
        </div>
      </div>

      {dto.healthStatus && (
        <div className="mb-3">
          <HealthBadge status={dto.healthStatus} />
        </div>
      )}

      {dto.hasAnomaly && (
        <span
          className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium mb-3 ${anomalyColour[dto.anomalyType] || 'bg-gray-100 text-gray-700'}`}
        >
          {dto.anomalyType}
        </span>
      )}

      {dto.highPriorityRecommendationCount > 0 && (
        <p className="text-xs text-red-600 mb-2">
          {dto.highPriorityRecommendationCount} high-priority recommendation{dto.highPriorityRecommendationCount > 1 ? 's' : ''}
        </p>
      )}

      <p className="text-xs text-gray-400 mb-3">{timeAgo(dto.completedAt)}</p>

      <div className="flex gap-2">
        <button
          onClick={onViewDashboard}
          className="flex-1 inline-flex items-center justify-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
        >
          <Eye className="h-3 w-3" />
          Full Dashboard
        </button>
        <button
          onClick={onViewMetrics}
          className="flex-1 inline-flex items-center justify-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
        >
          Metrics
        </button>
      </div>
    </div>
  )
}

function SummaryDrawer({
  simulationId,
  onClose,
}: {
  simulationId: number
  onClose: () => void
}) {
  const { data, isLoading } = useDashboardSummary(simulationId)

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-black/20" onClick={onClose} />
      <div className="relative w-full max-w-lg bg-white shadow-xl overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900">Dashboard Summary</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-lg">&times;</button>
        </div>

        <div className="p-6 space-y-6">
          {isLoading ? (
            <LoadingSpinner />
          ) : data?.data ? (
            <>
              {data.data.metrics && (
                <div className="bg-gray-50 rounded-xl p-4">
                  <h4 className="text-sm font-semibold text-gray-900 mb-3">Metrics</h4>
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <div>
                      <span className="text-gray-500">Avg Latency</span>
                      <p className="font-medium">{formatLatency(data.data.metrics.avgLatencyMs)}</p>
                    </div>
                    <div>
                      <span className="text-gray-500">p95 Latency</span>
                      <p className="font-medium">{formatLatency(data.data.metrics.p95LatencyMs)}</p>
                    </div>
                    <div>
                      <span className="text-gray-500">Error Rate</span>
                      <p className="font-medium">{formatErrorRate(data.data.metrics.errorRate)}</p>
                    </div>
                    <div>
                      <span className="text-gray-500">Throughput</span>
                      <p className="font-medium">{formatThroughput(data.data.metrics.throughputRps)}</p>
                    </div>
                  </div>
                </div>
              )}

              {data.data.aiInsight && (
                <div className="bg-blue-50 rounded-xl p-4">
                  <h4 className="text-sm font-semibold text-gray-900 mb-2">AI Insight</h4>
                  <p className="text-sm text-gray-700 mb-2">{data.data.aiInsight.summary}</p>
                  <div className="flex gap-2">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${anomalyColour[data.data.aiInsight.anomalyType]}`}>
                      {data.data.aiInsight.anomalyType}
                    </span>
                    <span className="text-xs text-gray-500">Trend: {data.data.aiInsight.predictedTrend}</span>
                  </div>
                </div>
              )}

              {data.data.recommendations.length > 0 && (
                <div>
                  <h4 className="text-sm font-semibold text-gray-900 mb-3">Top Recommendations</h4>
                  <div className="space-y-2">
                    {data.data.recommendations.slice(0, 3).map((rec) => (
                      <div key={rec.id} className="bg-white rounded-lg border border-gray-200 p-3">
                        <p className="text-sm font-medium text-gray-900">{rec.title}</p>
                        <p className="text-xs text-gray-500 mt-1">{rec.description}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            <p className="text-sm text-gray-500">No summary data available</p>
          )}
        </div>
      </div>
    </div>
  )
}

import { useParams } from 'react-router-dom'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts'
import { useMetricsSummary, useMetrics } from '../hooks/useMetrics'
import { HealthBadge } from '../components/ui/HealthBadge'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { severityColour } from '../lib/utils'
import { formatLatency, formatErrorRate, formatThroughput, formatDateTime } from '../lib/utils'
import type { MetricSnapshotDto } from '../types'

export default function MetricsView() {
  const { id } = useParams<{ id: string }>()
  const simulationId = id ? Number(id) : null

  const {
    data: summaryData,
    isLoading: summaryLoading,
    error: summaryError,
  } = useMetricsSummary(simulationId)
  const {
    data: metricsData,
    isLoading: metricsLoading,
    error: metricsError,
  } = useMetrics(simulationId)

  const summary = summaryData?.data
  const metrics = metricsData?.data

  if (summaryLoading || metricsLoading) return <LoadingSpinner label="Loading metrics..." />
  if (summaryError) return <ErrorCard message={summaryError.message} />
  if (metricsError) return <ErrorCard message={metricsError.message} />

  const latencyData =
    metrics?.map((m: MetricSnapshotDto) => ({
      type: m.metricType,
      avg: m.avgMs ?? m.value,
      p95: m.p95Ms ?? 0,
      p99: m.p99Ms ?? 0,
    })) || []

  const throughputData =
    metrics
      ?.filter((m: MetricSnapshotDto) => m.metricType === 'THROUGHPUT')
      .map((m: MetricSnapshotDto) => ({
        time: formatDateTime(m.recordedAt),
        rps: m.value,
      })) || []

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-gray-900">Metrics</h2>

      {/* KPI Tiles */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">p95 Latency</p>
          <p className="text-2xl font-bold text-gray-900">{formatLatency(summary?.p95LatencyMs)}</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Error Rate</p>
          <p className="text-2xl font-bold text-gray-900">{formatErrorRate(summary?.errorRate)}</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Throughput</p>
          <p className="text-2xl font-bold text-gray-900">{formatThroughput(summary?.throughputRps)}</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <p className="text-sm text-gray-500 mb-1">Health</p>
          <HealthBadge status={summary?.healthStatus ?? null} score={summary?.healthScore} />
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Latency Chart */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Latency by Metric Type</h3>
          {latencyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={latencyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="type" tick={{ fontSize: 11, fill: '#6b7280' }} />
                <YAxis tick={{ fontSize: 11, fill: '#6b7280' }} />
                <Tooltip
                  contentStyle={{ backgroundColor: 'white', border: '1px solid #e5e7eb', borderRadius: '0.5rem', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}
                />
                <Bar dataKey="avg" name="Avg" fill="#2563eb" />
                <Bar dataKey="p95" name="p95" fill="#d97706" />
                <Bar dataKey="p99" name="p99" fill="#dc2626" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-gray-400 text-center py-8">No latency data available</p>
          )}
        </div>

        {/* Throughput Chart */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Throughput Over Time</h3>
          {throughputData.length > 0 ? (
            <ResponsiveContainer width="100%" height={250}>
              <AreaChart data={throughputData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#6b7280' }} />
                <YAxis tick={{ fontSize: 11, fill: '#6b7280' }} />
                <Tooltip
                  contentStyle={{ backgroundColor: 'white', border: '1px solid #e5e7eb', borderRadius: '0.5rem', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}
                />
                <Area type="monotone" dataKey="rps" name="req/s" stroke="#2563eb" fill="#dbeafe" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-gray-400 text-center py-8">No throughput data available</p>
          )}
        </div>
      </div>

      {/* Snapshot Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-sm font-semibold text-gray-900">Metric Snapshots</h3>
        </div>
        {metrics && metrics.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Value</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Unit</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Severity</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Notes</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Recorded At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {metrics.map((m: MetricSnapshotDto) => (
                  <tr
                    key={m.id}
                    className={`hover:bg-gray-50 ${m.severity === 'CRITICAL' ? 'border-l-4 border-l-red-500' : ''}`}
                  >
                    <td className="px-4 py-3 text-gray-900 font-medium">{m.metricType}</td>
                    <td className="px-4 py-3 text-gray-700">{m.value.toFixed(2)}</td>
                    <td className="px-4 py-3 text-gray-500">{m.unit}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${severityColour[m.severity]}`}>
                        {m.severity}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 max-w-xs truncate">{m.notes || '—'}</td>
                    <td className="px-4 py-3 text-gray-500">{formatDateTime(m.recordedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-gray-400 text-center py-8">No metric snapshots available</p>
        )}
      </div>
    </div>
  )
}

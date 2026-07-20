import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarChart3, FileText, Brain, Trash2, ChevronLeft, ChevronRight } from 'lucide-react'
import { useSimulationList, useDeleteSimulation } from '../hooks/useSimulation'
import { useAnalyze } from '../hooks/useAI'
import { useAnalyzerStore } from '../store'
import { StatusBadge } from '../components/ui/StatusBadge'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { formatLatency, formatErrorRate, formatThroughput, formatDuration } from '../lib/utils'

export default function SimulationHistory() {
  const [page, setPage] = useState(0)
  const size = 10
  const { data, isLoading, error, refetch } = useSimulationList(page, size)
  const deleteMutation = useDeleteSimulation()
  const analyzeMutation = useAnalyze()
  const navigate = useNavigate()
  const setActiveSimulationId = useAnalyzerStore((s) => s.setActiveSimulationId)

  const paged = data?.data

  if (isLoading) return <LoadingSpinner label="Loading simulations..." />
  if (error) return <ErrorCard message={error.message} onRetry={() => refetch()} />

  if (!paged || paged.content.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <BarChart3 className="h-12 w-12 text-gray-300 mb-3" />
        <p className="text-gray-500 mb-4">No simulations yet</p>
        <button
          onClick={() => navigate('/simulate')}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Run your first simulation
        </button>
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Simulation History</h2>

      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Scenario</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Service</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">p95</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Error Rate</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Throughput</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Duration</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wide text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paged.content.map((sim) => (
                <tr key={sim.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-900 font-medium">#{sim.id}</td>
                  <td className="px-4 py-3 text-gray-900">{sim.scenarioName}</td>
                  <td className="px-4 py-3 text-gray-500">{sim.targetService}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={sim.status} />
                  </td>
                  <td className="px-4 py-3 text-gray-700">{formatLatency(sim.p95LatencyMs)}</td>
                  <td className="px-4 py-3 text-gray-700">{formatErrorRate(sim.actualErrorRate)}</td>
                  <td className="px-4 py-3 text-gray-700">{formatThroughput(sim.throughputRps)}</td>
                  <td className="px-4 py-3 text-gray-500">
                    {formatDuration(sim.startedAt, sim.completedAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => {
                          setActiveSimulationId(sim.id)
                          navigate(`/metrics/${sim.id}`)
                        }}
                        className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100"
                        title="View Dashboard"
                      >
                        <BarChart3 className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => navigate(`/logs/${sim.id}`)}
                        className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100"
                        title="View Logs"
                      >
                        <FileText className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => {
                          analyzeMutation.mutate(sim.id, {
                            onSuccess: () => navigate(`/insights/${sim.id}`),
                          })
                        }}
                        disabled={analyzeMutation.isPending}
                        className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
                        title="Analyze with AI"
                      >
                        <Brain className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => {
                          if (confirm('Delete this simulation?')) {
                            deleteMutation.mutate(sim.id)
                          }
                        }}
                        className="rounded-lg p-1.5 text-red-500 hover:bg-red-50"
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3">
          <span className="text-xs text-gray-500">
            Page {paged.page + 1} of {paged.totalPages} ({paged.totalElements} total)
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={paged.first}
              className="rounded-lg border border-gray-300 px-2 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={paged.last}
              className="rounded-lg border border-gray-300 px-2 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

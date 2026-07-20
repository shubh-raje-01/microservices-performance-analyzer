import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useRecommendations, useRecommendationStats } from '../hooks/useRecommendations'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { priorityColour, timeAgo, cn } from '../lib/utils'
import type { RecommendationDto, RecommendationPriority, RecommendationCategory } from '../types'

const PRIORITY_FILTERS: (RecommendationPriority | 'ALL')[] = ['ALL', 'HIGH', 'MEDIUM', 'LOW']
const CATEGORY_FILTERS: RecommendationCategory[] = [
  'LATENCY', 'ERROR_RATE', 'THROUGHPUT', 'SCALING', 'MEMORY', 'CONFIGURATION', 'ARCHITECTURE',
]

export default function Recommendations() {
  const { id } = useParams<{ id: string }>()
  const simulationId = id ? Number(id) : null

  const [priorityFilter, setPriorityFilter] = useState<RecommendationPriority | 'ALL'>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<RecommendationCategory | null>(null)

  const { data: recsData, isLoading: recsLoading, error: recsError, refetch } = useRecommendations(simulationId)
  const { data: statsData, isLoading: statsLoading } = useRecommendationStats(simulationId)

  const recs = recsData?.data
  const stats = statsData?.data

  if (recsLoading || statsLoading) return <LoadingSpinner label="Loading recommendations..." />
  if (recsError) return <ErrorCard message={recsError.message} onRetry={() => refetch()} />

  const filtered = (recs || []).filter((r: RecommendationDto) => {
    if (priorityFilter !== 'ALL' && r.priority !== priorityFilter) return false
    if (categoryFilter && r.category !== categoryFilter) return false
    return true
  })

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-gray-900">Recommendations</h2>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-4 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-xs text-gray-500 mb-1">Total</p>
            <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-xs text-gray-500 mb-1">High</p>
            <p className="text-2xl font-bold text-red-600">{stats.high}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-xs text-gray-500 mb-1">Medium</p>
            <p className="text-2xl font-bold text-amber-600">{stats.medium}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-xs text-gray-500 mb-1">Low</p>
            <p className="text-2xl font-bold text-blue-600">{stats.low}</p>
          </div>
        </div>
      )}

      {/* Priority Tabs */}
      <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
        {PRIORITY_FILTERS.map((p) => (
          <button
            key={p}
            onClick={() => setPriorityFilter(p)}
            className={cn(
              'rounded-md px-3 py-1.5 text-xs font-medium transition-colors',
              priorityFilter === p ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700',
            )}
          >
            {p}
          </button>
        ))}
      </div>

      {/* Category Chips */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setCategoryFilter(null)}
          className={cn(
            'rounded-full px-2.5 py-0.5 text-xs font-medium border transition-colors',
            !categoryFilter ? 'bg-gray-900 text-white border-gray-900' : 'border-gray-300 text-gray-600 hover:bg-gray-50',
          )}
        >
          All Categories
        </button>
        {CATEGORY_FILTERS.map((c) => (
          <button
            key={c}
            onClick={() => setCategoryFilter(categoryFilter === c ? null : c)}
            className={cn(
              'rounded-full px-2.5 py-0.5 text-xs font-medium border transition-colors',
              categoryFilter === c ? 'bg-gray-900 text-white border-gray-900' : 'border-gray-300 text-gray-600 hover:bg-gray-50',
            )}
          >
            {c.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Recommendation Cards */}
      {filtered.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-xl border border-gray-200">
          <p className="text-gray-500">No recommendations match the current filters</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((rec: RecommendationDto, index: number) => (
            <div
              key={rec.id}
              className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden"
            >
              <div className="px-6 py-4 border-b border-gray-100 flex items-center gap-3">
                <span className="text-sm font-bold text-gray-400">#{index + 1}</span>
                <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${priorityColour[rec.priority]}`}>
                  {rec.priority}
                </span>
                <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-700">
                  {rec.category.replace('_', ' ')}
                </span>
              </div>

              <div className="px-6 py-4 space-y-3">
                <h4 className="text-base font-semibold text-gray-900">{rec.title}</h4>
                <p className="text-sm text-gray-600">{rec.description}</p>

                <div className="bg-gray-50 rounded-lg p-3">
                  <p className="text-xs font-medium text-gray-500 mb-1">Recommended Action</p>
                  <p className="text-sm text-gray-700">{rec.action}</p>
                </div>
              </div>

              <div className="px-6 py-3 border-t border-gray-100 flex items-center gap-6">
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs text-gray-500">Confidence</span>
                    <span className="text-xs font-medium text-gray-700">{(rec.confidenceScore * 100).toFixed(0)}%</span>
                  </div>
                  <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-600 rounded-full"
                      style={{ width: `${rec.confidenceScore * 100}%` }}
                    />
                  </div>
                </div>

                <div className="flex-1">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs text-gray-500">Est. Impact</span>
                    <span className="text-xs font-medium text-gray-700">{(rec.estimatedImpact * 100).toFixed(0)}%</span>
                  </div>
                  <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-green-600 rounded-full"
                      style={{ width: `${rec.estimatedImpact * 100}%` }}
                    />
                  </div>
                </div>

                <span className="text-xs text-gray-400">{timeAgo(rec.createdAt)}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

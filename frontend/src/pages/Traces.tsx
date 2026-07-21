import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Network, ChevronLeft, ChevronRight, Clock, Layers, AlertCircle } from 'lucide-react'
import { useTraces } from '../hooks/useTraces'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { timeAgo } from '../lib/utils'
import type { TraceSummaryDto } from '../types'

export default function Traces() {
  const [page, setPage] = useState(0)
  const [serviceFilter, setServiceFilter] = useState('')
  const [hoursBack, setHoursBack] = useState(24)
  const navigate = useNavigate()

  const { data, isLoading, error } = useTraces({
    page,
    size: 20,
    serviceName: serviceFilter || undefined,
    hoursBack,
  })

  const traces = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Network className="h-5 w-5 text-blue-600" />
        <h2 className="text-lg font-semibold text-gray-900">Distributed Tracing</h2>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 bg-white rounded-xl border border-gray-200 shadow-sm p-4">
        <div className="flex items-center gap-2">
          <label className="text-xs font-medium text-gray-500">Service</label>
          <input
            type="text"
            value={serviceFilter}
            onChange={(e) => { setServiceFilter(e.target.value); setPage(0) }}
            placeholder="All services"
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm w-48 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs font-medium text-gray-500">Last</label>
          <select
            value={hoursBack}
            onChange={(e) => { setHoursBack(Number(e.target.value)); setPage(0) }}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value={1}>1 hour</option>
            <option value={6}>6 hours</option>
            <option value={24}>24 hours</option>
            <option value={72}>3 days</option>
            <option value={168}>7 days</option>
          </select>
        </div>
        <div className="ml-auto text-xs text-gray-400">
          {data?.data?.totalElements ?? 0} trace{(data?.data?.totalElements ?? 0) !== 1 ? 's' : ''}
        </div>
      </div>

      {/* Trace List */}
      {isLoading ? (
        <LoadingSpinner label="Loading traces..." />
      ) : error ? (
        <ErrorCard message={error.message} />
      ) : traces.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 bg-white rounded-xl border border-gray-200">
          <Network className="h-10 w-10 text-gray-300 mb-2" />
          <p className="text-gray-500">No traces recorded yet</p>
          <p className="text-xs text-gray-400 mt-1">
            Traces appear after requests hit instrumented endpoints
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {traces.map((trace) => (
            <TraceRow
              key={trace.traceId}
              trace={trace}
              onClick={() => navigate(`/traces/${trace.traceId}`)}
            />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-40"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-40"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  )
}

function TraceRow({ trace, onClick }: { trace: TraceSummaryDto; onClick: () => void }) {
  const isError = trace.status === 'ERROR'

  return (
    <button
      onClick={onClick}
      className="w-full text-left bg-white rounded-xl border border-gray-200 shadow-sm p-4 hover:border-blue-300 hover:shadow-md transition-all"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-medium text-gray-900 text-sm truncate">
              {trace.rootOperation}
            </span>
            <span
              className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                isError
                  ? 'bg-red-50 text-red-700'
                  : 'bg-green-50 text-green-700'
              }`}
            >
              {trace.status}
            </span>
          </div>

          <p className="text-xs text-gray-500 font-mono truncate mb-2">
            {trace.traceId}
          </p>

          <div className="flex items-center gap-4 text-xs text-gray-500">
            <span className="flex items-center gap-1">
              <Layers className="h-3 w-3" />
              {trace.spanCount} span{trace.spanCount !== 1 ? 's' : ''}
            </span>
            <span className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {trace.durationMs < 1000
                ? `${trace.durationMs}ms`
                : `${(trace.durationMs / 1000).toFixed(1)}s`}
            </span>
            {trace.serviceNames.length > 0 && (
              <span className="flex items-center gap-1">
                {trace.serviceNames.slice(0, 3).map((s) => (
                  <span
                    key={s}
                    className="inline-flex items-center rounded bg-gray-100 px-1.5 py-0.5 text-[10px] font-medium text-gray-700"
                  >
                    {s}
                  </span>
                ))}
                {trace.serviceNames.length > 3 && (
                  <span className="text-[10px] text-gray-400">
                    +{trace.serviceNames.length - 3}
                  </span>
                )}
              </span>
            )}
          </div>
        </div>

        <div className="text-right shrink-0">
          <p className="text-xs text-gray-400">{timeAgo(trace.startTime)}</p>
          {isError && <AlertCircle className="h-4 w-4 text-red-500 mt-1 ml-auto" />}
        </div>
      </div>
    </button>
  )
}

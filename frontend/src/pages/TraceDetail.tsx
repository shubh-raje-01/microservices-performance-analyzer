import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Clock, Layers, CheckCircle, XCircle } from 'lucide-react'
import { useTraceDetail, useTraceGraph } from '../hooks/useTraces'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import DependencyGraph from '../components/tracing/DependencyGraph'
import type { TraceSpanDto } from '../types'

export default function TraceDetail() {
  const { traceId } = useParams<{ traceId: string }>()
  const navigate = useNavigate()
  const { data: traceData, isLoading: traceLoading, error: traceError } = useTraceDetail(traceId)
  const { data: graphData } = useTraceGraph(traceId)

  const trace = traceData?.data
  const graph = graphData?.data

  if (traceLoading) return <LoadingSpinner label="Loading trace..." />
  if (traceError) return <ErrorCard message={traceError.message} />
  if (!trace) return <ErrorCard message="Trace not found" />

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate('/traces')}
          className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex-1 min-w-0">
          <h2 className="text-lg font-semibold text-gray-900 truncate">
            {trace.rootOperation}
          </h2>
          <p className="text-xs text-gray-500 font-mono truncate">{trace.traceId}</p>
        </div>
        <span
          className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${
            trace.status === 'ERROR'
              ? 'bg-red-50 text-red-700'
              : 'bg-green-50 text-green-700'
          }`}
        >
          {trace.status}
        </span>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-4 gap-4">
        <SummaryCard
          label="Duration"
          value={trace.durationMs < 1000 ? `${trace.durationMs}ms` : `${(trace.durationMs / 1000).toFixed(1)}s`}
          icon={<Clock className="h-4 w-4 text-blue-600" />}
        />
        <SummaryCard
          label="Spans"
          value={String(trace.spanCount)}
          icon={<Layers className="h-4 w-4 text-purple-600" />}
        />
        <SummaryCard
          label="Services"
          value={String(trace.serviceNames.length)}
          icon={<span className="text-xs">🔗</span>}
        />
        <SummaryCard
          label="Root Service"
          value={trace.rootService}
          icon={<span className="text-xs">📦</span>}
        />
      </div>

      {/* Dependency Graph */}
      {graph && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Request Flow</h3>
          <DependencyGraph data={graph} />
        </div>
      )}

      {/* Span Waterfall */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Span Waterfall</h3>
        <SpanWaterfall spans={trace.spans} totalDurationMs={trace.durationMs} />
      </div>
    </div>
  )
}

function SummaryCard({
  label,
  value,
  icon,
}: {
  label: string
  value: string
  icon: React.ReactNode
}) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4">
      <div className="flex items-center gap-2 mb-1">
        {icon}
        <span className="text-xs font-medium text-gray-500">{label}</span>
      </div>
      <p className="text-lg font-bold text-gray-900 truncate">{value}</p>
    </div>
  )
}

function SpanWaterfall({
  spans,
  totalDurationMs,
}: {
  spans: TraceSpanDto[]
  totalDurationMs: number
}) {
  if (!spans.length) {
    return <p className="text-sm text-gray-500">No spans in this trace</p>
  }

  const rootStart = spans[0]?.startTime

  return (
    <div className="space-y-1">
      {spans.map((span, i) => {
        const spanStart = new Date(span.startTime).getTime()
        const rootStartTime = new Date(rootStart).getTime()
        const offsetMs = spanStart - rootStartTime
        const offsetPct = totalDurationMs > 0 ? (offsetMs / totalDurationMs) * 100 : 0
        const widthPct =
          totalDurationMs > 0 ? Math.max((span.durationMs / totalDurationMs) * 100, 1) : 1

        return (
          <div key={i} className="flex items-center gap-3 group">
            <div className="w-56 shrink-0 flex items-center gap-1.5">
              <span className="text-xs text-gray-400 font-mono w-6 text-right">
                {i + 1}
              </span>
              {span.statusCode === 'ERROR' ? (
                <XCircle className="h-3.5 w-3.5 text-red-500 shrink-0" />
              ) : (
                <CheckCircle className="h-3.5 w-3.5 text-green-500 shrink-0" />
              )}
              <span className="text-xs font-medium text-gray-700 truncate">
                {span.serviceName}
              </span>
            </div>

            <div className="flex-1 relative h-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full h-px bg-gray-100" />
              </div>
              <div
                className="absolute top-1 h-4 rounded"
                style={{
                  left: `${offsetPct}%`,
                  width: `${widthPct}%`,
                  minWidth: '4px',
                  backgroundColor:
                    span.statusCode === 'ERROR'
                      ? '#fca5a5'
                      : span.durationMs > 500
                        ? '#fcd34d'
                        : '#86efac',
                }}
              >
                <span className="absolute inset-0 flex items-center justify-center text-[10px] font-medium text-gray-700 px-1 truncate">
                  {span.operationName}
                </span>
              </div>
            </div>

            <span className="w-20 text-right text-xs text-gray-500 shrink-0">
              {span.durationMs < 1000 ? `${span.durationMs}ms` : `${(span.durationMs / 1000).toFixed(1)}s`}
            </span>
          </div>
        )
      })}
    </div>
  )
}

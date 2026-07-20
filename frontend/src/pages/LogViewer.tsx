import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { ChevronDown, ChevronRight, XCircle, AlertTriangle, Info } from 'lucide-react'
import { useLogSummary, usePagedLogs } from '../hooks/useLogs'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { formatDateTime } from '../lib/utils'
import type { LogEntryDto } from '../types'
import { cn } from '../lib/utils'

const LOG_LEVELS: (string)[] = ['ALL', 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL']
const LOG_CATEGORIES: (string)[] = ['ALL', 'SIMULATION', 'PERFORMANCE', 'METRICS', 'AI_ANALYSIS', 'RECOMMENDATION', 'SYSTEM', 'AUDIT']

const levelColour: Record<string, string> = {
  TRACE: 'bg-gray-100 text-gray-600',
  DEBUG: 'bg-blue-100 text-blue-600',
  INFO: 'bg-green-100 text-green-700',
  WARN: 'bg-amber-100 text-amber-700',
  ERROR: 'bg-red-100 text-red-700',
  FATAL: 'bg-red-200 text-red-800',
}

export default function LogViewer() {
  const { id } = useParams<{ id: string }>()
  const simulationId = id ? Number(id) : null

  const [page, setPage] = useState(0)
  const [level, setLevel] = useState('ALL')
  const [category, setCategory] = useState('ALL')
  const [keyword, setKeyword] = useState('')
  const [debouncedKeyword, setDebouncedKeyword] = useState('')
  const [expandedId, setExpandedId] = useState<number | null>(null)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedKeyword(keyword), 400)
    return () => clearTimeout(t)
  }, [keyword])

  const hasFilters = level !== 'ALL' || category !== 'ALL' || debouncedKeyword !== ''

  const { data: summaryData, isLoading: summaryLoading } = useLogSummary(simulationId)
  const { data: logsData, isLoading: logsLoading, error: logsError, refetch } = usePagedLogs(
    simulationId,
    page,
    50,
  )

  const summary = summaryData?.data
  const logs = logsData?.data

  if (summaryLoading || logsLoading) return <LoadingSpinner label="Loading logs..." />
  if (logsError) return <ErrorCard message={logsError.message} onRetry={() => refetch()} />

  const clearFilters = () => {
    setLevel('ALL')
    setCategory('ALL')
    setKeyword('')
    setPage(0)
  }

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-gray-900">Log Viewer</h2>

      {/* Summary Tiles */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Total Logs</p>
            <p className="text-2xl font-bold text-gray-900">{summary.totalCount}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Errors</p>
            <p className={cn('text-2xl font-bold', summary.errorCount > 0 ? 'text-red-600' : 'text-gray-900')}>
              {summary.errorCount}
            </p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Warnings</p>
            <p className={cn('text-2xl font-bold', summary.warnCount > 0 ? 'text-amber-600' : 'text-gray-900')}>
              {summary.warnCount}
            </p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Threshold Breached</p>
            {summary.thresholdBreached ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700">
                <XCircle className="h-3 w-3" /> Yes
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700">
                <Info className="h-3 w-3" /> No
              </span>
            )}
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 flex flex-wrap items-end gap-3">
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Level</label>
          <select
            value={level}
            onChange={(e) => { setLevel(e.target.value); setPage(0) }}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          >
            {LOG_LEVELS.map((l) => (
              <option key={l} value={l}>{l}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Category</label>
          <select
            value={category}
            onChange={(e) => { setCategory(e.target.value); setPage(0) }}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          >
            {LOG_CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="flex-1 min-w-[200px]">
          <label className="block text-xs font-medium text-gray-500 mb-1">Keyword</label>
          <input
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            placeholder="Search logs..."
            className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
          />
        </div>
        {hasFilters && (
          <button
            onClick={clearFilters}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
          >
            Clear filters
          </button>
        )}
      </div>

      {/* Log Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {logs && logs.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500 w-8"></th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Time</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Level</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Category</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Source</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">Message</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {logs.content.map((log: LogEntryDto) => (
                    <>
                      <tr
                        key={log.id}
                        onClick={() => setExpandedId(expandedId === log.id ? null : log.id)}
                        className="hover:bg-gray-50 cursor-pointer"
                      >
                        <td className="px-4 py-3">
                          {expandedId === log.id ? (
                            <ChevronDown className="h-4 w-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="h-4 w-4 text-gray-400" />
                          )}
                        </td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{formatDateTime(log.occurredAt)}</td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${levelColour[log.level] || 'bg-gray-100 text-gray-600'}`}>
                            {log.level}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{log.category}</td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{log.source}</td>
                        <td className="px-4 py-3 text-gray-900 max-w-md truncate">{log.message}</td>
                      </tr>
                      {expandedId === log.id && (
                        <tr key={`${log.id}-expanded`}>
                          <td colSpan={6} className="px-6 py-4 bg-gray-50 border-t border-gray-100">
                            <div className="space-y-3">
                              <div>
                                <p className="text-xs font-medium text-gray-500 mb-1">Message</p>
                                <p className="text-sm text-gray-900">{log.message}</p>
                              </div>
                              {log.context && (
                                <div>
                                  <p className="text-xs font-medium text-gray-500 mb-1">Context</p>
                                  <pre className="text-xs bg-gray-100 rounded-lg p-3 overflow-x-auto text-gray-700">
                                    {(() => {
                                      try {
                                        return JSON.stringify(JSON.parse(log.context), null, 2)
                                      } catch {
                                        return log.context
                                      }
                                    })()}
                                  </pre>
                                </div>
                              )}
                              {log.stackTrace && (
                                <div>
                                  <p className="text-xs font-medium text-gray-500 mb-1">Stack Trace</p>
                                  <pre className="text-xs bg-red-50 rounded-lg p-3 overflow-x-auto text-red-700 max-h-48 overflow-y-auto">
                                    {log.stackTrace}
                                  </pre>
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3">
              <span className="text-xs text-gray-500">
                Page {logs.page + 1} of {logs.totalPages} ({logs.totalElements} total)
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={logs.first}
                  className="rounded-lg border border-gray-300 px-2 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                >
                  Prev
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={logs.last}
                  className="rounded-lg border border-gray-300 px-2 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-12">
            <AlertTriangle className="h-10 w-10 text-gray-300 mb-2" />
            <p className="text-gray-500">No logs found</p>
          </div>
        )}
      </div>
    </div>
  )
}

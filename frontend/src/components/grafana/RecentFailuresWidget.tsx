import { XCircle } from 'lucide-react'
import type { GrafanaRecentFailuresWidget } from '../../types'
import { timeAgo } from '../../lib/utils'

type Props = {
  data: GrafanaRecentFailuresWidget | null
}

export function RecentFailuresWidget({ data }: Props) {
  if (!data || data.failures.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Recent Failures</h3>
        <div className="flex items-center justify-center py-8">
          <p className="text-sm text-green-600 font-medium">No recent failures</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <XCircle className="h-4 w-4 text-red-600" />
          <h3 className="text-sm font-semibold text-gray-900">Recent Failures</h3>
        </div>
        <span className="inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
          {data.totalFailures}
        </span>
      </div>

      <div className="space-y-2 max-h-64 overflow-y-auto">
        {data.failures.map((f, i) => (
          <div
            key={`${f.serviceId}-${i}`}
            className="rounded-lg border border-red-100 bg-red-50 p-3"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-sm font-medium text-red-900">{f.serviceName}</span>
              <span className="text-xs text-red-500">{timeAgo(f.occurredAt)}</span>
            </div>
            <p className="text-xs text-red-700 line-clamp-2">{f.errorMessage}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

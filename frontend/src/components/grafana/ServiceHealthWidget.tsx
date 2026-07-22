import { CheckCircle, AlertCircle, XCircle } from 'lucide-react'
import type { GrafanaServiceHealthWidget } from '../../types'

type Props = {
  data: GrafanaServiceHealthWidget | null
}

export function ServiceHealthWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Service Health</h3>
        <p className="text-sm text-gray-400">No data available</p>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <h3 className="text-sm font-semibold text-gray-900 mb-4">Service Health</h3>

      <div className="grid grid-cols-3 gap-3 mb-4">
        <div className="rounded-lg border border-gray-200 p-3 bg-green-50">
          <div className="flex items-center gap-1.5 mb-1">
            <CheckCircle className="h-3.5 w-3.5 text-green-600" />
            <span className="text-xs font-medium text-green-700">Online</span>
          </div>
          <p className="text-xl font-bold text-green-800">{data.onlineCount}</p>
        </div>
        <div className="rounded-lg border border-gray-200 p-3 bg-amber-50">
          <div className="flex items-center gap-1.5 mb-1">
            <AlertCircle className="h-3.5 w-3.5 text-amber-600" />
            <span className="text-xs font-medium text-amber-700">Degraded</span>
          </div>
          <p className="text-xl font-bold text-amber-800">{data.degradedCount}</p>
        </div>
        <div className="rounded-lg border border-gray-200 p-3 bg-red-50">
          <div className="flex items-center gap-1.5 mb-1">
            <XCircle className="h-3.5 w-3.5 text-red-600" />
            <span className="text-xs font-medium text-red-700">Offline</span>
          </div>
          <p className="text-xl font-bold text-red-800">{data.offlineCount}</p>
        </div>
      </div>

      <div className="space-y-2 max-h-48 overflow-y-auto">
        {data.services.map((s) => (
          <div
            key={s.serviceId}
            className="flex items-center justify-between text-sm px-3 py-2 rounded-lg bg-gray-50"
          >
            <span className="font-medium text-gray-900 truncate">{s.serviceName}</span>
            <div className="flex items-center gap-2">
              {s.latencyMs != null && (
                <span className="text-xs text-gray-500">{s.latencyMs}ms</span>
              )}
              <span
                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                  s.status === 'ONLINE'
                    ? 'bg-green-100 text-green-700'
                    : s.status === 'DEGRADED'
                      ? 'bg-amber-100 text-amber-700'
                      : s.status === 'OFFLINE'
                        ? 'bg-red-100 text-red-700'
                        : 'bg-gray-100 text-gray-500'
                }`}
              >
                {s.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

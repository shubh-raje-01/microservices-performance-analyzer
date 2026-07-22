import { TrendingDown } from 'lucide-react'
import type { GrafanaTopSlowestWidget } from '../../types'
import { formatLatency } from '../../lib/utils'

type Props = {
  data: GrafanaTopSlowestWidget | null
}

export function TopSlowestWidget({ data }: Props) {
  if (!data || data.services.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Top Slowest Services</h3>
        <p className="text-sm text-gray-400">No data available</p>
      </div>
    )
  }

  const maxLatency = Math.max(...data.services.map((s) => s.averageLatencyMs))

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center gap-2 mb-4">
        <TrendingDown className="h-4 w-4 text-orange-600" />
        <h3 className="text-sm font-semibold text-gray-900">Top Slowest Services</h3>
      </div>

      <div className="space-y-3">
        {data.services.map((service) => (
          <div key={service.serviceId}>
            <div className="flex items-center justify-between text-sm mb-1">
              <span className="text-gray-900 font-medium truncate">{service.serviceName}</span>
              <span className="text-gray-600 font-mono text-xs">
                {formatLatency(service.averageLatencyMs)}
              </span>
            </div>
            <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-500"
                style={{
                  width: `${maxLatency > 0 ? (service.averageLatencyMs / maxLatency) * 100 : 0}%`,
                  backgroundColor:
                    service.averageLatencyMs > 5000 ? '#ef4444' :
                    service.averageLatencyMs > 1000 ? '#f59e0b' : '#22c55e',
                }}
              />
            </div>
            <p className="text-xs text-gray-400 mt-0.5">
              {service.checkCount} health check{service.checkCount !== 1 ? 's' : ''}
            </p>
          </div>
        ))}
      </div>
    </div>
  )
}

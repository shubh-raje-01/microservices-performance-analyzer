import { useState, useCallback } from 'react'
import { RefreshCw, Wifi, WifiOff, Clock } from 'lucide-react'
import { useGrafanaWidgets } from '../hooks/useGrafanaWidgets'
import { useGrafanaWebSocket } from '../hooks/useGrafanaWebSocket'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import {
  ServiceHealthWidget,
  CpuWidget,
  MemoryWidget,
  LatencyWidget,
  RequestRateWidget,
  ErrorRateWidget,
  TopSlowestWidget,
  RecentFailuresWidget,
  DependencyGraphWidget,
} from '../components/grafana'
import type { GrafanaWidgetData } from '../types'

/**
 * Grafana-compatible monitoring dashboard.
 *
 * Architecture decisions:
 * - Dual transport: REST polling as primary (30s interval), WebSocket for
 *   push updates. If WebSocket connects, it overrides the polling data
 *   with fresher payloads. This provides resilience: if WebSocket fails,
 *   polling keeps the dashboard alive.
 * - The widget grid uses CSS Grid with responsive columns so each widget
 *   is self-contained and can be rearranged or removed independently.
 * - Connection status indicator shows the WebSocket state so operators
 *   know whether data is real-time or poll-based.
 * - The 'generatedAt' timestamp from the backend is displayed so users
 *   can verify data freshness at a glance.
 */
export default function GrafanaDashboard() {
  const [hours, setHours] = useState(24)
  const { data: pollData, isLoading, error } = useGrafanaWidgets(hours)
  const [wsData, setWsData] = useState<GrafanaWidgetData | null>(null)

  const handleWsMessage = useCallback((data: GrafanaWidgetData) => {
    setWsData(data)
  }, [])

  const { isConnected, reconnect } = useGrafanaWebSocket({
    enabled: true,
    onMessage: handleWsMessage,
  })

  // WebSocket data takes priority when available
  const data = wsData || pollData?.data || null

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">Grafana Monitoring</h2>
          <p className="text-sm text-gray-500">Real-time service observability dashboard</p>
        </div>

        <div className="flex items-center gap-3">
          {/* Connection status */}
          <div className="flex items-center gap-1.5">
            {isConnected ? (
              <>
                <Wifi className="h-3.5 w-3.5 text-green-600" />
                <span className="text-xs text-green-700 font-medium">Live</span>
              </>
            ) : (
              <>
                <WifiOff className="h-3.5 w-3.5 text-gray-400" />
                <span className="text-xs text-gray-500">Polling</span>
              </>
            )}
          </div>

          {/* Time range selector */}
          <select
            value={hours}
            onChange={(e) => setHours(Number(e.target.value))}
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value={1}>Last 1 hour</option>
            <option value={6}>Last 6 hours</option>
            <option value={24}>Last 24 hours</option>
            <option value={72}>Last 3 days</option>
            <option value={168}>Last 7 days</option>
          </select>

          {/* Refresh - only reconnects WebSocket, doesn't clear data */}
          <button
            onClick={() => {
              if (!isConnected) reconnect()
            }}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </button>
        </div>
      </div>

      {/* Data freshness indicator */}
      {data?.generatedAt && (
        <div className="flex items-center gap-1.5 text-xs text-gray-400">
          <Clock className="h-3 w-3" />
          Last updated: {new Date(data.generatedAt).toLocaleTimeString()}
        </div>
      )}

      {/* Loading / Error states */}
      {isLoading && !data && <LoadingSpinner label="Loading dashboard..." />}
      {error && !data && <ErrorCard message={error.message} />}

      {/* Widget Grid */}
      {data && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <ServiceHealthWidget data={data.serviceHealth} />
          <CpuWidget data={data.cpu} />
          <MemoryWidget data={data.memory} />
          <LatencyWidget data={data.latency} />
          <RequestRateWidget data={data.requestRate} />
          <ErrorRateWidget data={data.errorRate} />
          <TopSlowestWidget data={data.topSlowest} />
          <RecentFailuresWidget data={data.recentFailures} />
          <div className="md:col-span-2 lg:col-span-3">
            <DependencyGraphWidget data={data.dependencyGraph} />
          </div>
        </div>
      )}

      {/* Prometheus info */}
      <div className="bg-gray-50 rounded-xl border border-gray-200 p-4">
        <p className="text-xs text-gray-500">
          <strong>Prometheus Datasource:</strong> Point your Grafana Prometheus datasource to{' '}
          <code className="bg-gray-100 px-1.5 py-0.5 rounded text-gray-700 font-mono">
            http://localhost:8080/api/v1/grafana/prometheus
          </code>{' '}
          for pre-built metrics. WebSocket endpoint:{' '}
          <code className="bg-gray-100 px-1.5 py-0.5 rounded text-gray-700 font-mono">
            ws://localhost:8080/ws/grafana
          </code>
        </p>
      </div>
    </div>
  )
}

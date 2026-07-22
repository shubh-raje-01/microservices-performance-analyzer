import { Timer } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { GrafanaLatencyWidget } from '../../types'
import { formatLatency } from '../../lib/utils'

type Props = {
  data: GrafanaLatencyWidget | null
}

export function LatencyWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Latency</h3>
        <p className="text-sm text-gray-400">No data available</p>
      </div>
    )
  }

  const chartData = data.trend.map((p) => ({
    time: new Date(p.time).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    value: Math.round(p.value),
  }))

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Timer className="h-4 w-4 text-emerald-600" />
          <h3 className="text-sm font-semibold text-gray-900">Latency</h3>
        </div>
      </div>

      <div className="grid grid-cols-5 gap-2 mb-4">
        {[
          { label: 'Avg', value: data.averageMs },
          { label: 'P50', value: data.p50Ms },
          { label: 'P95', value: data.p95Ms },
          { label: 'P99', value: data.p99Ms },
          { label: 'Max', value: data.maxMs },
        ].map((item) => (
          <div key={item.label} className="text-center">
            <p className="text-xs text-gray-500">{item.label}</p>
            <p className="text-sm font-bold text-gray-900">{formatLatency(item.value)}</p>
          </div>
        ))}
      </div>

      <div className="h-36">
        {chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis tick={{ fontSize: 10 }} width={40} />
              <Tooltip
                formatter={(value: number) => [formatLatency(value), 'Latency']}
                labelFormatter={(label) => `Time: ${label}`}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke="#10b981"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-sm text-gray-400 text-center pt-8">No trend data</p>
        )}
      </div>
    </div>
  )
}

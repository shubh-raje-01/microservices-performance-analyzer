import { Activity } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { GrafanaRequestRateWidget } from '../../types'
import { formatCount } from '../../lib/utils'

type Props = {
  data: GrafanaRequestRateWidget | null
}

export function RequestRateWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Request Rate</h3>
        <p className="text-sm text-gray-400">No data available</p>
      </div>
    )
  }

  const chartData = data.trend.map((p) => ({
    time: new Date(p.time).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    value: Math.round(p.value * 10) / 10,
  }))

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Activity className="h-4 w-4 text-cyan-600" />
          <h3 className="text-sm font-semibold text-gray-900">Request Rate</h3>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold text-gray-900">{data.totalRps}</p>
          <p className="text-xs text-gray-500">req/s</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4">
        <div className="bg-gray-50 rounded-lg p-2.5">
          <p className="text-xs text-gray-500">Total Requests</p>
          <p className="text-sm font-bold text-gray-900">{formatCount(data.totalRequests)}</p>
        </div>
        <div className="bg-gray-50 rounded-lg p-2.5">
          <p className="text-xs text-gray-500">Successful</p>
          <p className="text-sm font-bold text-green-700">{formatCount(data.successfulRequests)}</p>
        </div>
      </div>

      <div className="h-36">
        {chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="reqGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#06b6d4" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#06b6d4" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis tick={{ fontSize: 10 }} width={35} />
              <Tooltip
                formatter={(value) => [`${Number(value ?? 0)} req/s`, 'Rate']}
                labelFormatter={(label) => `Time: ${label}`}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#06b6d4"
                fill="url(#reqGradient)"
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-sm text-gray-400 text-center pt-8">No trend data</p>
        )}
      </div>
    </div>
  )
}

import { Cpu } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { GrafanaCpuWidget } from '../../types'

type Props = {
  data: GrafanaCpuWidget | null
}

export function CpuWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">CPU Usage</h3>
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
          <Cpu className="h-4 w-4 text-blue-600" />
          <h3 className="text-sm font-semibold text-gray-900">CPU Usage</h3>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold text-gray-900">{data.averageUsagePercent}%</p>
          <p className="text-xs text-gray-500">avg</p>
        </div>
      </div>

      <div className="h-40">
        {chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="cpuGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#3b82f6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} width={35} />
              <Tooltip
                formatter={(value) => [`${Number(value ?? 0)}%`, 'CPU']}
                labelFormatter={(label) => `Time: ${label}`}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#3b82f6"
                fill="url(#cpuGradient)"
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-sm text-gray-400 text-center pt-8">No trend data</p>
        )}
      </div>

      {data.perService.length > 0 && (
        <div className="mt-3 space-y-1.5 max-h-32 overflow-y-auto">
          {data.perService.map((s) => (
            <div key={s.serviceId} className="flex items-center justify-between text-xs">
              <span className="text-gray-600 truncate">{s.serviceName}</span>
              <div className="flex items-center gap-2">
                <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-500 rounded-full"
                    style={{ width: `${Math.min(s.usagePercent, 100)}%` }}
                  />
                </div>
                <span className="font-medium text-gray-900 w-12 text-right">
                  {s.usagePercent}%
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

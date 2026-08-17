import { HardDrive } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { GrafanaMemoryWidget } from '../../types'

type Props = {
  data: GrafanaMemoryWidget | null
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

export function MemoryWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Memory Usage</h3>
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
          <HardDrive className="h-4 w-4 text-purple-600" />
          <h3 className="text-sm font-semibold text-gray-900">Memory Usage</h3>
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
                <linearGradient id="memGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#a855f7" stopOpacity={0.3} />
                  <stop offset="100%" stopColor="#a855f7" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} width={35} />
              <Tooltip
                formatter={(value) => [`${Number(value ?? 0)}%`, 'Memory']}
                labelFormatter={(label) => `Time: ${label}`}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#a855f7"
                fill="url(#memGradient)"
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
                <span className="text-gray-500">{formatBytes(s.usedBytes)}</span>
                <div className="w-16 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-purple-500 rounded-full"
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

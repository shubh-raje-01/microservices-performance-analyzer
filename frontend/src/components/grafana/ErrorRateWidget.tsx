import { AlertTriangle } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import type { GrafanaErrorRateWidget } from '../../types'

type Props = {
  data: GrafanaErrorRateWidget | null
}

export function ErrorRateWidget({ data }: Props) {
  if (!data) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Error Rate</h3>
        <p className="text-sm text-gray-400">No data available</p>
      </div>
    )
  }

  // Sample trend points for the bar chart (max 30 points)
  const trend = data.trend.length > 30
    ? data.trend.filter((_, i) => i % Math.ceil(data.trend.length / 30) === 0)
    : data.trend

  const chartData = trend.map((p) => ({
    time: new Date(p.time).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    errors: p.value,
  }))

  const errorColor = data.errorRatePercent > 10 ? '#ef4444' :
    data.errorRatePercent > 5 ? '#f59e0b' : '#22c55e'

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-red-600" />
          <h3 className="text-sm font-semibold text-gray-900">Error Rate</h3>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold" style={{ color: errorColor }}>
            {data.errorRatePercent}%
          </p>
          <p className="text-xs text-gray-500">error rate</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-4">
        <div className="bg-red-50 rounded-lg p-2.5">
          <p className="text-xs text-red-600">Errors</p>
          <p className="text-sm font-bold text-red-800">{data.totalErrors}</p>
        </div>
        <div className="bg-gray-50 rounded-lg p-2.5">
          <p className="text-xs text-gray-500">Total Checks</p>
          <p className="text-sm font-bold text-gray-900">{data.totalRequests}</p>
        </div>
      </div>

      <div className="h-36">
        {chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
              <YAxis tick={{ fontSize: 10 }} width={25} />
              <Tooltip
                formatter={(value) => [Number(value ?? 0) ? 'Error' : 'OK', 'Status']}
                labelFormatter={(label) => `Time: ${label}`}
              />
              <Bar dataKey="errors" radius={[2, 2, 0, 0]}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={index}
                    fill={entry.errors ? '#ef4444' : '#22c55e'}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-sm text-gray-400 text-center pt-8">No trend data</p>
        )}
      </div>
    </div>
  )
}

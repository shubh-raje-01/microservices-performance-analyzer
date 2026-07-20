import { useParams, useNavigate } from 'react-router-dom'
import { ArrowUp, ArrowRight, ArrowDown, Brain, Loader2 } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { useAIInsight, useAnalyze } from '../hooks/useAI'
import { useMetricsSummary } from '../hooks/useMetrics'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorCard } from '../components/ui/ErrorCard'
import { anomalyColour, formatLatency } from '../lib/utils'

export default function AIInsights() {
  const { id } = useParams<{ id: string }>()
  const simulationId = id ? Number(id) : null
  const navigate = useNavigate()

  const {
    data: insightData,
    isLoading: insightLoading,
    error: insightError,
  } = useAIInsight(simulationId)
  const {
    data: summaryData,
    isLoading: summaryLoading,
  } = useMetricsSummary(simulationId)
  const analyzeMutation = useAnalyze()

  const insight = insightData?.data
  const summary = summaryData?.data

  if (insightLoading || summaryLoading) return <LoadingSpinner label="Loading AI insights..." />
  if (insightError) return <ErrorCard message={insightError.message} />

  if (!insight) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <Brain className="h-12 w-12 text-gray-300 mb-3" />
        <p className="text-gray-500 mb-4">No AI analysis available for this simulation</p>
        <button
          onClick={() => {
            analyzeMutation.mutate(simulationId!, {
              onSuccess: () => {},
            })
          }}
          disabled={analyzeMutation.isPending}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {analyzeMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Running AI analysis... this may take up to 30s
            </>
          ) : (
            <>
              <Brain className="h-4 w-4" />
              Run AI Analysis
            </>
          )}
        </button>
      </div>
    )
  }

  const trendIcon = {
    IMPROVING: <ArrowUp className="h-5 w-5 text-green-600" />,
    STABLE: <ArrowRight className="h-5 w-5 text-gray-500" />,
    DEGRADING: <ArrowDown className="h-5 w-5 text-red-600" />,
  }

  const featureData = Object.entries(insight.featureImportance)
    .map(([key, value]) => ({
      name: key.replace(/_/g, ' '),
      value,
    }))
    .sort((a, b) => b.value - a.value)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">AI Insights</h2>
        <button
          onClick={() => navigate(-1)}
          className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
        >
          Back
        </button>
      </div>

      {/* Anomaly Card */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Anomaly Analysis</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <p className="text-xs text-gray-500 mb-2">Anomaly Type</p>
            <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-sm font-medium ${anomalyColour[insight.anomalyType] || 'bg-gray-100 text-gray-700'}`}>
              {insight.anomalyType}
            </span>
          </div>
          <div>
            <p className="text-xs text-gray-500 mb-2">Anomaly Score</p>
            <div className="flex items-center gap-3">
              <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  className="h-full bg-blue-600 rounded-full"
                  style={{ width: `${Math.min(100, insight.anomalyScore)}%` }}
                />
              </div>
              <span className="text-sm font-medium text-gray-700">{insight.anomalyScore.toFixed(0)}%</span>
            </div>
          </div>
          <div>
            <p className="text-xs text-gray-500 mb-2">Predicted Trend</p>
            <div className="flex items-center gap-2">
              {trendIcon[insight.predictedTrend]}
              <span className="text-sm font-medium text-gray-700">{insight.predictedTrend}</span>
            </div>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-4">
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500">Predicted p95</p>
            <p className="text-lg font-bold text-gray-900">{formatLatency(insight.predictedP95Ms)}</p>
          </div>
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500">Current p95</p>
            <p className="text-lg font-bold text-gray-900">{formatLatency(summary?.p95LatencyMs)}</p>
          </div>
        </div>
      </div>

      {/* Summary */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Analysis Summary</h3>
        <p className="text-sm text-gray-700 leading-relaxed mb-4">{insight.summary}</p>

        {insight.detectedPatterns.length > 0 && (
          <div>
            <p className="text-xs font-medium text-gray-500 mb-2">Detected Patterns</p>
            <ul className="space-y-1">
              {insight.detectedPatterns.map((pattern, i) => (
                <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                  <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-blue-600 shrink-0" />
                  {pattern}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Feature Importance */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-4">Feature Importance</h3>
        {featureData.length > 0 ? (
          <ResponsiveContainer width="100%" height={Math.max(200, featureData.length * 40)}>
            <BarChart data={featureData} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis type="number" domain={[0, 1]} tick={{ fontSize: 11, fill: '#6b7280' }} />
              <YAxis type="category" dataKey="name" width={180} tick={{ fontSize: 11, fill: '#6b7280' }} />
              <Tooltip
                contentStyle={{ backgroundColor: 'white', border: '1px solid #e5e7eb', borderRadius: '0.5rem', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}
              />
              <Bar dataKey="value" fill="#7c3aed" />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-sm text-gray-400">No feature importance data available</p>
        )}
      </div>

      {/* Recommendations */}
      {insight.recommendations.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">AI Recommendations</h3>
          <ul className="space-y-2">
            {insight.recommendations.map((rec, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-green-600 shrink-0" />
                {rec}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

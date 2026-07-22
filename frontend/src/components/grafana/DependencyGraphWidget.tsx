import { Network } from 'lucide-react'
import type { GrafanaDependencyGraphWidget } from '../../types'

type Props = {
  data: GrafanaDependencyGraphWidget | null
}

const NODE_W = 150
const NODE_H = 60
const GAP = 70
const PAD = 30

function latencyColor(ms: number): string {
  if (ms < 100) return '#22c55e'
  if (ms < 500) return '#f59e0b'
  return '#ef4444'
}

function latencyBg(ms: number): string {
  if (ms < 100) return '#f0fdf4'
  if (ms < 500) return '#fffbeb'
  return '#fef2f2'
}

export function DependencyGraphWidget({ data }: Props) {
  if (!data || data.nodes.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Service Dependency Graph</h3>
        <p className="text-sm text-gray-400">No dependency data available</p>
      </div>
    )
  }

  const positions = new Map<string, { x: number; y: number }>()
  data.nodes.forEach((node, i) => {
    positions.set(node.serviceName, {
      x: PAD + i * (NODE_W + GAP),
      y: PAD + 30,
    })
  })

  const svgW = PAD * 2 + data.nodes.length * (NODE_W + GAP) - GAP
  const svgH = NODE_H + PAD * 2 + 60

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
      <div className="flex items-center gap-2 mb-4">
        <Network className="h-4 w-4 text-indigo-600" />
        <h3 className="text-sm font-semibold text-gray-900">Service Dependency Graph</h3>
      </div>

      <div className="overflow-x-auto">
        <svg width={svgW} height={svgH} viewBox={`0 0 ${svgW} ${svgH}`} className="min-w-full">
          <defs>
            <marker id="gArrow" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <polygon points="0 0, 8 3, 0 6" fill="#94a3b8" />
            </marker>
          </defs>

          {data.edges.map((edge, i) => {
            const from = positions.get(edge.source)
            const to = positions.get(edge.target)
            if (!from || !to) return null

            const x1 = from.x + NODE_W
            const y1 = from.y + NODE_H / 2
            const x2 = to.x
            const y2 = to.y + NODE_H / 2
            const midX = (x1 + x2) / 2

            return (
              <g key={i}>
                <path
                  d={`M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`}
                  fill="none"
                  stroke="#cbd5e1"
                  strokeWidth="2"
                  markerEnd="url(#gArrow)"
                />
                <text
                  x={midX}
                  y={Math.min(y1, y2) - 6}
                  textAnchor="middle"
                  className="fill-gray-500"
                  fontSize="10"
                >
                  {edge.callCount} call{edge.callCount !== 1 ? 's' : ''} ·{' '}
                  {edge.avgDurationMs.toFixed(0)}ms
                </text>
              </g>
            )
          })}

          {data.nodes.map((node) => {
            const pos = positions.get(node.serviceName)
            if (!pos) return null

            return (
              <g key={node.serviceName}>
                <rect
                  x={pos.x}
                  y={pos.y}
                  width={NODE_W}
                  height={NODE_H}
                  rx="10"
                  fill={latencyBg(node.avgLatencyMs)}
                  stroke={node.status === 'ERROR' ? '#ef4444' : '#e2e8f0'}
                  strokeWidth={node.status === 'ERROR' ? '2' : '1'}
                />
                <text
                  x={pos.x + NODE_W / 2}
                  y={pos.y + 22}
                  textAnchor="middle"
                  className="fill-gray-900"
                  fontSize="12"
                  fontWeight="600"
                >
                  {node.serviceName}
                </text>
                <text
                  x={pos.x + NODE_W / 2}
                  y={pos.y + 40}
                  textAnchor="middle"
                  fill={latencyColor(node.avgLatencyMs)}
                  fontSize="10"
                  fontWeight="500"
                >
                  {node.avgLatencyMs.toFixed(0)}ms · {node.spanCount} span
                  {node.spanCount !== 1 ? 's' : ''}
                </text>
              </g>
            )
          })}
        </svg>
      </div>
    </div>
  )
}

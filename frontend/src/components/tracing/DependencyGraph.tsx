import type { TraceDependencyGraphDto } from '../../types'

const NODE_WIDTH = 160
const NODE_HEIGHT = 64
const NODE_GAP = 80
const PADDING = 40

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

export default function DependencyGraph({ data }: { data: TraceDependencyGraphDto }) {
  if (!data.nodes.length) {
    return (
      <div className="flex items-center justify-center py-12 text-gray-400 text-sm">
        No dependency data for this trace
      </div>
    )
  }

  const nodePositions = new Map<string, { x: number; y: number }>()
  data.nodes.forEach((node, i) => {
    nodePositions.set(node.serviceName, {
      x: PADDING + i * (NODE_WIDTH + NODE_GAP),
      y: PADDING + 40,
    })
  })

  const svgWidth =
    PADDING * 2 + data.nodes.length * (NODE_WIDTH + NODE_GAP) - NODE_GAP
  const svgHeight = NODE_HEIGHT + PADDING * 2 + 80

  return (
    <div className="overflow-x-auto">
      <svg
        width={svgWidth}
        height={svgHeight}
        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        className="min-w-full"
      >
        <defs>
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="7"
            refX="10"
            refY="3.5"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#94a3b8" />
          </marker>
        </defs>

        {data.edges.map((edge, i) => {
          const from = nodePositions.get(edge.source)
          const to = nodePositions.get(edge.target)
          if (!from || !to) return null

          const x1 = from.x + NODE_WIDTH
          const y1 = from.y + NODE_HEIGHT / 2
          const x2 = to.x
          const y2 = to.y + NODE_HEIGHT / 2
          const midX = (x1 + x2) / 2

          return (
            <g key={i}>
              <path
                d={`M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`}
                fill="none"
                stroke="#cbd5e1"
                strokeWidth="2"
                markerEnd="url(#arrowhead)"
              />
              <text
                x={midX}
                y={Math.min(y1, y2) - 8}
                textAnchor="middle"
                className="fill-gray-500"
                fontSize="11"
              >
                {edge.callCount} call{edge.callCount !== 1 ? 's' : ''} ·{' '}
                {edge.avgDurationMs.toFixed(0)}ms
              </text>
            </g>
          )
        })}

        {data.nodes.map((node) => {
          const pos = nodePositions.get(node.serviceName)
          if (!pos) return null

          return (
            <g key={node.serviceName}>
              <rect
                x={pos.x}
                y={pos.y}
                width={NODE_WIDTH}
                height={NODE_HEIGHT}
                rx="12"
                fill={latencyBg(node.avgDurationMs)}
                stroke={node.status === 'ERROR' ? '#ef4444' : '#e2e8f0'}
                strokeWidth={node.status === 'ERROR' ? '2' : '1'}
              />
              <text
                x={pos.x + NODE_WIDTH / 2}
                y={pos.y + 24}
                textAnchor="middle"
                className="fill-gray-900"
                fontSize="13"
                fontWeight="600"
              >
                {node.serviceName}
              </text>
              <text
                x={pos.x + NODE_WIDTH / 2}
                y={pos.y + 44}
                textAnchor="middle"
                fill={latencyColor(node.avgDurationMs)}
                fontSize="11"
                fontWeight="500"
              >
                {node.avgDurationMs.toFixed(0)}ms avg · {node.operationCount} span
                {node.operationCount !== 1 ? 's' : ''}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

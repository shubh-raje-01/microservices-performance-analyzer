import { cn } from '../../lib/utils'

type Props = {
  status: string | null
  score?: number | null
}

export function HealthBadge({ status, score }: Props) {
  if (!status) {
    return (
      <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-500">
        Unknown
      </span>
    )
  }

  const colorMap: Record<string, string> = {
    HEALTHY: 'bg-green-100 text-green-700',
    DEGRADED: 'bg-amber-100 text-amber-700',
    CRITICAL: 'bg-red-100 text-red-700',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        colorMap[status] || 'bg-gray-100 text-gray-500',
      )}
    >
      {status}
      {score != null && ` · ${score.toFixed(1)}`}
    </span>
  )
}

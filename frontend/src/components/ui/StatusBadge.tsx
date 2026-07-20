import { cn } from '../../lib/utils'
import type { SimulationStatus } from '../../types'

type Props = {
  status: SimulationStatus
}

export function StatusBadge({ status }: Props) {
  const colorMap: Record<SimulationStatus, string> = {
    PENDING: 'bg-gray-100 text-gray-700',
    RUNNING: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    FAILED: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-amber-100 text-amber-700',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
        colorMap[status],
      )}
    >
      {status === 'RUNNING' && (
        <span className="h-1.5 w-1.5 rounded-full bg-blue-600 animate-pulse" />
      )}
      {status}
    </span>
  )
}

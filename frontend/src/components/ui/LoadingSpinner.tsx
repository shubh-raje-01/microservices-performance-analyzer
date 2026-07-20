import { cn } from '../../lib/utils'

type Props = {
  size?: 'sm' | 'md' | 'lg'
  label?: string
}

const sizeMap = {
  sm: 'h-4 w-4',
  md: 'h-6 w-6',
  lg: 'h-8 w-8',
}

export function LoadingSpinner({ size = 'md', label }: Props) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-8">
      <div
        className={cn(
          'animate-spin rounded-full border-2 border-gray-200 border-t-blue-600',
          sizeMap[size],
        )}
      />
      {label && <p className="text-sm text-gray-500">{label}</p>}
    </div>
  )
}

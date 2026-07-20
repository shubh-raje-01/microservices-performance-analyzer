import { AlertTriangle } from 'lucide-react'

type Props = {
  message: string
  onRetry?: () => void
}

export function ErrorCard({ message, onRetry }: Props) {
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-6">
      <div className="flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 text-red-600 mt-0.5 shrink-0" />
        <div className="flex-1">
          <h3 className="text-sm font-medium text-red-800">Something went wrong</h3>
          <p className="mt-1 text-sm text-red-700">{message}</p>
          {onRetry && (
            <button
              onClick={onRetry}
              className="mt-3 inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700"
            >
              Retry
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

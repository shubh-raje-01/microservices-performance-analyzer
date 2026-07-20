import { Menu, Plus, AlertCircle } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAnalyzerStore } from '../../store'
import { useSystemHealth } from '../../hooks/useDashboard'

const routeTitles: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/simulate': 'Run Simulation',
  '/history': 'Simulation History',
}

function getTitle(pathname: string): string {
  if (pathname.startsWith('/metrics')) return 'Metrics View'
  if (pathname.startsWith('/logs')) return 'Log Viewer'
  if (pathname.startsWith('/insights')) return 'AI Insights'
  if (pathname.startsWith('/recommendations')) return 'Recommendations'
  return routeTitles[pathname] || 'Dashboard'
}

export function TopBar() {
  const toggleSidebar = useAnalyzerStore((s) => s.toggleSidebar)
  const { data: healthData } = useSystemHealth()
  const navigate = useNavigate()
  const location = useLocation()
  const title = getTitle(location.pathname)
  const health = healthData?.data

  const healthDot =
    health && health.criticalCount > 0
      ? 'bg-red-500'
      : health && health.degradedCount > 0
        ? 'bg-amber-500'
        : 'bg-green-500'

  return (
    <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-4">
      <div className="flex items-center gap-3">
        <button
          onClick={toggleSidebar}
          className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100"
        >
          <Menu className="h-5 w-5" />
        </button>
        <h1 className="text-base font-semibold text-gray-900">{title}</h1>
      </div>

      <div className="flex items-center gap-3">
        {health && health.criticalCount > 0 && (
          <div className="flex items-center gap-1.5 text-xs text-red-600">
            <AlertCircle className="h-3.5 w-3.5" />
            {health.criticalCount} critical
          </div>
        )}

        <div className="flex items-center gap-2">
          <span className={`h-2 w-2 rounded-full ${healthDot}`} />
          {health && (
            <span className="text-xs text-gray-500">
              {health.healthyCount}/{health.totalServicesAnalysed} healthy
            </span>
          )}
        </div>

        <button
          onClick={() => navigate('/simulate')}
          className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
        >
          <Plus className="h-3.5 w-3.5" />
          New Simulation
        </button>
      </div>
    </header>
  )
}

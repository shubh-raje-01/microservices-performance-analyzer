import { NavLink } from 'react-router-dom'
import { BarChart3, Rocket, History, Network } from 'lucide-react'
import { cn } from '../../lib/utils'
import { useAnalyzerStore } from '../../store'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: BarChart3 },
  { to: '/simulate', label: 'Run Simulation', icon: Rocket },
  { to: '/history', label: 'History', icon: History },
  { to: '/traces', label: 'Tracing', icon: Network },
]

export function Sidebar() {
  const sidebarOpen = useAnalyzerStore((s) => s.sidebarOpen)

  return (
    <aside
      className={cn(
        'flex flex-col border-r border-gray-200 bg-white transition-all duration-200',
        sidebarOpen ? 'w-60' : 'w-16',
      )}
    >
      <div className="flex h-14 items-center border-b border-gray-200 px-3">
        {sidebarOpen && (
          <span className="text-sm font-semibold text-gray-900 truncate">
            Perf Analyzer
          </span>
        )}
      </div>

      <nav className="flex-1 space-y-1 px-2 py-3">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-700 hover:bg-gray-50',
                !sidebarOpen && 'justify-center px-0',
              )
            }
          >
            <item.icon className="h-5 w-5 shrink-0" />
            {sidebarOpen && <span>{item.label}</span>}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-gray-200 px-3 py-2">
        {sidebarOpen && (
          <span className="text-xs text-gray-400">v1.0.0</span>
        )}
      </div>
    </aside>
  )
}

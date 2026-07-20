import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboard'
import { useAnalyzerStore } from '../store'

export function useDashboardSummary(simulationId: number | null) {
  const { setDashboardSummary } = useAnalyzerStore()

  return useQuery({
    queryKey: ['dashboardSummary', simulationId],
    queryFn: async () => {
      const res = await dashboardApi.getSummary(simulationId!)
      setDashboardSummary(res.data.data)
      return res.data
    },
    enabled: simulationId != null,
  })
}

export function useRecentDashboards(limit = 10) {
  return useQuery({
    queryKey: ['dashboardRecent', limit],
    queryFn: () => dashboardApi.getRecent(limit).then((r) => r.data),
    retry: (failureCount, error) => {
      if (error instanceof Error && error.message.includes('ECONNREFUSED')) return false
      return failureCount < 1
    },
  })
}

export function useSystemHealth() {
  const { setSystemHealth } = useAnalyzerStore()

  return useQuery({
    queryKey: ['systemHealth'],
    queryFn: async () => {
      const res = await dashboardApi.getSystemHealth()
      setSystemHealth(res.data.data)
      return res.data
    },
    refetchInterval: (_query) => {
      if (_query.state.error) return false
      return 30_000
    },
  })
}

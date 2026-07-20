import { useQuery } from '@tanstack/react-query'
import { logsApi } from '../api/logs'

export function useLogSummary(simulationId: number | null) {
  return useQuery({
    queryKey: ['logSummary', simulationId],
    queryFn: () => logsApi.getSummary(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function usePagedLogs(simulationId: number | null, page = 0, size = 50) {
  return useQuery({
    queryKey: ['logs', simulationId, page, size],
    queryFn: () => logsApi.getPaged(simulationId!, page, size).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useLogSearch(params: {
  simulationId: number
  level?: string
  category?: string
  keyword?: string
  from?: string
  to?: string
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: ['logSearch', params],
    queryFn: () => logsApi.search(params).then((r) => r.data),
    enabled: params.simulationId != null,
  })
}

export function useLogErrors(simulationId: number | null) {
  return useQuery({
    queryKey: ['logErrors', simulationId],
    queryFn: () => logsApi.getErrors(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

import { useQuery } from '@tanstack/react-query'
import { metricsApi } from '../api/metrics'

export function useMetricsSummary(simulationId: number | null) {
  return useQuery({
    queryKey: ['metricsSummary', simulationId],
    queryFn: () => metricsApi.getSummary(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useMetrics(simulationId: number | null) {
  return useQuery({
    queryKey: ['metrics', simulationId],
    queryFn: () => metricsApi.getAll(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useProblematicMetrics(simulationId: number | null) {
  return useQuery({
    queryKey: ['problematicMetrics', simulationId],
    queryFn: () => metricsApi.getProblematic(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useServiceNames() {
  return useQuery({
    queryKey: ['serviceNames'],
    queryFn: () => metricsApi.getServiceNames().then((r) => r.data),
  })
}

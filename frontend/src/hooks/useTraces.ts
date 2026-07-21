import { useQuery } from '@tanstack/react-query'
import { getTraces, getTraceById, getTraceGraph } from '../api/traces'

export function useTraces(params: {
  page?: number
  size?: number
  serviceName?: string
  hoursBack?: number
}) {
  return useQuery({
    queryKey: ['traces', params],
    queryFn: () => getTraces(params).then((r) => r.data),
    refetchInterval: 15_000,
  })
}

export function useTraceDetail(traceId: string | undefined) {
  return useQuery({
    queryKey: ['trace', traceId],
    queryFn: () => getTraceById(traceId!).then((r) => r.data),
    enabled: !!traceId,
  })
}

export function useTraceGraph(traceId: string | undefined) {
  return useQuery({
    queryKey: ['traceGraph', traceId],
    queryFn: () => getTraceGraph(traceId!).then((r) => r.data),
    enabled: !!traceId,
  })
}

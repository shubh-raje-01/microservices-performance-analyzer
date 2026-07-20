import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { aiApi } from '../api/ai'

export function useAIInsight(simulationId: number | null) {
  return useQuery({
    queryKey: ['aiInsight', simulationId],
    queryFn: () => aiApi.getInsights(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useAnalyze() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (simulationId: number) =>
      aiApi.analyze(simulationId).then((r) => r.data),
    onSuccess: (_response, simulationId) => {
      queryClient.invalidateQueries({ queryKey: ['aiInsight', simulationId] })
      toast.success('AI analysis complete')
    },
    onError: (error: Error) => {
      toast.error(error.message || 'AI analysis failed')
    },
  })
}

export function useRecentAnomalies(hoursBack = 24) {
  return useQuery({
    queryKey: ['recentAnomalies', hoursBack],
    queryFn: () => aiApi.getRecentAnomalies(hoursBack).then((r) => r.data),
  })
}

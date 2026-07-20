import { useQuery } from '@tanstack/react-query'
import { recommendationsApi } from '../api/recommendations'
import type { RecommendationCategory } from '../types'

export function useRecommendations(simulationId: number | null) {
  return useQuery({
    queryKey: ['recommendations', simulationId],
    queryFn: () => recommendationsApi.getAll(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useHighPriority(simulationId: number | null) {
  return useQuery({
    queryKey: ['highPriorityRecommendations', simulationId],
    queryFn: () => recommendationsApi.getHighPriority(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useRecommendationStats(simulationId: number | null) {
  return useQuery({
    queryKey: ['recommendationStats', simulationId],
    queryFn: () => recommendationsApi.getStats(simulationId!).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useTopN(simulationId: number | null, n = 5) {
  return useQuery({
    queryKey: ['topRecommendations', simulationId, n],
    queryFn: () => recommendationsApi.getTopN(simulationId!, n).then((r) => r.data),
    enabled: simulationId != null,
  })
}

export function useRecommendationsByCategory(
  simulationId: number | null,
  category: RecommendationCategory | null,
) {
  return useQuery({
    queryKey: ['recommendationsByCategory', simulationId, category],
    queryFn: () =>
      recommendationsApi.getByCategory(simulationId!, category!).then((r) => r.data),
    enabled: simulationId != null && category != null,
  })
}

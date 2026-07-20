import client from './client'
import type { ApiResponse, RecommendationDto, RecommendationStats, RecommendationCategory } from '../types'

export const recommendationsApi = {
  getAll: (simulationId: number) =>
    client.get<ApiResponse<RecommendationDto[]>>(`/recommendations/${simulationId}`),

  getHighPriority: (simulationId: number) =>
    client.get<ApiResponse<RecommendationDto[]>>(`/recommendations/${simulationId}/high-priority`),

  getByCategory: (simulationId: number, category: RecommendationCategory) =>
    client.get<ApiResponse<RecommendationDto[]>>(`/recommendations/${simulationId}/category/${category}`),

  getTopN: (simulationId: number, n = 5) =>
    client.get<ApiResponse<RecommendationDto[]>>(`/recommendations/${simulationId}/top`, {
      params: { n },
    }),

  getStats: (simulationId: number) =>
    client.get<ApiResponse<RecommendationStats>>(`/recommendations/${simulationId}/stats`),
}

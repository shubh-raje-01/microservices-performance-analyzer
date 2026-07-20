import client from './client'
import type { ApiResponse, AIInsightDto } from '../types'

export const aiApi = {
  analyze: (simulationId: number) =>
    client.post<ApiResponse<AIInsightDto>>(`/analyze/${simulationId}`),

  getInsights: (simulationId: number) =>
    client.get<ApiResponse<AIInsightDto>>(`/analyze/${simulationId}/insights`),

  getRecentAnomalies: (hoursBack = 24) =>
    client.get<ApiResponse<AIInsightDto[]>>('/analyze/anomalies/recent', {
      params: { hoursBack },
    }),
}

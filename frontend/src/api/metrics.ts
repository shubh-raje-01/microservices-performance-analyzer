import client from './client'
import type { ApiResponse, MetricSnapshotDto, MetricsSummaryDto, PagedResponse } from '../types'

export const metricsApi = {
  getAll: (simulationId: number) =>
    client.get<ApiResponse<MetricSnapshotDto[]>>(`/metrics/${simulationId}`),

  getSummary: (simulationId: number) =>
    client.get<ApiResponse<MetricsSummaryDto>>(`/metrics/${simulationId}/summary`),

  getPaged: (simulationId: number, page = 0, size = 20) =>
    client.get<ApiResponse<PagedResponse<MetricSnapshotDto>>>(`/metrics/${simulationId}/paged`, {
      params: { page, size },
    }),

  getAggregated: (simulationId: number) =>
    client.get<ApiResponse<Record<string, unknown>>>(`/metrics/${simulationId}/aggregated`),

  getProblematic: (simulationId: number) =>
    client.get<ApiResponse<MetricSnapshotDto[]>>(`/metrics/${simulationId}/problematic`),

  getServiceNames: () =>
    client.get<ApiResponse<string[]>>('/metrics/services'),
}

import client from './client'
import type { ApiResponse, LogEntryDto, LogSummaryDto, PagedResponse } from '../types'

export const logsApi = {
  getPaged: (simulationId: number, page = 0, size = 50) =>
    client.get<ApiResponse<PagedResponse<LogEntryDto>>>(`/logs/${simulationId}`, {
      params: { page, size },
    }),

  getSummary: (simulationId: number) =>
    client.get<ApiResponse<LogSummaryDto>>(`/logs/${simulationId}/summary`),

  getErrors: (simulationId: number) =>
    client.get<ApiResponse<LogEntryDto[]>>(`/logs/${simulationId}/errors`),

  getProblematic: (simulationId: number) =>
    client.get<ApiResponse<LogEntryDto[]>>(`/logs/${simulationId}/problematic`),

  search: (params: {
    simulationId: number
    level?: string
    category?: string
    keyword?: string
    from?: string
    to?: string
    page?: number
    size?: number
  }) =>
    client.get<ApiResponse<PagedResponse<LogEntryDto>>>('/logs/search', { params }),
}

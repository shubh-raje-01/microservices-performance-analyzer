import client from './client'
import type { ApiResponse, DashboardSummaryDto, DashboardOverviewDto, SystemHealthDto } from '../types'

export const dashboardApi = {
  getSummary: (simulationId: number) =>
    client.get<ApiResponse<DashboardSummaryDto>>(`/dashboard/${simulationId}`),

  getRecent: (limit = 10) =>
    client.get<ApiResponse<DashboardOverviewDto[]>>('/dashboard/recent', {
      params: { limit },
    }),

  getSystemHealth: () =>
    client.get<ApiResponse<SystemHealthDto>>('/dashboard/system-health'),
}

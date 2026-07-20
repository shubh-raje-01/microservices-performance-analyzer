import client from './client'
import type { ApiResponse, SimulationResponse, PagedResponse, AnalysisRequestDto } from '../types'

export const simulationApi = {
  run: (dto: AnalysisRequestDto) =>
    client.post<ApiResponse<SimulationResponse>>('/simulate', dto),

  getById: (id: number) =>
    client.get<ApiResponse<SimulationResponse>>(`/simulate/${id}`),

  list: (page = 0, size = 10) =>
    client.get<ApiResponse<PagedResponse<SimulationResponse>>>('/simulate', {
      params: { page, size },
    }),

  delete: (id: number) =>
    client.delete<ApiResponse<null>>(`/simulate/${id}`),
}

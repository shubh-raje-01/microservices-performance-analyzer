import client from './client'
import type {
  ApiResponse,
  PagedResponse,
  TraceSummaryDto,
  TraceDetailDto,
  TraceDependencyGraphDto,
} from '../types'

export const getTraces = (params: {
  page?: number
  size?: number
  serviceName?: string
  hoursBack?: number
}) =>
  client.get<ApiResponse<PagedResponse<TraceSummaryDto>>>('/traces', {
    params,
  })

export const getTraceById = (traceId: string) =>
  client.get<ApiResponse<TraceDetailDto>>(`/traces/${traceId}`)

export const getTraceGraph = (traceId: string) =>
  client.get<ApiResponse<TraceDependencyGraphDto>>(
    `/traces/${traceId}/graph`,
  )
import client from './client'
import type { TraceSummaryDto, TraceDetailDto, TraceDependencyGraphDto } from '../types'

export const getTraces = (params: {
  page?: number
  size?: number
  serviceName?: string
  hoursBack?: number
}) =>
  client.get<{ content: TraceSummaryDto[]; totalElements: number; totalPages: number }>('/traces', { params })

export const getTraceById = (traceId: string) =>
  client.get<TraceDetailDto>(`/traces/${traceId}`)

export const getTraceGraph = (traceId: string) =>
  client.get<TraceDependencyGraphDto>(`/traces/${traceId}/graph`)

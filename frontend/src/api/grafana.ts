import client from './client'
import type { ApiResponse, GrafanaWidgetData } from '../types'

export const grafanaApi = {
  getWidgets: (hours = 24) =>
    client.get<ApiResponse<GrafanaWidgetData>>('/grafana/widgets', {
      params: { hours },
    }),
}

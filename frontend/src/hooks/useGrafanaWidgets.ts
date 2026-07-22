import { useQuery } from '@tanstack/react-query'
import { grafanaApi } from '../api/grafana'

export function useGrafanaWidgets(hours = 24) {
  return useQuery({
    queryKey: ['grafanaWidgets', hours],
    queryFn: () => grafanaApi.getWidgets(hours).then((r) => r.data),
    refetchInterval: (_query) => {
      if (_query.state.error) return false
      return 30_000
    },
  })
}

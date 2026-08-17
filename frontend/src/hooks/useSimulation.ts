import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { simulationApi } from '../api/simulation'
import { useAnalyzerStore } from '../store'
import type { AnalysisRequestDto } from '../types'

export function useSimulationList(page = 0, size = 10) {
  return useQuery({
    queryKey: ['simulations', page, size],
    queryFn: () => simulationApi.list(page, size).then((r) => r.data),
    retry: (failureCount, error) => {
      if (error instanceof Error && error.message.includes('ECONNREFUSED')) return false
      return failureCount < 1
    },
  })
}

export function useSimulation(id: number | null) {
  return useQuery({
    queryKey: ['simulation', id],
    queryFn: () => simulationApi.getById(id!).then((r) => r.data),
    enabled: id != null,
  })
}

export function useRunSimulation() {
  const { upsertRunningSimulation, setPollingSimulationId } = useAnalyzerStore()

  return useMutation({
    mutationFn: (dto: AnalysisRequestDto) =>
      simulationApi.run(dto).then((r) => r.data),
    onSuccess: (response) => {
      const sim = response.data
      upsertRunningSimulation(sim)
      setPollingSimulationId(sim.id)
      toast.success('Simulation started — polling for results')
    },
  })
}

export function useDeleteSimulation() {
  const queryClient = useQueryClient()
  const { removeRunningSimulation } = useAnalyzerStore()

  return useMutation({
    mutationFn: (id: number) => simulationApi.delete(id),
    onSuccess: (_, id) => {
      removeRunningSimulation(id)
      queryClient.invalidateQueries({ queryKey: ['simulations'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Simulation deleted')
    },
    onError: () => {
      toast.error('Failed to delete simulation')
    },
  })
}

export function usePollingSimulation(id: number | null) {
  const queryClient = useQueryClient()
  const {
    upsertRunningSimulation,
    removeRunningSimulation,
    pollingSimulationId,
    setPollingSimulationId,
  } = useAnalyzerStore()

  const { data } = useQuery({
    queryKey: ['simulation', id],
    queryFn: () => simulationApi.getById(id!).then((r) => r.data),
    enabled: id != null,
    refetchInterval: (query) => {
      const status = query.state.data?.data?.status
      if (status === 'PENDING' || status === 'RUNNING') return 2000
      return false
    },
  })

  useEffect(() => {
    if (!data?.data) return
    const sim = data.data
    upsertRunningSimulation(sim)

    if (sim.status === 'COMPLETED') {
      removeRunningSimulation(sim.id)
      if (pollingSimulationId === sim.id) setPollingSimulationId(null)
      toast.success(`Simulation "${sim.scenarioName}" completed`)
      queryClient.invalidateQueries({ queryKey: ['simulations'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    } else if (sim.status === 'FAILED') {
      removeRunningSimulation(sim.id)
      if (pollingSimulationId === sim.id) setPollingSimulationId(null)
      toast.error(`Simulation "${sim.scenarioName}" failed`)
      queryClient.invalidateQueries({ queryKey: ['simulations'] })
    } else if (sim.status === 'CANCELLED') {
      removeRunningSimulation(sim.id)
      if (pollingSimulationId === sim.id) setPollingSimulationId(null)
      toast.info(`Simulation "${sim.scenarioName}" cancelled`)
      queryClient.invalidateQueries({ queryKey: ['simulations'] })
    }
  }, [
    data?.data,
    pollingSimulationId,
    queryClient,
    removeRunningSimulation,
    setPollingSimulationId,
    upsertRunningSimulation,
  ])
}

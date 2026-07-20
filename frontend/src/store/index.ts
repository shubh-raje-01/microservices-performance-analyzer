import { create } from 'zustand'
import type { SimulationResponse, DashboardSummaryDto, SystemHealthDto } from '../types'

type AnalyzerState = {
  activeSimulationId: number | null
  setActiveSimulationId: (id: number | null) => void
  pollingSimulationId: number | null
  setPollingSimulationId: (id: number | null) => void
  runningSimulations: Record<number, SimulationResponse>
  upsertRunningSimulation: (sim: SimulationResponse) => void
  removeRunningSimulation: (id: number) => void
  dashboardSummary: DashboardSummaryDto | null
  setDashboardSummary: (summary: DashboardSummaryDto | null) => void
  systemHealth: SystemHealthDto | null
  setSystemHealth: (health: SystemHealthDto | null) => void
  sidebarOpen: boolean
  toggleSidebar: () => void
  reset: () => void
}

const initialState = {
  activeSimulationId: null,
  pollingSimulationId: null,
  runningSimulations: {} as Record<number, SimulationResponse>,
  dashboardSummary: null,
  systemHealth: null,
  sidebarOpen: true,
}

export const useAnalyzerStore = create<AnalyzerState>((set) => ({
  ...initialState,

  setActiveSimulationId: (id) => set({ activeSimulationId: id }),

  setPollingSimulationId: (id) => set({ pollingSimulationId: id }),

  upsertRunningSimulation: (sim) =>
    set((state) => ({
      runningSimulations: { ...state.runningSimulations, [sim.id]: sim },
    })),

  removeRunningSimulation: (id) =>
    set((state) => {
      const { [id]: _, ...rest } = state.runningSimulations
      return { runningSimulations: rest }
    }),

  setDashboardSummary: (summary) => set({ dashboardSummary: summary }),

  setSystemHealth: (health) => set({ systemHealth: health }),

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),

  reset: () => set(initialState),
}))

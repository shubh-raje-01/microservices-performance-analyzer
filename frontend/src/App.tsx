import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import Dashboard from './pages/Dashboard'
import SimulationLaunch from './pages/SimulationLaunch'
import SimulationHistory from './pages/SimulationHistory'
import MetricsView from './pages/MetricsView'
import LogViewer from './pages/LogViewer'
import AIInsights from './pages/AIInsights'
import Recommendations from './pages/Recommendations'
import Traces from './pages/Traces'
import TraceDetail from './pages/TraceDetail'
import GrafanaDashboard from './pages/GrafanaDashboard'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/grafana" element={<GrafanaDashboard />} />
          <Route path="/simulate" element={<SimulationLaunch />} />
          <Route path="/history" element={<SimulationHistory />} />
          <Route path="/metrics/:id" element={<MetricsView />} />
          <Route path="/logs/:id" element={<LogViewer />} />
          <Route path="/insights/:id" element={<AIInsights />} />
          <Route path="/recommendations/:id" element={<Recommendations />} />
          <Route path="/traces" element={<Traces />} />
          <Route path="/traces/:traceId" element={<TraceDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App

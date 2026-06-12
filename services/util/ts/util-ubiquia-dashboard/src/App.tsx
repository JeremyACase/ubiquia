import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from '@/components/Layout'
import { OntologiesPanel } from '@/panels/OntologiesPanel'
import { DagsPanel } from '@/panels/DagsPanel'
import { AgentNetworkPanel } from '@/panels/AgentNetworkPanel'
import { ComponentsPanel } from '@/panels/ComponentsPanel'
import { BeliefStatesPanel } from '@/panels/BeliefStatesPanel'
import { FlowTracingPanel } from '@/panels/FlowTracingPanel'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/ubiquia/core/communication-service/dashboard">
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Navigate to="/ontologies" replace />} />
            <Route path="ontologies" element={<OntologiesPanel />} />
            <Route path="dags" element={<DagsPanel />} />
            <Route path="agents" element={<AgentNetworkPanel />} />
            <Route path="flows" element={<FlowTracingPanel />} />
            <Route path="belief-states" element={<BeliefStatesPanel />} />
            <Route path="components" element={<ComponentsPanel />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

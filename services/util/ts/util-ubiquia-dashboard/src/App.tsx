import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from '@/components/Layout'
import { Placeholder } from '@/components/Placeholder'
import { OntologiesPanel } from '@/panels/OntologiesPanel'
import { DagsPanel } from '@/panels/DagsPanel'

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
            <Route
              path="agents"
              element={
                <Placeholder
                  title="Agent Network"
                  description="Online Ubiquia agents, sync status, cluster membership, and pending outbox records."
                />
              }
            />
            <Route
              path="flows"
              element={
                <Placeholder
                  title="Flow Tracing"
                  description="Track active FlowIds through DAG pipelines — completion rates, durations, and failure details."
                />
              }
            />
            <Route
              path="belief-states"
              element={
                <Placeholder
                  title="Belief States"
                  description="Deployed belief state services, record counts, schema versions, and inline query."
                />
              }
            />
            <Route
              path="components"
              element={
                <Placeholder
                  title="Components"
                  description="Deployed component pods, container images, health status, and backpressure indicators."
                />
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

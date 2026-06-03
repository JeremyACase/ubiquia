import { API, http } from './client'
import type { Flow, FlowEvent, GenericPage } from '@/types/ubiquia'

export function fetchRecentEvents(size = 60): Promise<GenericPage<FlowEvent>> {
  return http.get(`${API.events}/query/params?sort-descending=true&page=0&size=${size}`)
}

export function fetchEventsForFlow(flowId: string): Promise<GenericPage<FlowEvent>> {
  return http.get(
    `${API.events}/query/params?flow.id=${flowId}&sort-descending=false&sort-by-fields=createdAt&page=0&size=100`,
  )
}

// Kept for potential future use once the flow proxy is stable
export function fetchRecentFlows(graphId: string, size = 20): Promise<GenericPage<Flow>> {
  return http.get(
    `${API.flows}/query/params?graph.id=${graphId}&sort-descending=true&page=0&size=${size}`,
  )
}

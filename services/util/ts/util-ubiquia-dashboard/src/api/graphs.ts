import { API, http } from './client'
import type { Graph, GenericPage } from '@/types/ubiquia'

export function fetchGraphs(page = 0, size = 50): Promise<GenericPage<Graph>> {
  return http.get(`${API.graphs}/query/params?page=${page}&size=${size}`)
}

import { API, http } from './client'
import type { Agent } from '@/types/ubiquia'

export function fetchAgent(): Promise<Agent> {
  return http.get(`${API.agent}/instance/get`)
}

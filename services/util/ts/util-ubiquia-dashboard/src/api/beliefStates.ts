import { API, http } from './client'
import type { BeliefStateGeneration } from '@/types/ubiquia'

export function generateBeliefState(body: BeliefStateGeneration): Promise<BeliefStateGeneration> {
  return http.post(`${API.beliefState}/generate`, body)
}

export function teardownBeliefState(body: BeliefStateGeneration): Promise<BeliefStateGeneration> {
  return http.post(`${API.beliefState}/teardown`, body)
}

export function fetchProxiedUrls(): Promise<string[]> {
  return http.get(API.proxiedUrls)
}

export async function probeHealth(url: string): Promise<{ status: string }> {
  const healthUrl = url.replace(/\/$/, '') + '/actuator/health'
  return http.get(healthUrl)
}

import { API, http } from './client'
import type { Component, GenericPage } from '@/types/ubiquia'

export function fetchComponents(page = 0, size = 20): Promise<GenericPage<Component>> {
  return http.get(`${API.components}/query/params?page=${page}&size=${size}`)
}

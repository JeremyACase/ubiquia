import { API, http } from './client'
import type { DomainOntology, GenericPage, IngressResponse } from '@/types/ubiquia'

export function fetchOntologies(page = 0, size = 20): Promise<GenericPage<DomainOntology>> {
  return http.get(`${API.ontologies}/query/params?page=${page}&size=${size}`)
}

export function fetchOntology(id: string): Promise<DomainOntology> {
  return http.get(`${API.ontologies}/query/${id}`)
}

export function registerOntology(ontology: DomainOntology): Promise<IngressResponse> {
  return http.post(`${API.ontologies}/register/post`, ontology)
}

export function uploadOntology(file: File): Promise<IngressResponse> {
  return http.upload(`${API.ontologies}/register/upload`, file)
}

export function deleteOntology(id: string): Promise<string> {
  return http.delete(`${API.ontologies}/delete/${id}`)
}

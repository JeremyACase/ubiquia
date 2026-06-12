const FLOW_SERVICE = '/ubiquia/core/communication-service/flow-service'

const BELIEF_STATE_SERVICE = '/ubiquia/core/communication-service/belief-state-generator-service'

export const API = {
  ontologies: `${FLOW_SERVICE}/domain-ontology`,
  graphs: `${FLOW_SERVICE}/graph`,
  components: `${FLOW_SERVICE}/component`,
  agent: `${FLOW_SERVICE}/agent`,
  flows: `${FLOW_SERVICE}/flow`,
  events: `${FLOW_SERVICE}/event`,
  beliefState: `${BELIEF_STATE_SERVICE}/belief-state`,
  proxiedUrls: '/ubiquia/core-communication-service/component/get-proxied-urls',
} as const

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  })
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText)
    throw new Error(`${res.status} ${res.statusText}: ${text}`)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const http = {
  get: <T>(url: string) => request<T>(url),
  post: <T>(url: string, body: unknown) =>
    request<T>(url, { method: 'POST', body: JSON.stringify(body) }),
  delete: <T>(url: string) => request<T>(url, { method: 'DELETE' }),
  upload: <T>(url: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<T>(url, { method: 'POST', body: form, headers: {} })
  },
}

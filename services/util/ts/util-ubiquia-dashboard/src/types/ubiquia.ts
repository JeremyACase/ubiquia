export interface SemanticVersion {
  major: number
  minor: number
  patch: number
}

export interface KeyValuePair {
  key: string
  value: string
}

export interface BaseModel {
  id: string
  modelType: string
  createdAt?: string
  updatedAt?: string
  tags?: KeyValuePair[]
}

export interface DomainDataContract {
  schema?: Record<string, unknown>
}

export interface GraphEdge {
  leftNodeName: string
  rightNodeNames: string[]
}

export interface NodeSettings {
  persistPayload?: boolean
  validateInput?: boolean
  validateOutput?: boolean
}

export interface Node extends BaseModel {
  modelType: 'Node'
  name: string
  nodeType: string
  description?: string
  endpoint?: string
  upstreamNodes?: Node[]
  downstreamNodes?: Node[]
}

export interface Component extends BaseModel {
  modelType: 'Component'
  name: string
  componentType: string
  port?: number
}

export interface Graph extends BaseModel {
  modelType: 'Graph'
  name: string
  description: string
  author?: string
  nodes?: Node[]
  components?: Component[]
  edges?: GraphEdge[]
  capabilities?: string[]
}

export interface DomainOntology extends BaseModel {
  modelType: 'DomainOntology'
  name: string
  author?: string
  description?: string
  version: SemanticVersion
  graphs?: Graph[]
  domainDataContract?: DomainDataContract
}

export interface GenericPage<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  isLast: boolean
  isFirst: boolean
  numberOfElements: number
  isUnsorted: boolean
}

export interface IngressResponse {
  id: string
  modelType: 'IngressResponse'
  payloadModelType: string
}

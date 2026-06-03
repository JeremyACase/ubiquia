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

export interface Image {
  registry: string
  repository: string
  tag: string
}

export interface Config {
  configMap: string
  configMountPath: string
}

export interface CommunicationServiceSettings {
  exposeViaCommService: boolean
  proxiedEndpoint: string
}

export interface Component extends BaseModel {
  modelType: 'Component'
  name: string
  componentType: 'NONE' | 'TEMPLATE' | 'POD' | string
  port: number
  image: Image
  description?: string
  exposeService?: boolean
  config?: Config
  communicationServiceSettings?: CommunicationServiceSettings
  node?: Node
  graph?: Graph
  postStartExecCommands?: string[]
  environmentVariables?: { name: string; value: string }[]
}

export interface BeliefStateGeneration {
  domainName: string
  version: SemanticVersion
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

export interface FlowEventTimes {
  pollStartedTime?: string
  payloadSentToComponentTime?: string
  componentResponseTime?: string
  eventStartTime?: string
  eventCompleteTime?: string
  sentToOutboxTime?: string
  egressResponseReceivedTime?: string
  payloadEgressedTime?: string
}

export interface FlowEvent extends BaseModel {
  modelType: 'FlowEvent'
  flow?: { id: string; graph?: { id: string; name: string } }
  node?: Node
  inputPayload?: unknown
  outputPayload?: unknown
  httpResponseCode?: number
  flowEventTimes?: FlowEventTimes
  inputPayloadStamps?: KeyValuePair[]
  outputPayloadStamps?: KeyValuePair[]
}

export interface Flow extends BaseModel {
  modelType: 'Flow'
  graph?: Graph
  flowEvents?: FlowEvent[]
}

export interface Update extends BaseModel {
  modelType: 'Update'
  updateReason?: string
  model?: unknown
}

export interface Sync extends BaseModel {
  modelType: 'Sync'
  model?: unknown
}

export interface Agent extends BaseModel {
  modelType: 'Agent'
  baseUrl?: string
  reachable: boolean
  deployedGraphs?: Graph[]
  updates?: Update[]
  syncs?: Sync[]
  network?: {
    id: string
    agents?: Agent[]
  }
}

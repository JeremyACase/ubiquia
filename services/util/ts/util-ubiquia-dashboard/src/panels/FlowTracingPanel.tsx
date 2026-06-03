import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Activity, RefreshCw, AlertCircle, GitBranch, ChevronRight, ChevronDown, X, Clock, CheckCircle, XCircle } from 'lucide-react'
import { fetchGraphs } from '@/api/graphs'
import { fetchRecentEvents, fetchEventsForFlow } from '@/api/flows'
import type { Graph, FlowEvent } from '@/types/ubiquia'

// ── Layout constants ──────────────────────────────────────────────────────────
const NW = 164
const NH = 50
const HG = 96
const VG = 24
const PAD = 32

type Pos = { x: number; y: number }
type LayoutResult = { pos: Record<string, Pos>; w: number; h: number }

function layoutGraph(graph: Graph): LayoutResult {
  const nodes = graph.nodes ?? []
  const edges = graph.edges ?? []
  if (nodes.length === 0) return { pos: {}, w: PAD * 2, h: PAD * 2 }

  const childrenOf: Record<string, string[]> = {}
  const inDeg: Record<string, number> = {}
  for (const n of nodes) { childrenOf[n.name] = []; inDeg[n.name] = 0 }
  for (const e of edges) {
    for (const r of e.rightNodeNames) {
      childrenOf[e.leftNodeName]?.push(r)
      inDeg[r] = (inDeg[r] ?? 0) + 1
    }
  }
  const layer: Record<string, number> = {}
  const q = nodes.filter(n => !inDeg[n.name]).map(n => n.name)
  for (const n of q) layer[n] = 0
  const visited = new Set<string>()
  const bfs = [...q]
  while (bfs.length) {
    const cur = bfs.shift()!
    if (visited.has(cur)) continue
    visited.add(cur)
    for (const child of childrenOf[cur] ?? []) {
      layer[child] = Math.max(layer[child] ?? 0, (layer[cur] ?? 0) + 1)
      if (--inDeg[child] <= 0) bfs.push(child)
    }
  }
  for (const n of nodes) if (layer[n.name] === undefined) layer[n.name] = 0

  const groups: Record<number, string[]> = {}
  for (const n of nodes) (groups[layer[n.name]!] ??= []).push(n.name)

  const maxL = Math.max(...Object.keys(groups).map(Number))
  const maxN = Math.max(...Object.values(groups).map(g => g.length))
  const w = PAD * 2 + (maxL + 1) * NW + maxL * HG
  const h = PAD * 2 + maxN * NH + (maxN - 1) * VG

  const pos: Record<string, Pos> = {}
  for (let l = 0; l <= maxL; l++) {
    const grp = groups[l] ?? []
    const gh = grp.length * NH + (grp.length - 1) * VG
    const top = PAD + (h - PAD * 2 - gh) / 2
    grp.forEach((name, i) => {
      pos[name] = { x: PAD + l * (NW + HG), y: top + i * (NH + VG) }
    })
  }
  return { pos, w, h }
}

// ── Node type colours ─────────────────────────────────────────────────────────
const TYPE_STYLE: Record<string, { fill: string; stroke: string; label: string }> = {
  INGRESS:   { fill: '#0f2a4a', stroke: '#3b82f6', label: '#93c5fd' },
  EGRESS:    { fill: '#230f4a', stroke: '#8b5cf6', label: '#c4b5fd' },
  RELAY:     { fill: '#0a2a1e', stroke: '#10b981', label: '#6ee7b7' },
  SOURCE:    { fill: '#0f2a4a', stroke: '#38bdf8', label: '#7dd3fc' },
  SINK:      { fill: '#2a0f0f', stroke: '#f87171', label: '#fca5a5' },
  PROCESSOR: { fill: '#1a2a0a', stroke: '#84cc16', label: '#bef264' },
}
const DFLT = { fill: '#1e293b', stroke: '#475569', label: '#94a3b8' }
const typeStyle = (t?: string) => t ? (TYPE_STYLE[t.toUpperCase()] ?? DFLT) : DFLT

// ── Flow colors ───────────────────────────────────────────────────────────────
const PALETTE = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#06b6d4', '#f97316', '#84cc16', '#ec4899']
function flowColor(id: string): string {
  const h = id.split('').reduce((a, c) => (Math.imul(31, a) + c.charCodeAt(0)) | 0, 0)
  return PALETTE[Math.abs(h) % PALETTE.length]
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function timeAgo(iso?: string) {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  if (diff < 60_000) return `${Math.floor(diff / 1000)}s ago`
  const mins = Math.floor(diff / 60_000)
  if (mins < 60) return `${mins}m ago`
  return `${Math.floor(mins / 60)}h ago`
}

function durationMs(start?: string, end?: string): number | null {
  if (!start || !end) return null
  return new Date(end).getTime() - new Date(start).getTime()
}

function statusColor(code?: number) {
  if (!code) return '#64748b'
  if (code < 300) return '#10b981'
  if (code < 400) return '#f59e0b'
  return '#ef4444'
}

// ── Flow summary (derived from events, no separate flows API needed) ───────────
interface FlowSummary {
  id: string
  nodeNames: string[]   // visit order
  latestTime: string
  hasError: boolean
  color: string
}

function deriveFlows(events: FlowEvent[]): FlowSummary[] {
  const map = new Map<string, { nodeNames: string[]; latestTime: string; hasError: boolean }>()
  const sorted = [...events].sort(
    (a, b) => new Date(a.createdAt ?? 0).getTime() - new Date(b.createdAt ?? 0).getTime(),
  )
  for (const evt of sorted) {
    if (!evt.flow?.id) continue
    const fid = evt.flow.id
    if (!map.has(fid)) map.set(fid, { nodeNames: [], latestTime: '', hasError: false })
    const entry = map.get(fid)!
    const name = evt.node?.name
    if (name && !entry.nodeNames.includes(name)) entry.nodeNames.push(name)
    if ((evt.httpResponseCode ?? 0) >= 400) entry.hasError = true
    if ((evt.createdAt ?? '') > entry.latestTime) entry.latestTime = evt.createdAt ?? ''
  }
  return Array.from(map.entries())
    .sort(([, a], [, b]) => b.latestTime.localeCompare(a.latestTime))
    .map(([id, d]) => ({ id, ...d, color: flowColor(id) }))
}

// ── Canvas ────────────────────────────────────────────────────────────────────

function TraceCanvas({
  graph,
  activeEvents,
  selectedFlowId,
}: {
  graph: Graph
  activeEvents: FlowEvent[]
  selectedFlowId: string | null
}) {
  const { pos, w, h } = layoutGraph(graph)
  const nodes = graph.nodes ?? []
  const edges = graph.edges ?? []
  const now = Date.now()

  // Set of DAG edges that exist, for trail rendering
  const dagEdgeSet = useMemo(
    () => new Set(edges.flatMap(e => e.rightNodeNames.map(r => `${e.leftNodeName}→${r}`))),
    [edges],
  )

  // Active node IDs (event in last 8s)
  const activeNodeIds = useMemo(() => {
    const s = new Set<string>()
    for (const e of activeEvents)
      if (e.node?.id && now - new Date(e.createdAt ?? 0).getTime() < 8000)
        s.add(e.node.id)
    return s
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeEvents])

  // Error node IDs
  const errorNodeIds = useMemo(() => {
    const s = new Set<string>()
    for (const e of activeEvents)
      if (e.node?.id && (e.httpResponseCode ?? 0) >= 400 &&
          now - new Date(e.createdAt ?? 0).getTime() < 8000)
        s.add(e.node.id)
    return s
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeEvents])

  // Active flow paths — each flow gets a globe at its current node
  const flowPaths = useMemo(() => {
    const all = deriveFlows(activeEvents)
    // Only flows with activity in last 20s
    return all.filter(fp => now - new Date(fp.latestTime).getTime() < 20000)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeEvents])

  return (
    <div
      className="flex-1 overflow-auto bg-slate-950"
      style={{ backgroundImage: 'radial-gradient(#1e293b 1px, transparent 1px)', backgroundSize: '20px 20px' }}
    >
      <svg width={w} height={h}>
        <defs>
          <marker id="trace-arrow" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto">
            <path d="M0,0.5 L0,6.5 L7,3.5 z" fill="#475569" />
          </marker>
        </defs>

        {/* Base edges */}
        {edges.flatMap(e =>
          e.rightNodeNames.map(r => {
            const f = pos[e.leftNodeName], t = pos[r]
            if (!f || !t) return null
            const x1 = f.x + NW, y1 = f.y + NH / 2
            const x2 = t.x,      y2 = t.y + NH / 2
            const mx = (x1 + x2) / 2
            return (
              <path
                key={`edge-${e.leftNodeName}-${r}`}
                d={`M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2},${y2}`}
                fill="none" stroke="#334155" strokeWidth={1.5}
                markerEnd="url(#trace-arrow)"
              />
            )
          })
        )}

        {/* Traversed-edge trails (dashed, per active flow) */}
        {flowPaths.flatMap(fp =>
          fp.nodeNames.slice(0, -1).flatMap((from, i) => {
            const to = fp.nodeNames[i + 1]
            if (!dagEdgeSet.has(`${from}→${to}`)) return []
            const f = pos[from], t = pos[to]
            if (!f || !t) return []
            const x1 = f.x + NW, y1 = f.y + NH / 2
            const x2 = t.x,      y2 = t.y + NH / 2
            const mx = (x1 + x2) / 2
            const dim = selectedFlowId && fp.id !== selectedFlowId
            return (
              <path
                key={`trail-${fp.id}-${from}-${to}`}
                d={`M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2},${y2}`}
                fill="none"
                stroke={fp.color}
                strokeWidth={2}
                strokeDasharray="5 4"
                opacity={dim ? 0.1 : 0.45}
              />
            )
          })
        )}

        {/* Nodes */}
        {nodes.map(node => {
          const p = pos[node.name]
          if (!p) return null
          const s = typeStyle(node.nodeType)
          const isActive = activeNodeIds.has(node.id)
          const isError  = errorNodeIds.has(node.id)
          const activeStroke = isError ? '#ef4444' : '#3b82f6'
          const label = node.name.length > 20 ? node.name.slice(0, 19) + '…' : node.name

          return (
            <g key={node.id} transform={`translate(${p.x},${p.y})`}>
              {isActive && (
                <rect
                  x={-3} y={-3} width={NW + 6} height={NH + 6} rx={8}
                  fill="none" stroke={activeStroke} strokeWidth={1.5} opacity={0.5}
                  style={{ animation: 'nodePulse 1.5s ease-in-out infinite' }}
                />
              )}
              <rect
                width={NW} height={NH} rx={6}
                fill={s.fill}
                stroke={isActive ? activeStroke : s.stroke}
                strokeWidth={isActive ? 2 : 1.5}
              />
              <text x={10} y={18} fontSize={9} fill={s.label} fontFamily="ui-monospace,monospace">
                {node.nodeType?.toUpperCase()}
              </text>
              <text x={10} y={36} fontSize={12} fill="#e2e8f0" fontFamily="ui-sans-serif,system-ui,sans-serif">
                {label}
              </text>
            </g>
          )
        })}

        {/* Flow globes — CSS transform transition moves them between nodes */}
        {flowPaths.map(fp => {
          const currentNode = fp.nodeNames[fp.nodeNames.length - 1]
          const p = pos[currentNode]
          if (!p) return null
          const cx = p.x + NW / 2
          const cy = p.y + NH / 2
          const dim = selectedFlowId && fp.id !== selectedFlowId

          return (
            <g
              key={`glob-${fp.id}`}
              style={{
                transform: `translate(${cx}px, ${cy}px)`,
                transition: 'transform 0.85s cubic-bezier(0.4, 0, 0.2, 1)',
                willChange: 'transform',
                opacity: dim ? 0.15 : 1,
              }}
            >
              {/* Outer pulse ring */}
              <circle
                r={13} fill="none" stroke={fp.color} strokeWidth={1.5} opacity={0.35}
                style={{ animation: 'globePulse 2s ease-in-out infinite' }}
              />
              {/* Globe body */}
              <circle r={7} fill={fp.color} style={{ filter: `drop-shadow(0 0 5px ${fp.color})` }} />
              <title>{fp.id.slice(0, 8)} — {fp.nodeNames.join(' → ')}</title>
            </g>
          )
        })}
      </svg>

      <style>{`
        @keyframes nodePulse {
          0%, 100% { opacity: 0.4; }
          50%       { opacity: 0.9; }
        }
        @keyframes globePulse {
          0%, 100% { r: 13; opacity: 0.25; }
          50%       { r: 17; opacity: 0.5; }
        }
      `}</style>
    </div>
  )
}

// ── Flow list item ────────────────────────────────────────────────────────────

function FlowItem({
  flow,
  selected,
  onClick,
}: {
  flow: FlowSummary
  selected: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={[
        'w-full text-left px-3 py-2.5 border-b border-slate-800 transition-colors',
        selected ? 'bg-slate-800' : 'hover:bg-slate-800/50',
      ].join(' ')}
    >
      <div className="flex items-center gap-2">
        <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: flow.color }} />
        {flow.hasError
          ? <XCircle size={10} className="text-red-400 shrink-0" />
          : <CheckCircle size={10} className="text-green-400 shrink-0" />}
        <span className="text-xs font-mono text-slate-400 truncate flex-1">{flow.id.slice(0, 8)}…</span>
        <span className="text-xs text-slate-600 shrink-0">{timeAgo(flow.latestTime)}</span>
      </div>
      {flow.nodeNames.length > 0 && (
        <p className="text-xs text-slate-600 mt-0.5 ml-5 truncate">{flow.nodeNames.join(' → ')}</p>
      )}
    </button>
  )
}

// ── Event timeline ────────────────────────────────────────────────────────────

function EventTimeline({
  events,
  onEventClick,
  selectedEventId,
}: {
  events: FlowEvent[]
  onEventClick: (e: FlowEvent) => void
  selectedEventId: string | null
}) {
  return (
    <div className="space-y-1.5">
      {events.map(evt => {
        const dur = durationMs(evt.flowEventTimes?.eventStartTime, evt.flowEventTimes?.eventCompleteTime)
        const code = evt.httpResponseCode
        return (
          <button
            key={evt.id}
            onClick={() => onEventClick(evt)}
            className={[
              'w-full text-left px-3 py-2 rounded border transition-colors',
              selectedEventId === evt.id
                ? 'bg-slate-800 border-blue-700'
                : 'bg-slate-800/40 border-slate-700 hover:border-slate-500',
            ].join(' ')}
          >
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full shrink-0" style={{ background: statusColor(code) }} />
              <span className="text-xs text-slate-200 font-medium truncate flex-1">{evt.node?.name ?? '—'}</span>
              {code && <span className="text-xs font-mono shrink-0" style={{ color: statusColor(code) }}>{code}</span>}
              {dur != null && <span className="text-xs text-slate-600 shrink-0">{dur}ms</span>}
            </div>
            <p className="text-xs text-slate-600 mt-0.5 ml-4">{timeAgo(evt.createdAt)}</p>
          </button>
        )
      })}
    </div>
  )
}

// ── Event detail ──────────────────────────────────────────────────────────────

function EventDetail({ event, onClose }: { event: FlowEvent; onClose: () => void }) {
  const [tab, setTab] = useState<'in' | 'out' | 'timing'>('in')
  const times = event.flowEventTimes ?? {}
  const inDur = durationMs(times.eventStartTime, times.eventCompleteTime)

  function renderPayload(payload: unknown) {
    if (payload == null) return <p className="text-slate-600 text-xs italic">No payload</p>
    const str = typeof payload === 'string' ? payload : JSON.stringify(payload, null, 2)
    return (
      <pre className="text-xs text-green-300 font-mono whitespace-pre-wrap break-all bg-slate-950 rounded p-3 border border-slate-700 max-h-64 overflow-auto">
        {str}
      </pre>
    )
  }

  return (
    <div className="border-t border-slate-700 mt-3 pt-3">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold text-slate-300">{event.node?.name}</span>
        <button onClick={onClose} className="text-slate-600 hover:text-slate-400"><X size={12} /></button>
      </div>
      <div className="flex gap-1 mb-3">
        {(['in', 'out', 'timing'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={['px-2 py-1 text-xs rounded', tab === t ? 'bg-blue-700 text-white' : 'text-slate-500 hover:text-slate-300'].join(' ')}>
            {t === 'in' ? 'Input' : t === 'out' ? 'Output' : 'Timing'}
          </button>
        ))}
      </div>
      {tab === 'in' && renderPayload(event.inputPayload)}
      {tab === 'out' && renderPayload(event.outputPayload)}
      {tab === 'timing' && (
        <div className="space-y-1.5 text-xs font-mono">
          {([
            ['Event start',         times.eventStartTime],
            ['Sent to component',   times.payloadSentToComponentTime],
            ['Component responded', times.componentResponseTime],
            ['Event complete',      times.eventCompleteTime],
            ['Sent to outbox',      times.sentToOutboxTime],
          ] as [string, string | undefined][]).map(([label, val]) =>
            val ? (
              <div key={label} className="flex gap-2">
                <span className="text-slate-500 w-36 shrink-0">{label}</span>
                <span className="text-slate-300">{new Date(val).toISOString().slice(11, 23)}</span>
              </div>
            ) : null
          )}
          {inDur != null && (
            <div className="flex gap-2 pt-1 border-t border-slate-800">
              <span className="text-slate-500 w-36 shrink-0">Total duration</span>
              <span className="text-slate-200 font-semibold">{inDur}ms</span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── Panel ─────────────────────────────────────────────────────────────────────

export function FlowTracingPanel() {
  const [selectedGraphId, setSelectedGraphId] = useState<string | null>(null)
  const [selectedFlowId, setSelectedFlowId] = useState<string | null>(null)
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null)
  const [graphListOpen, setGraphListOpen] = useState(true)
  const [autoRefresh, setAutoRefresh] = useState(true)

  const { data: graphsData } = useQuery({
    queryKey: ['graphs', 0],
    queryFn: () => fetchGraphs(0, 50),
  })
  const graphs = graphsData?.content ?? []
  const selectedGraph = graphs.find(g => g.id === selectedGraphId) ?? null

  const graphNodeIds   = useMemo(() => new Set((selectedGraph?.nodes ?? []).map(n => n.id)), [selectedGraph])
  const graphNodeNames = useMemo(() => new Set((selectedGraph?.nodes ?? []).map(n => n.name)), [selectedGraph])

  const { data: eventsData, isFetching: fetchingEvents, refetch: refetchEvents } = useQuery({
    queryKey: ['trace-events'],
    queryFn: () => fetchRecentEvents(80),
    refetchInterval: autoRefresh ? 3000 : false,
  })
  const allRecentEvents = eventsData?.content ?? []

  // Events filtered to selected graph's nodes
  const activeEvents = useMemo(
    () => allRecentEvents.filter(e => e.node?.id && graphNodeIds.has(e.node.id)),
    [allRecentEvents, graphNodeIds],
  )

  // Flow list derived entirely from events — no separate /flow API call
  const recentFlows = useMemo(() => deriveFlows(activeEvents), [activeEvents])

  // Full event set for a selected flow (fetched on demand)
  const { data: flowEventsData, isLoading: loadingFlowEvents } = useQuery({
    queryKey: ['trace-flow-events', selectedFlowId],
    queryFn: () => fetchEventsForFlow(selectedFlowId!),
    enabled: !!selectedFlowId,
  })
  const selectedFlowEvents = useMemo(
    () => (flowEventsData?.content ?? []).filter(e => graphNodeNames.has(e.node?.name ?? '')),
    [flowEventsData, graphNodeNames],
  )

  const selectedEvent = selectedEventId
    ? selectedFlowEvents.find(e => e.id === selectedEventId) ?? null
    : null

  function selectGraph(g: Graph) {
    setSelectedGraphId(g.id)
    setSelectedFlowId(null)
    setSelectedEventId(null)
  }

  return (
    <div className="flex h-full overflow-hidden">
      {/* ── Graph list ── */}
      <div className="w-52 shrink-0 flex flex-col border-r border-slate-700 bg-slate-900">
        <button
          onClick={() => setGraphListOpen(o => !o)}
          className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 text-sm font-semibold text-slate-100 hover:bg-slate-800 transition-colors"
        >
          {graphListOpen ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
          Graphs
          {graphs.length > 0 && <span className="ml-auto text-xs text-slate-500">{graphs.length}</span>}
        </button>

        {graphListOpen && (
          <div className="flex-1 overflow-auto">
            {graphs.map(g => {
              const evtCount = allRecentEvents.filter(
                e => e.node?.id && new Set((g.nodes ?? []).map(n => n.id)).has(e.node!.id),
              ).length
              return (
                <button key={g.id} onClick={() => selectGraph(g)}
                  className={[
                    'w-full text-left px-4 py-2.5 border-b border-slate-800 transition-colors',
                    selectedGraphId === g.id ? 'bg-slate-800 border-r-2 border-r-blue-400' : 'hover:bg-slate-800/50',
                  ].join(' ')}
                >
                  <div className="flex items-center gap-2">
                    <GitBranch size={12} className={selectedGraphId === g.id ? 'text-blue-400' : 'text-slate-500'} />
                    <span className={`text-xs font-medium truncate ${selectedGraphId === g.id ? 'text-slate-100' : 'text-slate-300'}`}>
                      {g.name}
                    </span>
                    {evtCount > 0 && (
                      <span className="ml-auto shrink-0 text-xs bg-blue-900/60 text-blue-300 rounded px-1 py-0.5 leading-none">
                        {evtCount}
                      </span>
                    )}
                  </div>
                </button>
              )
            })}
          </div>
        )}
      </div>

      {/* ── Canvas ── */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex items-center gap-3 px-4 py-2.5 border-b border-slate-700 shrink-0">
          <Activity size={14} className="text-slate-400" />
          <span className="text-sm font-semibold text-slate-100 flex-1">
            {selectedGraph?.name ?? 'Flow Tracing'}
          </span>
          <button
            onClick={() => setAutoRefresh(a => !a)}
            className={[
              'flex items-center gap-1.5 px-2.5 py-1 rounded text-xs transition-colors border',
              autoRefresh ? 'bg-blue-900/50 text-blue-300 border-blue-700' : 'text-slate-500 border-slate-700 hover:border-slate-500',
            ].join(' ')}
          >
            <RefreshCw size={10} className={autoRefresh && fetchingEvents ? 'animate-spin' : ''} />
            {autoRefresh ? 'Live' : 'Paused'}
          </button>
          <button onClick={() => refetchEvents()}
            className="p-1.5 rounded text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors">
            <RefreshCw size={12} />
          </button>
        </div>

        {!selectedGraph ? (
          <div className="flex-1 flex flex-col items-center justify-center gap-3 text-center">
            <Activity size={36} className="text-slate-700" />
            <p className="text-slate-400 text-sm">Select a graph to trace flows</p>
            <p className="text-slate-600 text-xs">Choose a DAG from the list on the left</p>
          </div>
        ) : (selectedGraph.nodes?.length ?? 0) === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-slate-500 text-sm">No nodes in this graph</p>
          </div>
        ) : (
          <TraceCanvas
            graph={selectedGraph}
            activeEvents={activeEvents}
            selectedFlowId={selectedFlowId}
          />
        )}
      </div>

      {/* ── Flow sidebar ── */}
      {selectedGraph && (
        <div className="w-72 shrink-0 flex flex-col border-l border-slate-700 bg-slate-900 overflow-hidden">
          <div className="px-3 py-2.5 border-b border-slate-700 shrink-0">
            <p className="text-xs font-semibold text-slate-300">Recent Flows</p>
            {recentFlows.length > 0 && (
              <p className="text-xs text-slate-600 mt-0.5">{recentFlows.length} tracked</p>
            )}
          </div>

          {recentFlows.length === 0 ? (
            <div className="flex flex-col items-center justify-center flex-1 gap-2 px-4 text-center">
              <Clock size={20} className="text-slate-700" />
              <p className="text-xs text-slate-600">No flows recorded yet</p>
            </div>
          ) : (
            <div className="flex flex-col flex-1 overflow-hidden">
              <div className={`overflow-auto ${selectedFlowId ? 'max-h-48 border-b border-slate-800' : 'flex-1'}`}>
                {recentFlows.map(f => (
                  <FlowItem
                    key={f.id}
                    flow={f}
                    selected={selectedFlowId === f.id}
                    onClick={() => {
                      if (selectedFlowId === f.id) { setSelectedFlowId(null); setSelectedEventId(null) }
                      else { setSelectedFlowId(f.id); setSelectedEventId(null) }
                    }}
                  />
                ))}
              </div>

              {selectedFlowId && (
                <div className="flex-1 overflow-auto px-3 py-3">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold text-slate-400">Event Timeline</span>
                    <button onClick={() => { setSelectedFlowId(null); setSelectedEventId(null) }}
                      className="text-slate-600 hover:text-slate-400"><X size={12} /></button>
                  </div>

                  {loadingFlowEvents ? (
                    <div className="flex items-center gap-2 text-xs text-slate-500">
                      <RefreshCw size={11} className="animate-spin" /> Loading…
                    </div>
                  ) : selectedFlowEvents.length === 0 ? (
                    <div className="flex flex-col items-center gap-2 py-4 text-center">
                      <AlertCircle size={16} className="text-slate-700" />
                      <p className="text-xs text-slate-600">No events found for this flow</p>
                    </div>
                  ) : (
                    <>
                      <EventTimeline
                        events={selectedFlowEvents}
                        selectedEventId={selectedEventId}
                        onEventClick={e => setSelectedEventId(selectedEventId === e.id ? null : e.id)}
                      />
                      {selectedEvent && (
                        <EventDetail event={selectedEvent} onClose={() => setSelectedEventId(null)} />
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

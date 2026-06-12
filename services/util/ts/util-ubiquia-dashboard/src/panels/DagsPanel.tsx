import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { GitBranch, RefreshCw, AlertCircle, X, Cpu } from 'lucide-react'
import { fetchGraphs } from '@/api/graphs'
import type { Graph, Node } from '@/types/ubiquia'

// ── Layout constants ──────────────────────────────────────────────────────────
const NW = 164   // node width
const NH = 50    // node height
const HG = 96    // horizontal gap between layers
const VG = 24    // vertical gap between nodes in same layer
const PAD = 32   // canvas padding

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
const typeStyle = (t?: string) =>
  t ? (TYPE_STYLE[t.toUpperCase()] ?? DFLT) : DFLT

// ── DAG layout (longest-path layering) ───────────────────────────────────────
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

  // Kahn's BFS + longest-path layer assignment
  const layer: Record<string, number> = {}
  const q: string[] = nodes.filter(n => !inDeg[n.name]).map(n => n.name)
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
  // Disconnected nodes fall into layer 0
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

// ── SVG canvas ────────────────────────────────────────────────────────────────
function DagSvg({
  graph,
  selectedNode,
  onNodeClick,
}: {
  graph: Graph
  selectedNode: Node | null
  onNodeClick: (node: Node | null) => void
}) {
  const { pos, w, h } = layoutGraph(graph)
  const nodes = graph.nodes ?? []
  const edges = graph.edges ?? []

  return (
    <div
      className="flex-1 overflow-auto bg-slate-950"
      style={{
        backgroundImage: 'radial-gradient(#1e293b 1px, transparent 1px)',
        backgroundSize: '20px 20px',
      }}
      onClick={() => onNodeClick(null)}
    >
      <svg width={w} height={h}>
        <defs>
          <marker
            id="dag-arrow"
            markerWidth="7" markerHeight="7"
            refX="6" refY="3.5"
            orient="auto"
          >
            <path d="M0,0.5 L0,6.5 L7,3.5 z" fill="#475569" />
          </marker>
        </defs>

        {/* Edges */}
        {edges.flatMap(e =>
          e.rightNodeNames.map(r => {
            const f = pos[e.leftNodeName]
            const t = pos[r]
            if (!f || !t) return null
            const x1 = f.x + NW, y1 = f.y + NH / 2
            const x2 = t.x,      y2 = t.y + NH / 2
            const mx = (x1 + x2) / 2
            return (
              <path
                key={`${e.leftNodeName}→${r}`}
                d={`M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2},${y2}`}
                fill="none"
                stroke="#334155"
                strokeWidth={1.5}
                markerEnd="url(#dag-arrow)"
              />
            )
          })
        )}

        {/* Nodes */}
        {nodes.map(node => {
          const p = pos[node.name]
          if (!p) return null
          const s = typeStyle(node.nodeType)
          const isSelected = selectedNode?.id === node.id
          const label = node.name.length > 20 ? node.name.slice(0, 19) + '…' : node.name
          return (
            <g
              key={node.id ?? node.name}
              transform={`translate(${p.x},${p.y})`}
              onClick={ev => { ev.stopPropagation(); onNodeClick(node) }}
              style={{ cursor: 'pointer' }}
            >
              <rect
                width={NW} height={NH} rx={6}
                fill={s.fill}
                stroke={isSelected ? '#f59e0b' : s.stroke}
                strokeWidth={isSelected ? 2 : 1.5}
              />
              <text
                x={10} y={18}
                fontSize={9}
                fill={s.label}
                fontFamily="ui-monospace,monospace"
              >
                {node.nodeType?.toUpperCase()}
              </text>
              <text
                x={10} y={36}
                fontSize={12}
                fill="#e2e8f0"
                fontFamily="ui-sans-serif,system-ui,sans-serif"
              >
                {label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

// ── Node detail card ──────────────────────────────────────────────────────────
function NodeDetail({
  node,
  graph,
  onClose,
}: {
  node: Node
  graph: Graph
  onClose: () => void
}) {
  const s = typeStyle(node.nodeType)
  const downstream = graph.edges
    ?.filter(e => e.leftNodeName === node.name)
    .flatMap(e => e.rightNodeNames) ?? []
  const upstream = graph.edges
    ?.filter(e => e.rightNodeNames.includes(node.name))
    .map(e => e.leftNodeName) ?? []

  return (
    <div className="absolute top-3 right-3 w-60 bg-slate-900 border border-slate-700 rounded-lg shadow-xl z-10 text-sm">
      <div className="flex items-center justify-between px-3 py-2.5 border-b border-slate-700">
        <span className="font-semibold text-slate-100 truncate">{node.name}</span>
        <button
          onClick={onClose}
          className="ml-2 shrink-0 text-slate-500 hover:text-slate-300 transition-colors"
        >
          <X size={13} />
        </button>
      </div>
      <div className="px-3 py-2.5 space-y-2.5">
        <span
          className="inline-block text-xs font-mono px-1.5 py-0.5 rounded border"
          style={{ background: s.fill, color: s.label, borderColor: s.stroke }}
        >
          {node.nodeType}
        </span>

        {node.description && (
          <p className="text-xs text-slate-400">{node.description}</p>
        )}

        {node.endpoint && (
          <div>
            <p className="text-xs text-slate-500 mb-0.5">Endpoint</p>
            <p className="text-xs font-mono text-blue-300 break-all">{node.endpoint}</p>
          </div>
        )}

        {upstream.length > 0 && (
          <div>
            <p className="text-xs text-slate-500 mb-1">Upstream ({upstream.length})</p>
            <div className="space-y-0.5">
              {upstream.map(u => (
                <p key={u} className="text-xs text-slate-300 font-mono truncate">{u}</p>
              ))}
            </div>
          </div>
        )}

        {downstream.length > 0 && (
          <div>
            <p className="text-xs text-slate-500 mb-1">Downstream ({downstream.length})</p>
            <div className="space-y-0.5">
              {downstream.map(d => (
                <p key={d} className="text-xs text-slate-300 font-mono truncate">{d}</p>
              ))}
            </div>
          </div>
        )}

        <p className="text-xs text-slate-700 font-mono pt-1 border-t border-slate-800 truncate">
          {node.id}
        </p>
      </div>
    </div>
  )
}

// ── Graph list item ───────────────────────────────────────────────────────────
function GraphItem({
  graph,
  selected,
  onClick,
}: {
  graph: Graph
  selected: boolean
  onClick: () => void
}) {
  const nodeCount = graph.nodes?.length ?? 0
  const edgeCount = graph.edges?.reduce((s, e) => s + e.rightNodeNames.length, 0) ?? 0

  return (
    <button
      onClick={onClick}
      className={[
        'w-full text-left px-4 py-3 border-b border-slate-800 transition-colors',
        selected
          ? 'bg-slate-800 border-r-2 border-r-blue-400'
          : 'hover:bg-slate-800/50',
      ].join(' ')}
    >
      <div className="flex items-center gap-2">
        <GitBranch size={13} className={selected ? 'text-blue-400 shrink-0' : 'text-slate-500 shrink-0'} />
        <span className={`text-sm font-medium truncate ${selected ? 'text-slate-100' : 'text-slate-300'}`}>
          {graph.name}
        </span>
      </div>
      {graph.description && (
        <p className="text-xs text-slate-500 mt-0.5 ml-5 truncate">{graph.description}</p>
      )}
      <div className="flex gap-3 mt-1 ml-5 text-xs text-slate-600">
        {nodeCount > 0 && <span>{nodeCount} node{nodeCount !== 1 ? 's' : ''}</span>}
        {edgeCount > 0 && <span>{edgeCount} edge{edgeCount !== 1 ? 's' : ''}</span>}
      </div>
    </button>
  )
}

// ── Panel ─────────────────────────────────────────────────────────────────────
export function DagsPanel() {
  const [page, setPage] = useState(0)
  const [selectedGraph, setSelectedGraph] = useState<Graph | null>(null)
  const [selectedNode, setSelectedNode] = useState<Node | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['graphs', page],
    queryFn: () => fetchGraphs(page, 50),
  })

  const graphs = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  function selectGraph(g: Graph) {
    setSelectedGraph(g)
    setSelectedNode(null)
  }

  return (
    <div className="flex h-full">
      {/* Left pane: graph list */}
      <div className="w-64 shrink-0 flex flex-col border-r border-slate-700 bg-slate-900">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700 shrink-0">
          <div>
            <h2 className="text-sm font-semibold text-slate-100">DAGs</h2>
            {data && (
              <p className="text-xs text-slate-500 mt-0.5">{data.totalElements} registered</p>
            )}
          </div>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-1.5 rounded text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors disabled:opacity-50"
            title="Refresh"
          >
            <RefreshCw size={13} className={isFetching ? 'animate-spin' : ''} />
          </button>
        </div>

        <div className="flex-1 overflow-auto">
          {isLoading && (
            <div className="flex items-center justify-center h-32 text-slate-500 text-xs gap-2">
              <RefreshCw size={12} className="animate-spin" />
              Loading…
            </div>
          )}
          {isError && (
            <div className="flex flex-col items-center justify-center h-32 gap-2 px-4 text-center">
              <AlertCircle size={16} className="text-red-400" />
              <p className="text-xs text-slate-500">{(error as Error).message}</p>
            </div>
          )}
          {!isLoading && !isError && graphs.length === 0 && (
            <div className="flex flex-col items-center justify-center h-32 px-4 text-center">
              <p className="text-xs text-slate-500">No graphs registered</p>
            </div>
          )}
          {graphs.map(g => (
            <GraphItem
              key={g.id}
              graph={g}
              selected={selectedGraph?.id === g.id}
              onClick={() => selectGraph(g)}
            />
          ))}
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 px-4 py-2 border-t border-slate-700 shrink-0">
            <button
              onClick={() => setPage(p => p - 1)}
              disabled={page === 0}
              className="px-2 py-1 text-xs rounded border border-slate-700 text-slate-400 hover:border-slate-500 disabled:opacity-30"
            >
              Prev
            </button>
            <span className="text-xs text-slate-500">{page + 1} / {totalPages}</span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-2 py-1 text-xs rounded border border-slate-700 text-slate-400 hover:border-slate-500 disabled:opacity-30"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {/* Right pane: canvas */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {!selectedGraph ? (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-center">
            <GitBranch size={32} className="text-slate-700" />
            <p className="text-slate-400 text-sm">Select a graph to visualize</p>
            <p className="text-slate-600 text-xs">Choose a DAG from the list on the left</p>
          </div>
        ) : (
          <>
            {/* Canvas header */}
            <div className="flex items-center gap-3 px-5 py-3 border-b border-slate-700 shrink-0">
              <GitBranch size={14} className="text-blue-400 shrink-0" />
              <div className="flex-1 min-w-0">
                <h2 className="text-sm font-semibold text-slate-100 truncate">{selectedGraph.name}</h2>
                {selectedGraph.description && (
                  <p className="text-xs text-slate-500 truncate">{selectedGraph.description}</p>
                )}
              </div>
              <div className="flex items-center gap-3 text-xs text-slate-500 shrink-0">
                {(selectedGraph.nodes?.length ?? 0) > 0 && (
                  <span>{selectedGraph.nodes!.length} nodes</span>
                )}
                {(selectedGraph.components?.length ?? 0) > 0 && (
                  <span className="flex items-center gap-1">
                    <Cpu size={11} />
                    {selectedGraph.components!.length} components
                  </span>
                )}
              </div>
            </div>

            {/* DAG canvas + node detail overlay */}
            <div className="flex-1 relative overflow-hidden flex flex-col">
              {(selectedGraph.nodes?.length ?? 0) === 0 ? (
                <div className="flex flex-col items-center justify-center flex-1 gap-2">
                  <p className="text-slate-500 text-sm">No nodes in this graph</p>
                </div>
              ) : (
                <DagSvg
                  graph={selectedGraph}
                  selectedNode={selectedNode}
                  onNodeClick={setSelectedNode}
                />
              )}

              {selectedNode && (
                <NodeDetail
                  node={selectedNode}
                  graph={selectedGraph}
                  onClose={() => setSelectedNode(null)}
                />
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

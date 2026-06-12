import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Network, RefreshCw, AlertCircle, GitBranch, ArrowUpDown, X, Wifi, WifiOff } from 'lucide-react'
import { fetchAgent } from '@/api/agents'
import type { Agent } from '@/types/ubiquia'

function timeAgo(iso?: string) {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  const days = Math.floor(diff / 86_400_000)
  if (days > 0) return `${days}d ago`
  const hours = Math.floor(diff / 3_600_000)
  if (hours > 0) return `${hours}h ago`
  const mins = Math.floor(diff / 60_000)
  return mins > 0 ? `${mins}m ago` : 'just now'
}

function StatusDot({ reachable }: { reachable: boolean }) {
  return (
    <span
      className={[
        'inline-block w-2 h-2 rounded-full shrink-0',
        reachable ? 'bg-green-400' : 'bg-red-500',
      ].join(' ')}
    />
  )
}

function AgentCard({
  agent,
  isLocal,
  selected,
  onClick,
}: {
  agent: Agent
  isLocal: boolean
  selected: boolean
  onClick: () => void
}) {
  const graphCount = agent.deployedGraphs?.length ?? 0
  const pendingUpdates = agent.updates?.length ?? 0
  const pendingSyncs = agent.syncs?.length ?? 0

  return (
    <button
      onClick={onClick}
      className={[
        'w-full text-left p-4 rounded-lg border transition-colors',
        selected
          ? 'bg-slate-800 border-blue-500'
          : 'bg-slate-800/50 border-slate-700 hover:border-slate-500',
      ].join(' ')}
    >
      <div className="flex items-start gap-3">
        <StatusDot reachable={agent.reachable} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-semibold text-slate-100 font-mono truncate">
              {agent.baseUrl ?? agent.id}
            </span>
            {isLocal && (
              <span className="text-xs px-1.5 py-0.5 rounded bg-blue-900/60 text-blue-300 border border-blue-700 shrink-0">
                local
              </span>
            )}
            <span
              className={[
                'text-xs px-1.5 py-0.5 rounded shrink-0',
                agent.reachable
                  ? 'bg-green-900/40 text-green-300 border border-green-800'
                  : 'bg-red-900/40 text-red-300 border border-red-800',
              ].join(' ')}
            >
              {agent.reachable ? 'online' : 'offline'}
            </span>
          </div>

          <div className="flex flex-wrap gap-3 mt-2 text-xs text-slate-500">
            {graphCount > 0 && (
              <span className="flex items-center gap-1">
                <GitBranch size={11} />
                {graphCount} graph{graphCount !== 1 ? 's' : ''}
              </span>
            )}
            {pendingUpdates > 0 && (
              <span className="flex items-center gap-1 text-amber-500">
                <ArrowUpDown size={11} />
                {pendingUpdates} update{pendingUpdates !== 1 ? 's' : ''}
              </span>
            )}
            {pendingSyncs > 0 && (
              <span className="flex items-center gap-1 text-purple-400">
                <RefreshCw size={11} />
                {pendingSyncs} sync{pendingSyncs !== 1 ? 's' : ''}
              </span>
            )}
            {graphCount === 0 && pendingUpdates === 0 && pendingSyncs === 0 && (
              <span>idle</span>
            )}
          </div>
        </div>
      </div>
    </button>
  )
}

function AgentDetail({ agent, isLocal, onClose }: { agent: Agent; isLocal: boolean; onClose: () => void }) {
  const graphs = agent.deployedGraphs ?? []
  const updates = agent.updates ?? []
  const syncs = agent.syncs ?? []

  return (
    <div className="w-72 shrink-0 flex flex-col border-l border-slate-700 bg-slate-900 overflow-auto">
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700 shrink-0">
        <div className="flex items-center gap-2 min-w-0">
          <StatusDot reachable={agent.reachable} />
          <span className="text-sm font-semibold text-slate-100 truncate">
            {isLocal ? 'Local Agent' : 'Agent Detail'}
          </span>
        </div>
        <button
          onClick={onClose}
          className="ml-2 shrink-0 text-slate-500 hover:text-slate-300 transition-colors"
        >
          <X size={13} />
        </button>
      </div>

      <div className="flex-1 overflow-auto px-4 py-3 space-y-4 text-sm">
        {/* Base URL */}
        {agent.baseUrl && (
          <div>
            <p className="text-xs text-slate-500 mb-1">Base URL</p>
            <p className="text-xs font-mono text-blue-300 break-all">{agent.baseUrl}</p>
          </div>
        )}

        {/* Timestamps */}
        <div className="flex gap-4 text-xs text-slate-500">
          <span>Joined {timeAgo(agent.createdAt)}</span>
          {agent.updatedAt && <span>Updated {timeAgo(agent.updatedAt)}</span>}
        </div>

        {/* Deployed graphs */}
        <div>
          <p className="text-xs text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
            <GitBranch size={11} />
            Deployed Graphs ({graphs.length})
          </p>
          {graphs.length === 0 ? (
            <p className="text-xs text-slate-600">None</p>
          ) : (
            <div className="space-y-1.5">
              {graphs.map(g => (
                <div key={g.id} className="px-2 py-1.5 rounded bg-slate-800 border border-slate-700">
                  <p className="text-xs text-slate-200 font-medium truncate">{g.name}</p>
                  {g.description && (
                    <p className="text-xs text-slate-500 truncate mt-0.5">{g.description}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pending updates */}
        {updates.length > 0 && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
              <ArrowUpDown size={11} />
              Pending Updates ({updates.length})
            </p>
            <div className="space-y-1.5">
              {updates.map(u => (
                <div key={u.id} className="px-2 py-1.5 rounded bg-amber-950/30 border border-amber-800/40">
                  {u.updateReason && (
                    <p className="text-xs text-amber-300">{u.updateReason}</p>
                  )}
                  <p className="text-xs text-slate-600 font-mono mt-0.5">{timeAgo(u.createdAt)}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Pending syncs */}
        {syncs.length > 0 && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
              <RefreshCw size={11} />
              Pending Syncs ({syncs.length})
            </p>
            <div className="space-y-1.5">
              {syncs.map(s => (
                <div key={s.id} className="px-2 py-1.5 rounded bg-purple-950/30 border border-purple-800/40">
                  <p className="text-xs text-slate-600 font-mono">{timeAgo(s.createdAt)}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ID footer */}
        <p className="text-xs text-slate-700 font-mono pt-1 border-t border-slate-800 truncate">{agent.id}</p>
      </div>
    </div>
  )
}

export function AgentNetworkPanel() {
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data: localAgent, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['agent'],
    queryFn: fetchAgent,
    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const allAgents: Agent[] = localAgent?.network?.agents ?? (localAgent ? [localAgent] : [])
  const selectedAgent = allAgents.find(a => a.id === selectedId) ?? null
  const isSelectedLocal = selectedAgent?.id === localAgent?.id

  const onlineCount = allAgents.filter(a => a.reachable).length
  const offlineCount = allAgents.length - onlineCount

  return (
    <div className="flex h-full">
      {/* Main pane */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
          <div>
            <h1 className="text-lg font-semibold text-slate-100">Agent Network</h1>
            {!isLoading && !isError && allAgents.length > 0 && (
              <div className="flex items-center gap-3 mt-0.5 text-xs text-slate-500">
                <span className="flex items-center gap-1">
                  <Wifi size={11} className="text-green-400" />
                  {onlineCount} online
                </span>
                {offlineCount > 0 && (
                  <span className="flex items-center gap-1 text-red-400">
                    <WifiOff size={11} />
                    {offlineCount} offline
                  </span>
                )}
              </div>
            )}
          </div>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 rounded text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors disabled:opacity-50"
            title="Refresh"
          >
            <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-auto px-6 py-4">
          {isLoading && (
            <div className="flex items-center justify-center h-48 text-slate-500 text-sm gap-2">
              <RefreshCw size={14} className="animate-spin" />
              Loading agent network…
            </div>
          )}

          {isError && (
            <div className="flex flex-col items-center justify-center h-48 gap-3 text-center">
              <AlertCircle size={24} className="text-red-400" />
              <p className="text-slate-400 text-sm">Failed to load agent</p>
              <p className="text-slate-600 text-xs max-w-sm">{(error as Error).message}</p>
              <p className="text-slate-600 text-xs">Is the Ubiquia backend running?</p>
            </div>
          )}

          {!isLoading && !isError && allAgents.length === 0 && (
            <div className="flex flex-col items-center justify-center h-48 gap-3 text-center">
              <Network size={32} className="text-slate-700" />
              <p className="text-slate-400 text-sm">No agents found</p>
            </div>
          )}

          {allAgents.length > 0 && (
            <div className="grid gap-3 grid-cols-1 xl:grid-cols-2">
              {allAgents.map(agent => (
                <AgentCard
                  key={agent.id}
                  agent={agent}
                  isLocal={agent.id === localAgent?.id}
                  selected={selectedId === agent.id}
                  onClick={() => setSelectedId(prev => prev === agent.id ? null : agent.id)}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Detail pane */}
      {selectedAgent && (
        <AgentDetail
          agent={selectedAgent}
          isLocal={isSelectedLocal}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  )
}

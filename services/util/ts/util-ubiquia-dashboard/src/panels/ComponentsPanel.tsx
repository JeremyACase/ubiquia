import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Cpu, RefreshCw, AlertCircle, GitBranch, X, Globe, Box } from 'lucide-react'
import { fetchComponents } from '@/api/components'
import type { Component } from '@/types/ubiquia'

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

const TYPE_STYLE: Record<string, { bg: string; text: string; border: string }> = {
  POD:      { bg: 'bg-green-900/40',  text: 'text-green-300',  border: 'border-green-800' },
  TEMPLATE: { bg: 'bg-blue-900/40',   text: 'text-blue-300',   border: 'border-blue-800' },
  NONE:     { bg: 'bg-slate-800',     text: 'text-slate-400',  border: 'border-slate-700' },
}
function typeStyle(t: string) {
  return TYPE_STYLE[t.toUpperCase()] ?? TYPE_STYLE.NONE
}

function imageStr(c: Component) {
  const { registry, repository, tag } = c.image
  return `${registry}/${repository}:${tag}`
}

function ComponentRow({
  component,
  selected,
  onClick,
}: {
  component: Component
  selected: boolean
  onClick: () => void
}) {
  const ts = typeStyle(component.componentType)

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
        <Cpu size={13} className={selected ? 'text-blue-400 shrink-0' : 'text-slate-500 shrink-0'} />
        <span className={`text-sm font-medium truncate ${selected ? 'text-slate-100' : 'text-slate-300'}`}>
          {component.name}
        </span>
        <span className={`ml-auto text-xs px-1.5 py-0.5 rounded border shrink-0 ${ts.bg} ${ts.text} ${ts.border}`}>
          {component.componentType}
        </span>
      </div>

      <div className="mt-1.5 ml-5 space-y-0.5">
        <p className="text-xs text-slate-500 font-mono truncate">{imageStr(component)}</p>
        <div className="flex gap-3 text-xs text-slate-600">
          <span>:{component.port}</span>
          {component.graph && (
            <span className="flex items-center gap-1">
              <GitBranch size={10} />
              {component.graph.name}
            </span>
          )}
          {component.exposeService && (
            <span className="flex items-center gap-1 text-blue-500">
              <Globe size={10} />
              exposed
            </span>
          )}
        </div>
      </div>
    </button>
  )
}

function ComponentDetail({
  component,
  onClose,
}: {
  component: Component
  onClose: () => void
}) {
  const ts = typeStyle(component.componentType)

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-3 border-b border-slate-700 shrink-0">
        <div className="flex items-center gap-2 min-w-0">
          <Cpu size={14} className="text-blue-400 shrink-0" />
          <span className="text-sm font-semibold text-slate-100 truncate">{component.name}</span>
          <span className={`text-xs px-1.5 py-0.5 rounded border shrink-0 ${ts.bg} ${ts.text} ${ts.border}`}>
            {component.componentType}
          </span>
        </div>
        <button
          onClick={onClose}
          className="ml-2 shrink-0 text-slate-500 hover:text-slate-300 transition-colors"
        >
          <X size={13} />
        </button>
      </div>

      <div className="flex-1 overflow-auto px-5 py-4 space-y-5 text-sm">
        {/* Description */}
        {component.description && (
          <p className="text-slate-400 text-sm">{component.description}</p>
        )}

        {/* Image */}
        <div>
          <p className="text-xs text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
            <Box size={11} /> Container Image
          </p>
          <div className="rounded bg-slate-800 border border-slate-700 divide-y divide-slate-700">
            {[
              ['Registry', component.image.registry],
              ['Repository', component.image.repository],
              ['Tag', component.image.tag],
            ].map(([label, value]) => (
              <div key={label} className="flex items-center px-3 py-2 gap-3">
                <span className="text-xs text-slate-500 w-20 shrink-0">{label}</span>
                <span className="text-xs text-slate-200 font-mono truncate">{value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Port + exposure */}
        <div className="flex gap-4 text-xs">
          <div>
            <p className="text-slate-500 mb-1">Port</p>
            <p className="font-mono text-slate-200">{component.port}</p>
          </div>
          <div>
            <p className="text-slate-500 mb-1">Expose Service</p>
            <p className={component.exposeService ? 'text-green-400' : 'text-slate-500'}>
              {component.exposeService ? 'Yes' : 'No'}
            </p>
          </div>
          {component.communicationServiceSettings?.exposeViaCommService && (
            <div>
              <p className="text-slate-500 mb-1">Comm Service Endpoint</p>
              <p className="font-mono text-blue-300 break-all">
                {component.communicationServiceSettings.proxiedEndpoint}
              </p>
            </div>
          )}
        </div>

        {/* Graph / Node */}
        {(component.graph || component.node) && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
              <GitBranch size={11} /> Topology
            </p>
            <div className="space-y-1.5">
              {component.graph && (
                <div className="flex items-center gap-2 px-3 py-2 rounded bg-slate-800 border border-slate-700">
                  <span className="text-xs text-slate-500 w-12 shrink-0">Graph</span>
                  <span className="text-xs text-slate-200 font-medium truncate">{component.graph.name}</span>
                </div>
              )}
              {component.node && (
                <div className="flex items-center gap-2 px-3 py-2 rounded bg-slate-800 border border-slate-700">
                  <span className="text-xs text-slate-500 w-12 shrink-0">Node</span>
                  <span className="text-xs text-slate-200 font-medium truncate">{component.node.name}</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Config */}
        {component.config && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2">Config Mount</p>
            <p className="text-xs font-mono text-slate-400">{component.config.configMountPath}</p>
          </div>
        )}

        {/* Post-start commands */}
        {(component.postStartExecCommands?.length ?? 0) > 0 && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2">Post-start Commands</p>
            <div className="space-y-1">
              {component.postStartExecCommands!.map((cmd, i) => (
                <p key={i} className="text-xs font-mono text-slate-300 bg-slate-800 rounded px-2 py-1">{cmd}</p>
              ))}
            </div>
          </div>
        )}

        {/* Environment variables */}
        {(component.environmentVariables?.length ?? 0) > 0 && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2">Environment Variables</p>
            <div className="rounded bg-slate-800 border border-slate-700 divide-y divide-slate-700">
              {component.environmentVariables!.map(ev => (
                <div key={ev.name} className="flex items-center px-3 py-1.5 gap-3">
                  <span className="text-xs font-mono text-slate-400 shrink-0">{ev.name}</span>
                  <span className="text-xs font-mono text-slate-300 truncate">{ev.value}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tags */}
        {(component.tags?.length ?? 0) > 0 && (
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider mb-2">Tags</p>
            <div className="flex flex-wrap gap-1.5">
              {component.tags!.map(t => (
                <span key={`${t.key}:${t.value}`} className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300 border border-slate-600">
                  {t.key}: {t.value}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="flex gap-4 text-xs text-slate-600 font-mono pt-2 border-t border-slate-800">
          <span>ID: {component.id}</span>
          {component.createdAt && <span>Created {timeAgo(component.createdAt)}</span>}
        </div>
      </div>
    </div>
  )
}

export function ComponentsPanel() {
  const [page, setPage] = useState(0)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['components', page],
    queryFn: () => fetchComponents(page, 20),
  })

  const components = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const selectedComponent = components.find(c => c.id === selectedId) ?? null

  return (
    <div className="flex h-full">
      {/* Left pane: list */}
      <div className="w-72 shrink-0 flex flex-col border-r border-slate-700 bg-slate-900">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700 shrink-0">
          <div>
            <h2 className="text-sm font-semibold text-slate-100">Components</h2>
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
              <RefreshCw size={12} className="animate-spin" /> Loading…
            </div>
          )}
          {isError && (
            <div className="flex flex-col items-center justify-center h-32 gap-2 px-4 text-center">
              <AlertCircle size={16} className="text-red-400" />
              <p className="text-xs text-slate-500">{(error as Error).message}</p>
            </div>
          )}
          {!isLoading && !isError && components.length === 0 && (
            <div className="flex flex-col items-center justify-center h-32 px-4 text-center">
              <p className="text-xs text-slate-500">No components registered</p>
            </div>
          )}
          {components.map(c => (
            <ComponentRow
              key={c.id}
              component={c}
              selected={selectedId === c.id}
              onClick={() => setSelectedId(prev => prev === c.id ? null : c.id)}
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

      {/* Right pane: detail */}
      {selectedComponent ? (
        <ComponentDetail
          component={selectedComponent}
          onClose={() => setSelectedId(null)}
        />
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center gap-3 text-center">
          <Cpu size={32} className="text-slate-700" />
          <p className="text-slate-400 text-sm">Select a component to inspect</p>
          <p className="text-slate-600 text-xs">Choose from the list on the left</p>
        </div>
      )}
    </div>
  )
}

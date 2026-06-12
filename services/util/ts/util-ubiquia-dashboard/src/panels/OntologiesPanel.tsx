import { useState, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Upload, Trash2, ChevronDown, ChevronRight, GitBranch, Tag, RefreshCw, AlertCircle } from 'lucide-react'
import { fetchOntologies, uploadOntology, deleteOntology } from '@/api/ontologies'
import type { DomainOntology } from '@/types/ubiquia'

function versionStr(v: DomainOntology['version']) {
  return `v${v.major}.${v.minor}.${v.patch}`
}

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

function SchemaViewer({ schema }: { schema: Record<string, unknown> }) {
  return (
    <pre className="bg-slate-950 rounded p-3 text-xs text-green-400 font-mono overflow-auto max-h-64 border border-slate-700">
      {JSON.stringify(schema, null, 2)}
    </pre>
  )
}

function OntologyRow({ ontology, onDelete }: { ontology: DomainOntology; onDelete: (id: string) => void }) {
  const [expanded, setExpanded] = useState(false)
  const [showSchema, setShowSchema] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const graphCount = ontology.graphs?.length ?? 0
  const nodeCount = ontology.graphs?.reduce((sum, g) => sum + (g.nodes?.length ?? 0), 0) ?? 0
  const schemaDefinitions = ontology.domainDataContract?.schema
    ? Object.keys((ontology.domainDataContract.schema as { definitions?: Record<string, unknown> }).definitions ?? {})
    : []

  return (
    <div className="border border-slate-700 rounded-lg bg-slate-800/50 hover:border-slate-600 transition-colors">
      {/* Header row */}
      <div
        className="flex items-center gap-3 px-4 py-3 cursor-pointer select-none"
        onClick={() => setExpanded((e) => !e)}
      >
        <span className="text-slate-500 w-4">
          {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </span>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-slate-100 text-sm">{ontology.name}</span>
            <span className="text-xs font-mono px-1.5 py-0.5 rounded bg-blue-900/50 text-blue-300 border border-blue-800">
              {versionStr(ontology.version)}
            </span>
            {ontology.author && (
              <span className="text-xs text-slate-500">{ontology.author}</span>
            )}
          </div>
          {ontology.description && (
            <p className="text-xs text-slate-400 mt-0.5 truncate">{ontology.description}</p>
          )}
        </div>

        <div className="flex items-center gap-3 shrink-0 text-xs text-slate-500">
          {graphCount > 0 && (
            <span className="flex items-center gap-1">
              <GitBranch size={12} />
              {graphCount} graph{graphCount !== 1 ? 's' : ''}
            </span>
          )}
          {nodeCount > 0 && (
            <span>{nodeCount} node{nodeCount !== 1 ? 's' : ''}</span>
          )}
          <span>{timeAgo(ontology.updatedAt ?? ontology.createdAt)}</span>
        </div>

        <button
          onClick={(e) => { e.stopPropagation(); setConfirmDelete(true) }}
          className="p-1.5 rounded text-slate-600 hover:text-red-400 hover:bg-red-900/20 transition-colors"
          title="Delete ontology"
        >
          <Trash2 size={13} />
        </button>
      </div>

      {/* Delete confirmation */}
      {confirmDelete && (
        <div className="mx-4 mb-3 p-3 rounded bg-red-950/50 border border-red-800 flex items-center justify-between gap-3">
          <span className="text-sm text-red-300">Delete <strong>{ontology.name}</strong>?</span>
          <div className="flex gap-2">
            <button
              onClick={() => setConfirmDelete(false)}
              className="px-3 py-1 text-xs rounded border border-slate-600 text-slate-400 hover:border-slate-400"
            >
              Cancel
            </button>
            <button
              onClick={() => { onDelete(ontology.id); setConfirmDelete(false) }}
              className="px-3 py-1 text-xs rounded bg-red-700 hover:bg-red-600 text-white"
            >
              Delete
            </button>
          </div>
        </div>
      )}

      {/* Expanded detail */}
      {expanded && (
        <div className="border-t border-slate-700 px-4 py-3 space-y-4">
          {/* Tags */}
          {(ontology.tags?.length ?? 0) > 0 && (
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1.5 flex items-center gap-1">
                <Tag size={11} /> Tags
              </p>
              <div className="flex flex-wrap gap-1.5">
                {ontology.tags!.map((t) => (
                  <span
                    key={`${t.key}:${t.value}`}
                    className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300 border border-slate-600"
                  >
                    {t.key}: {t.value}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Schema types */}
          {schemaDefinitions.length > 0 && (
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1.5">DDC Types</p>
              <div className="flex flex-wrap gap-1.5">
                {schemaDefinitions.map((def) => (
                  <span key={def} className="text-xs px-2 py-0.5 rounded font-mono bg-purple-900/30 text-purple-300 border border-purple-800/50">
                    {def}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Graphs */}
          {(ontology.graphs?.length ?? 0) > 0 && (
            <div>
              <p className="text-xs text-slate-500 uppercase tracking-wider mb-1.5 flex items-center gap-1">
                <GitBranch size={11} /> Graphs
              </p>
              <div className="space-y-1.5">
                {ontology.graphs!.map((g) => (
                  <div key={g.id} className="flex items-start gap-2 p-2 rounded bg-slate-900/50 border border-slate-700">
                    <div className="flex-1 min-w-0">
                      <span className="text-sm text-slate-200 font-medium">{g.name}</span>
                      {g.description && (
                        <p className="text-xs text-slate-400 mt-0.5">{g.description}</p>
                      )}
                    </div>
                    <div className="flex gap-2 text-xs text-slate-500 shrink-0 pt-0.5">
                      {(g.nodes?.length ?? 0) > 0 && <span>{g.nodes!.length} nodes</span>}
                      {(g.components?.length ?? 0) > 0 && <span>{g.components!.length} components</span>}
                      {(g.edges?.length ?? 0) > 0 && <span>{g.edges!.length} edges</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Raw DDC schema toggle */}
          {ontology.domainDataContract?.schema && (
            <div>
              <button
                onClick={() => setShowSchema((s) => !s)}
                className="text-xs text-slate-500 hover:text-slate-300 flex items-center gap-1 transition-colors"
              >
                {showSchema ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                Raw DDC Schema
              </button>
              {showSchema && (
                <div className="mt-2">
                  <SchemaViewer schema={ontology.domainDataContract.schema as Record<string, unknown>} />
                </div>
              )}
            </div>
          )}

          {/* Metadata footer */}
          <div className="flex gap-4 text-xs text-slate-600 font-mono">
            <span>ID: {ontology.id}</span>
            {ontology.createdAt && <span>Created: {new Date(ontology.createdAt).toLocaleDateString()}</span>}
          </div>
        </div>
      )}
    </div>
  )
}

export function OntologiesPanel() {
  const [page, setPage] = useState(0)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const queryClient = useQueryClient()

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['ontologies', page],
    queryFn: () => fetchOntologies(page, 20),
  })

  const uploadMutation = useMutation({
    mutationFn: uploadOntology,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ontologies'] }),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteOntology,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ontologies'] }),
  })

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) uploadMutation.mutate(file)
    e.target.value = ''
  }

  const ontologies = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
        <div>
          <h1 className="text-lg font-semibold text-slate-100">Domain Ontologies</h1>
          {totalElements > 0 && (
            <p className="text-xs text-slate-500 mt-0.5">{totalElements} registered</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 rounded text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors disabled:opacity-50"
            title="Refresh"
          >
            <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".yaml,.yml"
            className="hidden"
            onChange={handleFileChange}
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isPending}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded bg-blue-700 hover:bg-blue-600 text-white transition-colors disabled:opacity-50"
          >
            <Upload size={13} />
            {uploadMutation.isPending ? 'Registering…' : 'Register Ontology'}
          </button>
        </div>
      </div>

      {/* Upload feedback */}
      {uploadMutation.isError && (
        <div className="mx-6 mt-4 p-3 rounded bg-red-950/50 border border-red-800 flex items-center gap-2 text-sm text-red-300 shrink-0">
          <AlertCircle size={14} />
          {(uploadMutation.error as Error).message}
        </div>
      )}
      {uploadMutation.isSuccess && (
        <div className="mx-6 mt-4 p-3 rounded bg-green-950/50 border border-green-800 text-sm text-green-300 shrink-0">
          Ontology registered: <span className="font-mono text-xs">{uploadMutation.data.id}</span>
        </div>
      )}

      {/* Body */}
      <div className="flex-1 overflow-auto px-6 py-4">
        {isLoading && (
          <div className="flex items-center justify-center h-48 text-slate-500 text-sm gap-2">
            <RefreshCw size={14} className="animate-spin" />
            Loading ontologies…
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-center justify-center h-48 gap-3 text-center">
            <AlertCircle size={24} className="text-red-400" />
            <p className="text-slate-400 text-sm">Failed to load ontologies</p>
            <p className="text-slate-600 text-xs max-w-sm">{(error as Error).message}</p>
            <p className="text-slate-600 text-xs">Is the Ubiquia backend running?</p>
          </div>
        )}

        {!isLoading && !isError && ontologies.length === 0 && (
          <div className="flex flex-col items-center justify-center h-48 gap-3 text-center">
            <p className="text-slate-400 text-sm">No ontologies registered</p>
            <p className="text-slate-600 text-xs">Upload a YAML ontology file to get started.</p>
          </div>
        )}

        {ontologies.length > 0 && (
          <div className="space-y-2">
            {ontologies.map((o) => (
              <OntologyRow
                key={o.id}
                ontology={o}
                onDelete={(id) => deleteMutation.mutate(id)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 px-6 py-3 border-t border-slate-700 shrink-0">
          <button
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
            className="px-3 py-1 text-xs rounded border border-slate-700 text-slate-400 hover:border-slate-500 disabled:opacity-30"
          >
            Prev
          </button>
          <span className="text-xs text-slate-500">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 text-xs rounded border border-slate-700 text-slate-400 hover:border-slate-500 disabled:opacity-30"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

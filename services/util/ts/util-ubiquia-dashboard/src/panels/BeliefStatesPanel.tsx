import { useState, useCallback } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  Database, Play, Trash2, AlertCircle, CheckCircle, ChevronDown, ChevronRight,
  RefreshCw, Activity, Link, Clock, XCircle,
} from 'lucide-react'
import { generateBeliefState, teardownBeliefState, fetchProxiedUrls, probeHealth } from '@/api/beliefStates'
import type { BeliefStateGeneration, SemanticVersion } from '@/types/ubiquia'

// ── Local registry (localStorage-backed) ─────────────────────────────────────

interface BeliefStateRecord {
  id: string
  domainName: string
  version: SemanticVersion
  status: 'active' | 'torn_down'
  generatedAt: string
  tornDownAt?: string
}

const STORAGE_KEY = 'ubiquia-belief-states'

function loadRegistry(): BeliefStateRecord[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

function saveRegistry(records: BeliefStateRecord[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records))
}

function recordId(domainName: string, v: SemanticVersion) {
  return `${domainName}:${v.major}.${v.minor}.${v.patch}`
}

function useBeliefStateRegistry() {
  const [records, setRecords] = useState<BeliefStateRecord[]>(loadRegistry)

  const update = useCallback((next: BeliefStateRecord[]) => {
    setRecords(next)
    saveRegistry(next)
  }, [])

  function addGenerated(payload: BeliefStateGeneration) {
    const id = recordId(payload.domainName, payload.version)
    const existing = records.find(r => r.id === id)
    const entry: BeliefStateRecord = existing
      ? { ...existing, status: 'active', tornDownAt: undefined, generatedAt: new Date().toISOString() }
      : { id, domainName: payload.domainName, version: payload.version, status: 'active', generatedAt: new Date().toISOString() }
    update([entry, ...records.filter(r => r.id !== id)])
  }

  function markTornDown(payload: BeliefStateGeneration) {
    const id = recordId(payload.domainName, payload.version)
    update(records.map(r => r.id === id ? { ...r, status: 'torn_down', tornDownAt: new Date().toISOString() } : r))
  }

  function remove(id: string) {
    update(records.filter(r => r.id !== id))
  }

  return { records, addGenerated, markTornDown, remove }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function versionStr(v: SemanticVersion) {
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

function matchedProxiedUrl(domainName: string, urls: string[]): string | undefined {
  const lower = domainName.toLowerCase()
  return urls.find(u => u.toLowerCase().includes(lower))
}

// ── Health probe ──────────────────────────────────────────────────────────────

type HealthState = 'idle' | 'checking' | 'up' | 'down'

function HealthBadge({ state }: { state: HealthState }) {
  if (state === 'idle') return null
  if (state === 'checking') return (
    <span className="flex items-center gap-1 text-xs text-slate-400">
      <RefreshCw size={11} className="animate-spin" /> checking…
    </span>
  )
  if (state === 'up') return (
    <span className="flex items-center gap-1 text-xs text-green-400">
      <CheckCircle size={11} /> UP
    </span>
  )
  return (
    <span className="flex items-center gap-1 text-xs text-red-400">
      <XCircle size={11} /> DOWN
    </span>
  )
}

// ── Belief state card ─────────────────────────────────────────────────────────

function BeliefStateCard({
  record,
  proxiedUrls,
  onTeardown,
  onRemove,
}: {
  record: BeliefStateRecord
  proxiedUrls: string[]
  onTeardown: (r: BeliefStateRecord) => void
  onRemove: (id: string) => void
}) {
  const isActive = record.status === 'active'
  const matchedUrl = matchedProxiedUrl(record.domainName, proxiedUrls)
  const [healthState, setHealthState] = useState<HealthState>('idle')

  async function checkHealth() {
    if (!matchedUrl) return
    setHealthState('checking')
    try {
      const result = await probeHealth(matchedUrl)
      setHealthState(result?.status?.toUpperCase() === 'UP' ? 'up' : 'down')
    } catch {
      setHealthState('down')
    }
  }

  return (
    <div className={[
      'rounded-lg border p-4 transition-colors',
      isActive ? 'bg-slate-800/50 border-slate-700' : 'bg-slate-900/30 border-slate-800',
    ].join(' ')}>
      {/* Header row */}
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className={`text-sm font-semibold truncate ${isActive ? 'text-slate-100' : 'text-slate-500'}`}>
              {record.domainName}
            </span>
            <span className={[
              'text-xs font-mono px-1.5 py-0.5 rounded border',
              isActive
                ? 'bg-blue-900/40 text-blue-300 border-blue-800'
                : 'bg-slate-800 text-slate-500 border-slate-700',
            ].join(' ')}>
              {versionStr(record.version)}
            </span>
            <span className={[
              'text-xs px-1.5 py-0.5 rounded',
              isActive ? 'bg-green-900/30 text-green-400' : 'bg-slate-800 text-slate-600',
            ].join(' ')}>
              {isActive ? 'active' : 'torn down'}
            </span>
          </div>

          {/* Timestamps */}
          <div className="flex flex-wrap gap-3 mt-1.5 text-xs text-slate-600">
            <span className="flex items-center gap-1">
              <Clock size={10} />
              Generated {timeAgo(record.generatedAt)}
            </span>
            {record.tornDownAt && (
              <span className="flex items-center gap-1 text-slate-700">
                <Trash2 size={10} />
                Torn down {timeAgo(record.tornDownAt)}
              </span>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1 shrink-0">
          {isActive && (
            <button
              onClick={() => onTeardown(record)}
              className="p-1.5 rounded text-slate-600 hover:text-red-400 hover:bg-red-900/20 transition-colors"
              title="Tear down"
            >
              <Trash2 size={13} />
            </button>
          )}
          <button
            onClick={() => onRemove(record.id)}
            className="p-1.5 rounded text-slate-700 hover:text-slate-400 hover:bg-slate-800 transition-colors"
            title="Remove from registry"
          >
            <XCircle size={13} />
          </button>
        </div>
      </div>

      {/* Proxied endpoint row */}
      {isActive && (
        <div className="mt-3 pt-3 border-t border-slate-800 flex items-center gap-3">
          {matchedUrl ? (
            <>
              <Link size={11} className="text-slate-500 shrink-0" />
              <span className="text-xs font-mono text-blue-400 truncate flex-1">{matchedUrl}</span>
              <HealthBadge state={healthState} />
              <button
                onClick={checkHealth}
                disabled={healthState === 'checking'}
                className="flex items-center gap-1 text-xs text-slate-500 hover:text-slate-300 transition-colors disabled:opacity-50 shrink-0"
              >
                <Activity size={11} />
                Health
              </button>
            </>
          ) : (
            <span className="text-xs text-slate-700 italic flex items-center gap-1">
              <Link size={11} />
              Not found in proxied endpoints
            </span>
          )}
        </div>
      )}
    </div>
  )
}

// ── Version input ─────────────────────────────────────────────────────────────

interface VersionFields { major: string; minor: string; patch: string }
const EMPTY_VERSION: VersionFields = { major: '0', minor: '0', patch: '0' }
function parseVersion(v: VersionFields): SemanticVersion {
  return { major: parseInt(v.major, 10) || 0, minor: parseInt(v.minor, 10) || 0, patch: parseInt(v.patch, 10) || 0 }
}

function VersionInput({ value, onChange }: { value: VersionFields; onChange: (v: VersionFields) => void }) {
  function field(k: keyof VersionFields) {
    return (
      <input
        type="number" min={0} value={value[k]}
        onChange={e => onChange({ ...value, [k]: e.target.value })}
        className="w-14 bg-slate-800 border border-slate-600 rounded px-2 py-1.5 text-sm text-slate-200 font-mono text-center focus:outline-none focus:border-blue-500"
      />
    )
  }
  return (
    <div className="flex items-center gap-1 text-slate-500 text-sm">
      {field('major')}<span>.</span>{field('minor')}<span>.</span>{field('patch')}
    </div>
  )
}

// ── Action card ───────────────────────────────────────────────────────────────

function ActionCard({
  title, description, icon, actionLabel, actionClass, mutation, onSuccess,
}: {
  title: string
  description: string
  icon: React.ReactNode
  actionLabel: string
  actionClass: string
  mutation: ReturnType<typeof useMutation<BeliefStateGeneration, Error, BeliefStateGeneration>>
  onSuccess: (result: BeliefStateGeneration) => void
}) {
  const [domainName, setDomainName] = useState('')
  const [version, setVersion] = useState<VersionFields>(EMPTY_VERSION)
  const [open, setOpen] = useState(true)

  function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!domainName.trim()) return
    const payload: BeliefStateGeneration = { domainName: domainName.trim(), version: parseVersion(version) }
    mutation.mutate(payload, {
      onSuccess: (data) => { onSuccess(data); setDomainName(''); setVersion(EMPTY_VERSION) },
    })
  }

  return (
    <div className="rounded-lg border border-slate-700 bg-slate-800/50">
      <button onClick={() => setOpen(o => !o)} className="w-full flex items-center gap-3 px-4 py-3 text-left">
        <span className="text-slate-400">{icon}</span>
        <div className="flex-1 min-w-0">
          <span className="text-sm font-semibold text-slate-100">{title}</span>
          <p className="text-xs text-slate-500 mt-0.5">{description}</p>
        </div>
        <span className="text-slate-600 shrink-0">{open ? <ChevronDown size={14} /> : <ChevronRight size={14} />}</span>
      </button>

      {open && (
        <form onSubmit={submit} className="border-t border-slate-700 px-4 py-4 space-y-4">
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">Domain Name</label>
            <input
              type="text" value={domainName} onChange={e => setDomainName(e.target.value)}
              placeholder="e.g. pets"
              className="w-full bg-slate-800 border border-slate-600 rounded px-3 py-1.5 text-sm text-slate-200 placeholder:text-slate-600 focus:outline-none focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1.5">Version (major . minor . patch)</label>
            <VersionInput value={version} onChange={setVersion} />
          </div>
          {mutation.isError && (
            <div className="flex items-start gap-2 p-2.5 rounded bg-red-950/50 border border-red-800 text-xs text-red-300">
              <AlertCircle size={13} className="mt-0.5 shrink-0" />
              {(mutation.error as Error).message}
            </div>
          )}
          <button
            type="submit" disabled={mutation.isPending || !domainName.trim()}
            className={`flex items-center gap-2 px-4 py-2 rounded text-sm font-medium text-white transition-colors disabled:opacity-50 ${actionClass}`}
          >
            {mutation.isPending
              ? <><span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />Working…</>
              : actionLabel}
          </button>
        </form>
      )}
    </div>
  )
}

// ── Panel ─────────────────────────────────────────────────────────────────────

export function BeliefStatesPanel() {
  const { records, addGenerated, markTornDown, remove } = useBeliefStateRegistry()
  const [showHistory, setShowHistory] = useState(false)

  const generateMutation = useMutation({ mutationFn: generateBeliefState })
  const teardownMutation = useMutation({ mutationFn: teardownBeliefState })

  const { data: proxiedUrls = [], refetch: refetchUrls, isFetching: fetchingUrls } = useQuery({
    queryKey: ['proxied-urls'],
    queryFn: fetchProxiedUrls,
    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  function handleTeardownFromCard(record: BeliefStateRecord) {
    const payload: BeliefStateGeneration = { domainName: record.domainName, version: record.version }
    teardownMutation.mutate(payload, { onSuccess: () => markTornDown(payload) })
  }

  const activeRecords = records.filter(r => r.status === 'active')
  const tornDownRecords = records.filter(r => r.status === 'torn_down')

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
        <div className="flex items-center gap-3">
          <Database size={18} className="text-slate-400" />
          <div>
            <h1 className="text-lg font-semibold text-slate-100">Belief States</h1>
            <p className="text-xs text-slate-500 mt-0.5">
              {activeRecords.length} active
              {tornDownRecords.length > 0 && ` · ${tornDownRecords.length} torn down`}
            </p>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-auto px-6 py-5 space-y-6">

        {/* ── Active belief states ── */}
        {activeRecords.length > 0 && (
          <section>
            <h2 className="text-xs text-slate-500 uppercase tracking-wider mb-3">Active</h2>
            <div className="space-y-3">
              {activeRecords.map(r => (
                <BeliefStateCard
                  key={r.id}
                  record={r}
                  proxiedUrls={proxiedUrls}
                  onTeardown={handleTeardownFromCard}
                  onRemove={remove}
                />
              ))}
            </div>
          </section>
        )}

        {activeRecords.length === 0 && records.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 gap-3 text-center">
            <Database size={32} className="text-slate-700" />
            <p className="text-slate-500 text-sm">No belief states tracked yet</p>
            <p className="text-slate-600 text-xs">Use Generate below to deploy a belief state service.</p>
          </div>
        )}

        {/* ── Proxied endpoints ── */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-xs text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
              <Link size={11} />
              Proxied Endpoints ({proxiedUrls.length})
            </h2>
            <button
              onClick={() => refetchUrls()}
              disabled={fetchingUrls}
              className="p-1 rounded text-slate-600 hover:text-slate-400 transition-colors disabled:opacity-50"
              title="Refresh proxied endpoints"
            >
              <RefreshCw size={11} className={fetchingUrls ? 'animate-spin' : ''} />
            </button>
          </div>
          {proxiedUrls.length === 0 ? (
            <p className="text-xs text-slate-700 italic">No endpoints currently registered with the communication service.</p>
          ) : (
            <div className="rounded-lg border border-slate-800 divide-y divide-slate-800">
              {proxiedUrls.map((url, i) => (
                <div key={i} className="flex items-center gap-2 px-3 py-2">
                  <Link size={11} className="text-slate-600 shrink-0" />
                  <span className="text-xs font-mono text-slate-400 truncate">{url}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* ── Generate / Teardown actions ── */}
        <section>
          <h2 className="text-xs text-slate-500 uppercase tracking-wider mb-3">Actions</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <ActionCard
              title="Generate"
              description="Build, package, and deploy a belief state service for a domain."
              icon={<Play size={16} />}
              actionLabel="Generate Belief State"
              actionClass="bg-blue-700 hover:bg-blue-600"
              mutation={generateMutation}
              onSuccess={data => addGenerated(data)}
            />
            <ActionCard
              title="Teardown"
              description="Undeploy and clean up a previously generated belief state service."
              icon={<Trash2 size={16} />}
              actionLabel="Tear Down Belief State"
              actionClass="bg-red-700 hover:bg-red-600"
              mutation={teardownMutation}
              onSuccess={data => markTornDown(data)}
            />
          </div>
        </section>

        {/* ── Torn-down history ── */}
        {tornDownRecords.length > 0 && (
          <section>
            <button
              onClick={() => setShowHistory(h => !h)}
              className="flex items-center gap-2 text-xs text-slate-600 hover:text-slate-400 transition-colors mb-3"
            >
              {showHistory ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
              History ({tornDownRecords.length} torn down)
            </button>
            {showHistory && (
              <div className="space-y-2">
                {tornDownRecords.map(r => (
                  <BeliefStateCard
                    key={r.id}
                    record={r}
                    proxiedUrls={proxiedUrls}
                    onTeardown={handleTeardownFromCard}
                    onRemove={remove}
                  />
                ))}
              </div>
            )}
          </section>
        )}

        {/* ── Info callout ── */}
        <div className="rounded-lg border border-slate-700 bg-slate-800/30 px-4 py-3 text-xs text-slate-500 space-y-1">
          <p className="font-medium text-slate-400">How it works</p>
          <p>Generating a belief state compiles the domain schema into a deployable Kubernetes service. The registry above persists across page reloads so you can track what's deployed.</p>
          <p>Proxied endpoints are services currently registered with the communication service. Health probes call <code className="text-slate-400">/actuator/health</code> on the matched endpoint.</p>
        </div>
      </div>
    </div>
  )
}

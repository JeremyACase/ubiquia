import { NavLink } from 'react-router-dom'
import { BookOpen, GitBranch, Network, Activity, Database, Cpu } from 'lucide-react'

const NAV = [
  { to: '/ontologies', label: 'Domain Ontologies', icon: BookOpen },
  { to: '/dags', label: 'DAGs', icon: GitBranch },
  { to: '/agents', label: 'Agent Network', icon: Network },
  { to: '/flows', label: 'Flow Tracing', icon: Activity },
  { to: '/belief-states', label: 'Belief States', icon: Database },
  { to: '/components', label: 'Components', icon: Cpu },
]

export function Sidebar() {
  return (
    <aside className="w-56 shrink-0 flex flex-col bg-slate-900 border-r border-slate-700">
      <div className="px-4 py-5 border-b border-slate-700">
        <span className="text-xs font-mono font-semibold tracking-widest text-blue-400 uppercase">
          Ubiquia
        </span>
        <p className="text-slate-500 text-xs mt-0.5">Platform Dashboard</p>
      </div>

      <nav className="flex-1 py-3">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              [
                'flex items-center gap-3 px-4 py-2.5 text-sm transition-colors',
                isActive
                  ? 'bg-slate-800 text-blue-400 border-r-2 border-blue-400'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50',
              ].join(' ')
            }
          >
            <Icon size={15} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-4 py-3 border-t border-slate-700">
        <p className="text-xs text-slate-600">v0.1.0</p>
      </div>
    </aside>
  )
}

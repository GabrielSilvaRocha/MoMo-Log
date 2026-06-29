import type { ReactNode } from 'react'
import type { AppView } from '../App'

type AppShellProps = {
  children: ReactNode
  currentView: AppView
  onNavigate: (view: AppView) => void
}

const navItems: Array<{ id: AppView; label: string; icon: string }> = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  { id: 'workout', label: 'Treino do dia', icon: '🏋️' },
  { id: 'exercises', label: 'Exercícios', icon: '🔁' },
]

export function AppShell({ children, currentView, onNavigate }: AppShellProps) {
  return (
    <div className="min-h-screen bg-mo-background text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_right,rgba(119,255,107,0.16),transparent_28rem)]" />
      <div className="relative mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-8 rounded-3xl border border-mo-border bg-mo-surface/80 p-5 backdrop-blur">
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
            <div>
              <p className="text-sm uppercase tracking-[0.4em] text-mo-primary">Mo² LOG</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight">Treino híbrido inteligente</h1>
              <p className="mt-2 text-mo-muted">Musculação, corrida, adaptação e evolução em um só fluxo.</p>
            </div>
            <div className="rounded-2xl border border-mo-border bg-black/20 px-4 py-3 text-sm text-mo-muted">
              API: <span className="text-mo-primary">localhost:8000</span>
            </div>
          </div>

          <nav className="mt-6 flex flex-wrap gap-3">
            {navItems.map((item) => {
              const active = currentView === item.id
              return (
                <button
                  key={item.id}
                  onClick={() => onNavigate(item.id)}
                  className={`rounded-2xl px-4 py-2 text-sm font-semibold transition ${
                    active
                      ? 'bg-mo-primary text-black shadow-glow'
                      : 'border border-mo-border bg-white/[0.03] text-mo-muted hover:text-white'
                  }`}
                >
                  <span className="mr-2">{item.icon}</span>
                  {item.label}
                </button>
              )
            })}
          </nav>
        </header>

        {children}
      </div>
    </div>
  )
}

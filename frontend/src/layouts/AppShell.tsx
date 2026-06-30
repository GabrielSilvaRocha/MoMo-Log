import type { ReactNode } from 'react'
import type { AppView } from '../App'
import type { User } from '../types/api'

type AppShellProps = {
  children: ReactNode
  currentView: AppView
  onNavigate: (view: AppView) => void
  user: User
  onLogout: () => void
}

const navItems: Array<{ id: AppView; label: string; icon: string }> = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  { id: 'planning', label: 'Planejamento', icon: '📅' },
  { id: 'templates', label: 'Templates', icon: '🧩' },
  { id: 'workout', label: 'Treino do dia', icon: '🏋️' },
  { id: 'running', label: 'Corridas', icon: '🏃' },
  { id: 'analytics', label: 'Estatísticas', icon: '📈' },
  { id: 'intelligence', label: 'Inteligência', icon: '🧠' },
  { id: 'history', label: 'Histórico', icon: '🗂️' },
  { id: 'reports', label: 'Relatórios', icon: '📄' },
  { id: 'goals', label: 'Metas', icon: '🎯' },
  { id: 'exercises', label: 'Exercícios', icon: '🔁' },
  { id: 'adaptation', label: 'Adaptação', icon: '🧠' },
  { id: 'profile', label: 'Perfil', icon: '👤' },
  { id: 'mvp', label: 'MVP', icon: '🚀' },
  { id: 'deploy', label: 'Deploy', icon: '🛠️' },
]

export function AppShell({ children, currentView, onNavigate, user, onLogout }: AppShellProps) {
  return (
    <div className="min-h-screen bg-mo-background text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_right,rgba(119,255,107,0.16),transparent_28rem)]" />
      <div className="relative mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-8 rounded-3xl border border-mo-border bg-mo-surface/80 p-5 backdrop-blur">
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
            <div>
              <p className="text-sm uppercase tracking-[0.4em] text-mo-primary">Mo² LOG</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight">Mo² LOG com inteligência híbrida</h1>
              <p className="mt-2 text-mo-muted">Treino híbrido, templates de musculação, esteira manual, adaptação, relatórios e inteligência de evolução.</p>
            </div>
            <div className="flex flex-col gap-2 rounded-2xl border border-mo-border bg-black/20 px-4 py-3 text-sm text-mo-muted sm:items-end">
              <span>Usuário: <strong className="text-white">{user.name}</strong></span>
              <span>Versão: <span className="text-mo-primary">v6.0.0</span></span>
              <button onClick={onLogout} className="text-left text-xs font-semibold text-mo-primary hover:text-white sm:text-right">Sair</button>
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

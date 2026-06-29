import type { ReactNode } from 'react'

type AppShellProps = {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="min-h-screen bg-mo-background text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_right,rgba(119,255,107,0.16),transparent_28rem)]" />
      <div className="relative mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-8 flex flex-col justify-between gap-4 rounded-3xl border border-mo-border bg-mo-surface/80 p-5 backdrop-blur md:flex-row md:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.4em] text-mo-primary">Mo² LOG</p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight">Dashboard híbrido</h1>
            <p className="mt-2 text-mo-muted">Musculação, corrida e evolução em um só painel.</p>
          </div>
          <div className="rounded-2xl border border-mo-border bg-black/20 px-4 py-3 text-sm text-mo-muted">
            API: <span className="text-mo-primary">localhost:8000</span>
          </div>
        </header>

        {children}
      </div>
    </div>
  )
}

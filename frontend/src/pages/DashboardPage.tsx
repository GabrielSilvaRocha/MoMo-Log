import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { GoalCard } from '../components/GoalCard'
import { MetricCard } from '../components/MetricCard'
import { ProgressBar } from '../components/ProgressBar'
import { SessionList } from '../components/SessionList'
import type { ApiStatus, WeekDashboard, WeeklyStatistics } from '../types/api'
import { formatNumber } from '../utils/format'

type DashboardState = {
  health: ApiStatus | null
  dashboard: WeekDashboard | null
  statistics: WeeklyStatistics | null
}

type DashboardPageProps = {
  onStartWorkout: () => void
}

export function DashboardPage({ onStartWorkout }: DashboardPageProps) {
  const [state, setState] = useState<DashboardState>({
    health: null,
    dashboard: null,
    statistics: null,
  })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true

    async function loadDashboard() {
      try {
        setLoading(true)
        const [health, dashboard, statistics] = await Promise.all([
          mo2logApi.health(),
          mo2logApi.weekDashboard(),
          mo2logApi.weekStatistics(),
        ])

        if (mounted) {
          setState({ health, dashboard, statistics })
          setError(null)
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Erro ao carregar dashboard')
        }
      } finally {
        if (mounted) setLoading(false)
      }
    }

    loadDashboard()

    return () => {
      mounted = false
    }
  }, [])

  const todayTitle = useMemo(() => {
    const todaySessions = state.dashboard?.today_sessions ?? []
    if (!todaySessions.length) return 'Sem treino planejado para hoje'
    return todaySessions.map((session) => session.title).join(' + ')
  }, [state.dashboard])

  if (loading) {
    return (
      <div className="rounded-3xl border border-mo-border bg-mo-surface p-8 text-mo-muted">
        Carregando dados do Mo² LOG...
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-8">
        <h2 className="text-xl font-semibold text-white">Não foi possível carregar o dashboard.</h2>
        <p className="mt-2 text-red-200">{error}</p>
        <p className="mt-4 text-sm text-mo-muted">
          Confirme se o backend está rodando em http://localhost:8000 e se as migrations foram aplicadas.
        </p>
      </div>
    )
  }

  const dashboard = state.dashboard
  const statistics = state.statistics

  return (
    <main className="space-y-8">
      <section className="grid gap-4 lg:grid-cols-[1.6fr_1fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
          <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Hoje</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">{todayTitle}</h2>
          <p className="mt-3 text-mo-muted">
            O Mo² LOG mostra o plano do dia, o que já foi concluído e os próximos treinos da semana.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button onClick={onStartWorkout} className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow">
              Iniciar treino
            </button>
            <button onClick={onStartWorkout} className="rounded-2xl border border-mo-border px-5 py-3 font-semibold text-white">
              Trocar exercício
            </button>
          </div>
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-6">
          <p className="text-sm text-mo-muted">Score híbrido</p>
          <strong className="mt-2 block text-4xl text-white">{formatNumber(dashboard?.hybrid_score ?? 0, 0)}</strong>
          <p className="mt-2 text-sm text-mo-muted">Versão {state.health?.version} · API {state.health?.status?.toUpperCase()}</p>
          <div className="mt-6 rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Foco da semana</p>
            <p className="mt-1 font-semibold text-white">{dashboard?.next_focus}</p>
            <p className="mt-2 text-xs text-mo-muted">Recuperação: {dashboard?.recovery_balance === 'adequate' ? 'planejada' : 'monitorar carga'}</p>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Volume da semana"
          value={`${formatNumber(dashboard?.weekly_strength_volume ?? 0, 0)} kg`}
          hint="Soma de carga x repetições registradas"
          icon="🏋️"
        />
        <MetricCard
          label="Corrida na semana"
          value={`${formatNumber(dashboard?.weekly_running_distance_km ?? 0)} km`}
          hint="Dados do plano e registros manuais"
          icon="🏃"
        />
        <MetricCard
          label="Consistência"
          value={`${formatNumber(dashboard?.completion_rate ?? 0, 0)}%`}
          hint="Sessões concluídas vs planejadas"
          icon="🔥"
        />
        <MetricCard
          label="Score híbrido"
          value={`${formatNumber(dashboard?.hybrid_score ?? 0, 0)}`}
          hint="Força, corrida, consistência e volume"
          icon="ðŸ§­"
        />
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h2 className="text-lg font-semibold text-white">Mix híbrido</h2>
          <div className="mt-5 space-y-4">
            {(dashboard?.training_mix ?? []).map((item) => {
              const progress = item.planned ? Math.round((item.completed / item.planned) * 100) : 0
              return (
                <div key={item.key}>
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="font-semibold text-white">{item.label}</span>
                    <span className="text-mo-muted">{item.completed}/{item.planned}</span>
                  </div>
                  <ProgressBar value={progress} label={item.label} />
                </div>
              )
            })}
          </div>
        </section>
        <div className="lg:col-span-2">
          <SessionList
            title="Treinos de hoje"
            sessions={dashboard?.today_sessions ?? []}
            emptyText="Nenhuma sessão planejada para hoje."
          />
        </div>
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h2 className="text-lg font-semibold text-white">Progresso semanal</h2>
          <div className="mt-5">
            <ProgressBar value={dashboard?.completion_rate ?? 0} label="Conclusão do plano" />
          </div>
          <p className="mt-4 text-sm text-mo-muted">
            {statistics?.completed_sessions ?? 0} concluídos · {statistics?.upcoming_sessions ?? 0} próximos ·{' '}
            {statistics?.adapted_sessions ?? 0} adaptados
          </p>
        </section>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <SessionList
          title="Treinos concluídos"
          sessions={dashboard?.completed_sessions ?? []}
          emptyText="Ainda não há treinos concluídos nesta semana."
        />
        <SessionList
          title="Próximos treinos da semana"
          sessions={dashboard?.upcoming_sessions ?? []}
          emptyText="Não há próximos treinos planejados."
        />
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <div className="space-y-4">
          {(statistics?.active_goals ?? []).map((goal) => (
            <GoalCard key={goal.id} goal={goal} />
          ))}
        </div>

        <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h2 className="text-lg font-semibold text-white">Insights iniciais</h2>
          <div className="mt-4 space-y-3">
            {(statistics?.insights ?? []).map((insight) => (
              <p key={insight} className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">
                {insight}
              </p>
            ))}
          </div>
        </section>
      </section>
    </main>
  )
}

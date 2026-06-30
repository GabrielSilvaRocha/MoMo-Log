import { useEffect, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { GoalCard } from '../components/GoalCard'
import { MetricCard } from '../components/MetricCard'
import { ProgressBar } from '../components/ProgressBar'
import type { WeeklyStatistics } from '../types/api'
import { formatDate, formatNumber } from '../utils/format'

export function AnalyticsPage() {
  const [statistics, setStatistics] = useState<WeeklyStatistics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        setLoading(true)
        const data = await mo2logApi.weekStatistics(getCurrentUserId(), '2026-06-29')
        if (mounted) {
          setStatistics(data)
          setError(null)
        }
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Erro ao carregar estatísticas')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    load()

    return () => {
      mounted = false
    }
  }, [])

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Analytics Core</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Estatísticas e evolução</h2>
        <p className="mt-3 max-w-3xl text-mo-muted">
          O Mo² LOG combina musculação, corrida, metas e histórico para responder se o plano está sendo executado com consistência.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {loading && <div className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">Carregando analytics...</div>}

      {statistics && (
        <>
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard label="Conclusão" value={`${formatNumber(statistics.completion_rate, 0)}%`} hint="Plano semanal executado" icon="🔥" />
            <MetricCard label="Musculação" value={String(statistics.strength_sessions_completed)} hint="Sessões concluídas" icon="🏋️" />
            <MetricCard label="Corrida" value={String(statistics.running_sessions_completed)} hint="Sessões concluídas" icon="🏃" />
            <MetricCard label="Adaptados" value={String(statistics.adapted_sessions)} hint="Treinos ajustados" icon="🔁" />
          </section>

          <section className="grid gap-4 lg:grid-cols-[1fr_0.9fr]">
            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <div className="flex flex-col justify-between gap-2 md:flex-row md:items-center">
                <div>
                  <h3 className="text-lg font-semibold text-white">Semana analisada</h3>
                  <p className="mt-1 text-sm text-mo-muted">
                    {formatDate(statistics.week_start)} até {formatDate(statistics.week_end)}
                  </p>
                </div>
                <span className="rounded-full bg-white/[0.03] px-4 py-2 text-sm text-mo-muted">
                  Referência: {statistics.reference_date}
                </span>
              </div>
              <div className="mt-6">
                <ProgressBar value={statistics.completion_rate} label="Consistência semanal" />
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-2">
                <div className="rounded-2xl bg-white/[0.03] p-4">
                  <p className="text-sm text-mo-muted">Volume de musculação</p>
                  <p className="mt-1 text-2xl font-semibold text-white">{formatNumber(statistics.weekly_strength_volume, 0)} kg</p>
                </div>
                <div className="rounded-2xl bg-white/[0.03] p-4">
                  <p className="text-sm text-mo-muted">Quilometragem</p>
                  <p className="mt-1 text-2xl font-semibold text-white">{formatNumber(statistics.weekly_running_distance_km)} km</p>
                </div>
              </div>
            </div>

            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Insights</h3>
              <div className="mt-4 space-y-3">
                {statistics.insights.map((insight) => (
                  <p key={insight} className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">
                    {insight}
                  </p>
                ))}
              </div>
            </div>
          </section>

          <section className="grid gap-4 lg:grid-cols-2">
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-white">Metas ativas</h3>
              {statistics.active_goals.map((goal) => (
                <GoalCard key={goal.id} goal={goal} />
              ))}
            </div>

            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Recordes pessoais</h3>
              <div className="mt-4 space-y-3">
                {statistics.personal_records.map((record) => (
                  <article key={record.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                    <p className="text-sm text-mo-muted">{record.record_type}</p>
                    <h4 className="mt-1 font-semibold text-white">{record.title}</h4>
                    <p className="mt-2 text-2xl font-bold text-mo-primary">
                      {formatNumber(record.value)} {record.unit}
                    </p>
                  </article>
                ))}
              </div>
            </div>
          </section>
        </>
      )}
    </main>
  )
}

import { useEffect, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { MetricCard } from '../components/MetricCard'
import { ProgressBar } from '../components/ProgressBar'
import { LoadingState } from '../components/LoadingState'
import type { FiveKForecast, PlannedVsDone, WeeklyIntelligence } from '../types/api'
import { formatDate, formatNumber } from '../utils/format'

export function IntelligencePage() {
  const [weekly, setWeekly] = useState<WeeklyIntelligence | null>(null)
  const [plan, setPlan] = useState<PlannedVsDone | null>(null)
  const [forecast, setForecast] = useState<FiveKForecast | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        setLoading(true)
        const userId = getCurrentUserId()
        const [weeklyData, planData, forecastData] = await Promise.all([
          mo2logApi.weeklyIntelligence(userId, '2026-06-29'),
          mo2logApi.plannedVsDone(userId, '2026-06-29'),
          mo2logApi.forecast5k(userId),
        ])
        if (mounted) {
          setWeekly(weeklyData)
          setPlan(planData)
          setForecast(forecastData)
          setError(null)
        }
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Erro ao carregar inteligência do Mo² LOG')
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
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Intelligence Core</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Inteligência e evolução</h2>
        <p className="mt-3 max-w-3xl text-mo-muted">
          O Mo² LOG interpreta sua semana híbrida: compara planejado vs realizado, analisa volume, corrida, adaptações e gera recomendações acionáveis.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {loading && <LoadingState title="Carregando inteligência..." description="Analisando treinos, corridas e metas da semana." />}

      {weekly && forecast && (
        <>
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard label="Score híbrido" value={`${formatNumber(weekly.hybrid_score, 0)}/100`} hint="Consistência + corrida + musculação" icon="🧠" />
            <MetricCard label="Conclusão" value={`${formatNumber(weekly.summary.completion_rate, 0)}%`} hint="Planejado vs realizado" icon="🔥" />
            <MetricCard label="Previsão 5 km" value={forecast.predicted_5k_time_label ?? '-'} hint={`Confiança: ${forecast.confidence}`} icon="🏁" />
            <MetricCard label="Pace previsto" value={forecast.predicted_pace_label ?? '-'} hint={`${forecast.based_on_runs} corrida(s) usadas`} icon="⚡" />
          </section>

          <section className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <div className="flex flex-col justify-between gap-2 md:flex-row md:items-center">
                <div>
                  <h3 className="text-lg font-semibold text-white">Leitura da semana</h3>
                  <p className="mt-1 text-sm text-mo-muted">
                    {formatDate(weekly.week_start)} até {formatDate(weekly.week_end)}
                  </p>
                </div>
                <span className="rounded-full bg-mo-primary/10 px-4 py-2 text-sm font-semibold text-mo-primary">
                  Score {formatNumber(weekly.hybrid_score, 0)}
                </span>
              </div>

              <div className="mt-6">
                <ProgressBar value={weekly.hybrid_score} label="Score híbrido semanal" />
              </div>

              <div className="mt-5 grid gap-3 md:grid-cols-3">
                <div className="rounded-2xl bg-white/[0.03] p-4">
                  <p className="text-sm text-mo-muted">Volume</p>
                  <p className="mt-1 text-2xl font-semibold text-white">{formatNumber(weekly.summary.strength_volume, 0)} kg</p>
                </div>
                <div className="rounded-2xl bg-white/[0.03] p-4">
                  <p className="text-sm text-mo-muted">Corrida</p>
                  <p className="mt-1 text-2xl font-semibold text-white">{formatNumber(weekly.summary.running_distance_km)} km</p>
                </div>
                <div className="rounded-2xl bg-white/[0.03] p-4">
                  <p className="text-sm text-mo-muted">RPE médio</p>
                  <p className="mt-1 text-2xl font-semibold text-white">{weekly.summary.average_rpe ?? '-'}</p>
                </div>
              </div>
            </div>

            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Recomendações</h3>
              <div className="mt-4 space-y-3">
                {weekly.recommendations.map((item) => (
                  <article key={item.title} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                    <h4 className="font-semibold text-white">{item.title}</h4>
                    <p className="mt-2 text-sm text-mo-muted">{item.action}</p>
                  </article>
                ))}
              </div>
            </div>
          </section>

          <section className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Insights inteligentes</h3>
              <div className="mt-4 space-y-3">
                {weekly.insights.map((insight) => (
                  <article key={`${insight.type}-${insight.title}`} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                    <div className="flex items-start justify-between gap-3">
                      <h4 className="font-semibold text-white">{insight.title}</h4>
                      <span className="rounded-full bg-mo-primary/10 px-3 py-1 text-xs font-semibold text-mo-primary">{insight.severity}</span>
                    </div>
                    <p className="mt-2 text-sm text-mo-muted">{insight.message}</p>
                  </article>
                ))}
              </div>
            </div>

            <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Previsão 5 km</h3>
              <p className="mt-2 text-sm text-mo-muted">
                Estimativa inicial baseada nas corridas registradas. Quanto mais corridas de esteira você lançar, melhor fica a leitura.
              </p>
              <div className="mt-5 rounded-3xl border border-mo-primary/30 bg-mo-primary/10 p-5">
                <p className="text-sm text-mo-muted">Tempo previsto</p>
                <p className="mt-2 text-4xl font-bold text-mo-primary">{forecast.predicted_5k_time_label}</p>
                <p className="mt-2 text-sm text-mo-muted">Pace estimado: {forecast.predicted_pace_label}</p>
              </div>
              <div className="mt-4 space-y-2">
                {forecast.notes.map((note) => (
                  <p key={note} className="rounded-2xl bg-white/[0.03] p-3 text-sm text-mo-muted">{note}</p>
                ))}
              </div>
            </div>
          </section>
        </>
      )}

      {plan && (
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-lg font-semibold text-white">Planejado vs realizado</h3>
          <div className="mt-4 grid gap-3 md:grid-cols-4">
            {Object.entries(plan.by_type).map(([type, values]) => (
              <article key={type} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <p className="text-sm capitalize text-mo-muted">{type}</p>
                <p className="mt-2 text-2xl font-semibold text-white">{values.completed}/{values.planned}</p>
                <p className="mt-2 text-xs text-mo-muted">Adaptados: {values.adapted} · Pulados: {values.skipped}</p>
              </article>
            ))}
          </div>
          <div className="mt-5 grid gap-3 md:grid-cols-7">
            {plan.days.map((day) => (
              <div key={day.date} className="rounded-2xl bg-white/[0.03] p-3">
                <p className="text-xs text-mo-muted">{formatDate(day.date)}</p>
                <div className="mt-3 space-y-2">
                  {day.sessions.length === 0 && <p className="text-xs text-mo-muted">Sem sessão</p>}
                  {day.sessions.map((session) => (
                    <div key={session.id} className="rounded-xl border border-mo-border bg-black/20 p-2">
                      <p className="text-xs font-semibold text-white">{session.title}</p>
                      <p className="mt-1 text-[11px] text-mo-muted">{session.session_type} · {session.status}</p>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}

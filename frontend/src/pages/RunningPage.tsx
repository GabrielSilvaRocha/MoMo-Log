import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { MetricCard } from '../components/MetricCard'
import type { RunningActivity, StravaStatus, StravaSyncResult } from '../types/api'
import { formatDateTime, formatDuration, formatNumber, formatPace } from '../utils/format'

type RunningState = {
  activities: RunningActivity[]
  strava: StravaStatus | null
}

export function RunningPage() {
  const [state, setState] = useState<RunningState>({ activities: [], strava: null })
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [syncResult, setSyncResult] = useState<StravaSyncResult | null>(null)

  async function load() {
    try {
      setLoading(true)
      const [activities, strava] = await Promise.all([mo2logApi.runningActivities(1), mo2logApi.stravaStatus(1)])
      setState({ activities, strava })
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar corridas')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const summary = useMemo(() => {
    const totalDistanceKm = state.activities.reduce((total, activity) => total + Number(activity.distance_m) / 1000, 0)
    const totalSeconds = state.activities.reduce((total, activity) => total + activity.moving_time_s, 0)
    const avgPace = totalDistanceKm > 0 ? totalSeconds / 60 / totalDistanceKm : 0

    return {
      totalDistanceKm,
      totalSeconds,
      avgPace,
      count: state.activities.length,
    }
  }, [state.activities])

  async function syncStrava() {
    setSyncing(true)
    setSyncResult(null)
    try {
      const result = await mo2logApi.stravaSync(1)
      setSyncResult(result)
      await load()
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao sincronizar Strava')
    } finally {
      setSyncing(false)
    }
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Running Core</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Corridas e Strava</h2>
            <p className="mt-3 max-w-3xl text-mo-muted">
              A corrida continua sendo registrada no Strava. O Mo² LOG importa os dados, vincula ao planejamento semanal e transforma em
              análise de consistência.
            </p>
          </div>
          <button
            onClick={syncStrava}
            disabled={syncing}
            className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-50"
          >
            {syncing ? 'Sincronizando...' : 'Sincronizar Strava'}
          </button>
        </div>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {syncResult && (
        <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">
          {syncResult.message} Importadas: {syncResult.imported} · Ignoradas: {syncResult.ignored}
        </div>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Corridas" value={String(summary.count)} hint="Atividades importadas" icon="🏃" />
        <MetricCard label="Distância total" value={`${formatNumber(summary.totalDistanceKm)} km`} hint="Somatório das atividades" icon="📏" />
        <MetricCard label="Tempo em movimento" value={formatDuration(summary.totalSeconds)} hint="Tempo efetivo de corrida" icon="⏱️" />
        <MetricCard label="Pace médio" value={formatPace(summary.avgPace)} hint="Média calculada do histórico" icon="⚡" />
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.8fr_1.2fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-lg font-semibold text-white">Status Strava</h3>
          <div className="mt-4 rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Conexão</p>
            <p className="mt-1 text-xl font-semibold text-white">{state.strava?.connected ? 'Conectado' : 'Não conectado'}</p>
          </div>
          <div className="mt-3 rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Atleta</p>
            <p className="mt-1 font-semibold text-white">{state.strava?.strava_athlete_id ?? 'Mock local'}</p>
          </div>
          <p className="mt-4 text-sm text-mo-muted">
            Nesta fase a sincronização ainda é simulada. A integração OAuth real será a próxima camada do Running Core.
          </p>
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-lg font-semibold text-white">Atividades importadas</h3>
          <div className="mt-5 space-y-3">
            {loading && <p className="text-sm text-mo-muted">Carregando corridas...</p>}
            {!loading && state.activities.length === 0 && (
              <p className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">
                Nenhuma corrida importada. Clique em “Sincronizar Strava” para gerar os dados mock da semana.
              </p>
            )}
            {state.activities.map((activity) => (
              <article key={activity.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
                  <div>
                    <h4 className="font-semibold text-white">{activity.name}</h4>
                    <p className="mt-1 text-sm text-mo-muted">{formatDateTime(activity.start_date)}</p>
                  </div>
                  <span className="rounded-full bg-mo-primary/10 px-3 py-1 text-xs font-semibold text-mo-primary">
                    {activity.source ?? 'strava'}
                  </span>
                </div>
                <div className="mt-4 grid gap-3 text-sm md:grid-cols-3">
                  <div className="rounded-xl bg-black/20 p-3">
                    <p className="text-mo-muted">Distância</p>
                    <p className="font-semibold text-white">{formatNumber(Number(activity.distance_m) / 1000)} km</p>
                  </div>
                  <div className="rounded-xl bg-black/20 p-3">
                    <p className="text-mo-muted">Tempo</p>
                    <p className="font-semibold text-white">{formatDuration(activity.moving_time_s)}</p>
                  </div>
                  <div className="rounded-xl bg-black/20 p-3">
                    <p className="text-mo-muted">Pace</p>
                    <p className="font-semibold text-white">{formatPace(activity.average_pace)}</p>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </main>
  )
}

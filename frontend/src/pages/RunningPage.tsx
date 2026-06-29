import { FormEvent, useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { MetricCard } from '../components/MetricCard'
import type { RunningActivity, StravaStatus, StravaSyncResult } from '../types/api'
import { formatDateTime, formatDuration, formatNumber, formatPace } from '../utils/format'

type RunningState = {
  activities: RunningActivity[]
  strava: StravaStatus | null
}

type ManualRunForm = {
  name: string
  distanceKm: string
  durationMinutes: string
  startDate: string
  startTime: string
  notes: string
}

const sourceLabels: Record<string, string> = {
  manual_treadmill: 'Esteira manual',
  manual_outdoor: 'Manual rua',
  manual: 'Manual',
  samsung_health: 'Samsung Health',
  health_connect: 'Health Connect',
  gpx_import: 'GPX',
  strava: 'Strava',
  strava_mock: 'Mock Strava',
}

function getSourceLabel(source?: string | null) {
  if (!source) return 'Manual'
  return sourceLabels[source] ?? source
}

function getTodayDateInput() {
  return new Date().toISOString().slice(0, 10)
}

function getNowTimeInput() {
  return new Date().toTimeString().slice(0, 5)
}

export function RunningPage() {
  const [state, setState] = useState<RunningState>({ activities: [], strava: null })
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [savingManualRun, setSavingManualRun] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [syncResult, setSyncResult] = useState<StravaSyncResult | null>(null)
  const [manualRun, setManualRun] = useState<ManualRunForm>({
    name: 'Corrida na esteira',
    distanceKm: '',
    durationMinutes: '',
    startDate: getTodayDateInput(),
    startTime: getNowTimeInput(),
    notes: '',
  })

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
    const treadmillCount = state.activities.filter((activity) => activity.source === 'manual_treadmill').length

    return {
      totalDistanceKm,
      totalSeconds,
      avgPace,
      count: state.activities.length,
      treadmillCount,
    }
  }, [state.activities])

  async function connectStrava() {
    try {
      const response = await mo2logApi.stravaAuthorize(1)
      if (!response.configured) {
        setError('OAuth do Strava ainda não está configurado. Como alternativa, use o cadastro manual para corridas na esteira.')
        return
      }
      window.location.href = response.authorization_url
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao iniciar conexão com Strava')
    }
  }

  async function syncStrava() {
    setSyncing(true)
    setSyncResult(null)
    setSuccessMessage(null)
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

  async function createManualRun(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSavingManualRun(true)
    setSuccessMessage(null)
    setSyncResult(null)

    try {
      const distanceKm = Number(manualRun.distanceKm.replace(',', '.'))
      const durationMinutes = Number(manualRun.durationMinutes.replace(',', '.'))

      if (!Number.isFinite(distanceKm) || distanceKm <= 0) {
        throw new Error('Informe uma distância válida em km.')
      }

      if (!Number.isFinite(durationMinutes) || durationMinutes <= 0) {
        throw new Error('Informe uma duração válida em minutos.')
      }

      const startDateTime = new Date(`${manualRun.startDate}T${manualRun.startTime || '00:00'}:00`)

      await mo2logApi.createManualRun({
        user_id: 1,
        name: manualRun.name.trim() || 'Corrida na esteira',
        distance_m: Math.round(distanceKm * 1000),
        moving_time_s: Math.round(durationMinutes * 60),
        elapsed_time_s: Math.round(durationMinutes * 60),
        activity_type: 'TreadmillRun',
        source: 'manual_treadmill',
        start_date: startDateTime.toISOString(),
        total_elevation_gain: 0,
      })

      setManualRun({
        name: 'Corrida na esteira',
        distanceKm: '',
        durationMinutes: '',
        startDate: getTodayDateInput(),
        startTime: getNowTimeInput(),
        notes: '',
      })
      setSuccessMessage('Corrida na esteira registrada com sucesso.')
      setError(null)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao cadastrar corrida manual')
    } finally {
      setSavingManualRun(false)
    }
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Running Sources</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Corridas</h2>
            <p className="mt-3 max-w-3xl text-mo-muted">
              O Mo² LOG agora não depende apenas do Strava. Você pode registrar corridas manualmente, especialmente treinos na esteira,
              manter o Strava como fonte opcional e preparar a base para Samsung Health / Health Connect no futuro.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              onClick={connectStrava}
              className="rounded-2xl border border-mo-primary/50 px-5 py-3 font-semibold text-mo-primary transition hover:bg-mo-primary/10"
            >
              Conectar Strava
            </button>
            <button
              onClick={syncStrava}
              disabled={syncing}
              className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-50"
            >
              {syncing ? 'Sincronizando...' : 'Sincronizar mock/Strava'}
            </button>
          </div>
        </div>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {successMessage && <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{successMessage}</div>}
      {syncResult && (
        <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">
          {syncResult.message} Importadas: {syncResult.imported} · Ignoradas: {syncResult.ignored}
        </div>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <MetricCard label="Corridas" value={String(summary.count)} hint="Atividades registradas" icon="🏃" />
        <MetricCard label="Esteira" value={String(summary.treadmillCount)} hint="Entradas manuais" icon="🏟️" />
        <MetricCard label="Distância total" value={`${formatNumber(summary.totalDistanceKm)} km`} hint="Somatório das atividades" icon="📏" />
        <MetricCard label="Tempo em movimento" value={formatDuration(summary.totalSeconds)} hint="Tempo efetivo de corrida" icon="⏱️" />
        <MetricCard label="Pace médio" value={formatPace(summary.avgPace)} hint="Média calculada do histórico" icon="⚡" />
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <form onSubmit={createManualRun} className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-mo-primary">Entrada manual</p>
            <h3 className="mt-2 text-xl font-semibold text-white">Registrar corrida na esteira</h3>
            <p className="mt-2 text-sm text-mo-muted">
              Use quando a corrida for feita na esteira ou quando você preferir lançar o treino sem depender de integração externa.
            </p>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <label className="space-y-2 md:col-span-2">
              <span className="text-sm font-medium text-mo-muted">Nome do treino</span>
              <input
                value={manualRun.name}
                onChange={(event) => setManualRun((current) => ({ ...current, name: event.target.value }))}
                className="w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none transition focus:border-mo-primary"
                placeholder="Corrida na esteira"
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm font-medium text-mo-muted">Distância (km)</span>
              <input
                value={manualRun.distanceKm}
                onChange={(event) => setManualRun((current) => ({ ...current, distanceKm: event.target.value }))}
                className="w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none transition focus:border-mo-primary"
                inputMode="decimal"
                placeholder="5,00"
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm font-medium text-mo-muted">Duração (min)</span>
              <input
                value={manualRun.durationMinutes}
                onChange={(event) => setManualRun((current) => ({ ...current, durationMinutes: event.target.value }))}
                className="w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none transition focus:border-mo-primary"
                inputMode="decimal"
                placeholder="30"
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm font-medium text-mo-muted">Data</span>
              <input
                type="date"
                value={manualRun.startDate}
                onChange={(event) => setManualRun((current) => ({ ...current, startDate: event.target.value }))}
                className="w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none transition focus:border-mo-primary"
              />
            </label>

            <label className="space-y-2">
              <span className="text-sm font-medium text-mo-muted">Horário</span>
              <input
                type="time"
                value={manualRun.startTime}
                onChange={(event) => setManualRun((current) => ({ ...current, startTime: event.target.value }))}
                className="w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none transition focus:border-mo-primary"
              />
            </label>
          </div>

          <button
            type="submit"
            disabled={savingManualRun}
            className="mt-5 w-full rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-50"
          >
            {savingManualRun ? 'Salvando...' : 'Registrar corrida manual'}
          </button>
        </form>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-lg font-semibold text-white">Fontes de corrida</h3>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-mo-primary/30 bg-mo-primary/10 p-4">
              <p className="text-sm font-semibold text-mo-primary">Principal agora</p>
              <h4 className="mt-1 font-semibold text-white">Manual / Esteira</h4>
              <p className="mt-2 text-sm text-mo-muted">Ideal para os seus treinos indoor. O Mo² LOG calcula pace e volume semanal automaticamente.</p>
            </div>
            <div className="rounded-2xl bg-white/[0.03] p-4">
              <p className="text-sm font-semibold text-mo-muted">Opcional</p>
              <h4 className="mt-1 font-semibold text-white">Strava</h4>
              <p className="mt-2 text-sm text-mo-muted">Mantido como integração opcional quando a API estiver disponível para sua conta.</p>
            </div>
            <div className="rounded-2xl bg-white/[0.03] p-4">
              <p className="text-sm font-semibold text-mo-muted">Próxima integração</p>
              <h4 className="mt-1 font-semibold text-white">Samsung Health</h4>
              <p className="mt-2 text-sm text-mo-muted">Entrada planejada por arquivo exportado ou Health Connect em uma versão mobile futura.</p>
            </div>
            <div className="rounded-2xl bg-white/[0.03] p-4">
              <p className="text-sm font-semibold text-mo-muted">Futuro</p>
              <h4 className="mt-1 font-semibold text-white">GPX / CSV / FIT</h4>
              <p className="mt-2 text-sm text-mo-muted">Importação manual por arquivo para não depender de fornecedores fechados.</p>
            </div>
          </div>

          <div className="mt-5 rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Status Strava</p>
            <p className="mt-1 text-xl font-semibold text-white">{state.strava?.connected ? 'Conectado' : 'Não conectado'}</p>
            <p className="mt-2 text-sm text-mo-muted">Atleta: {state.strava?.strava_athlete_id ?? 'Mock local'}</p>
          </div>
        </div>
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <h3 className="text-lg font-semibold text-white">Atividades registradas</h3>
        <div className="mt-5 space-y-3">
          {loading && <p className="text-sm text-mo-muted">Carregando corridas...</p>}
          {!loading && state.activities.length === 0 && (
            <p className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">
              Nenhuma corrida registrada. Cadastre uma corrida na esteira ou use a sincronização mock/Strava.
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
                  {getSourceLabel(activity.source)}
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
      </section>
    </main>
  )
}

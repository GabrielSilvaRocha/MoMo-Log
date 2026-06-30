import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import type { HistorySummary, TrainingSession } from '../types/api'
import { formatDate, formatDuration, formatNumber, formatPace, translateSessionType, translateStatus } from '../utils/format'

const USER_ID = 1

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoIso(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().slice(0, 10)
}

function sessionVolume(session: TrainingSession): number {
  return (session.strength_exercises ?? []).reduce((total, workoutExercise) => {
    return total + workoutExercise.set_logs.reduce((exerciseTotal, setLog) => {
      return exerciseTotal + Number(setLog.load) * setLog.reps
    }, 0)
  }, 0)
}

function sessionSetCount(session: TrainingSession): number {
  return (session.strength_exercises ?? []).reduce((total, workoutExercise) => total + workoutExercise.set_logs.length, 0)
}

function statusClass(status: string): string {
  const classes: Record<string, string> = {
    completed: 'border-emerald-400/30 bg-emerald-400/10 text-emerald-200',
    in_progress: 'border-yellow-400/30 bg-yellow-400/10 text-yellow-200',
    adapted: 'border-mo-primary/30 bg-mo-primary/10 text-mo-primary',
    skipped: 'border-red-400/30 bg-red-400/10 text-red-200',
    planned: 'border-mo-border bg-white/[0.03] text-mo-muted',
  }
  return classes[status] ?? classes.planned
}

export function HistoryPage() {
  const [dateFrom, setDateFrom] = useState(daysAgoIso(30))
  const [dateTo, setDateTo] = useState(todayIso())
  const [sessionType, setSessionType] = useState('')
  const [status, setStatus] = useState('')
  const [summary, setSummary] = useState<HistorySummary | null>(null)
  const [sessions, setSessions] = useState<TrainingSession[]>([])
  const [selectedSession, setSelectedSession] = useState<TrainingSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function loadHistory() {
    try {
      setLoading(true)
      setError(null)
      const [summaryData, sessionData] = await Promise.all([
        mo2logApi.historySummary(USER_ID, dateFrom, dateTo),
        mo2logApi.sessionHistory(USER_ID, {
          dateFrom,
          dateTo,
          sessionType: sessionType || undefined,
          status: status || undefined,
          limit: 100,
        }),
      ])
      setSummary(summaryData)
      setSessions(sessionData)
      setSelectedSession(sessionData[0] ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar histórico')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadHistory()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selectedVolume = useMemo(() => selectedSession ? sessionVolume(selectedSession) : 0, [selectedSession])
  const selectedSets = useMemo(() => selectedSession ? sessionSetCount(selectedSession) : 0, [selectedSession])

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-soft">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <p className="text-sm uppercase tracking-[0.35em] text-mo-primary">Histórico</p>
            <h2 className="mt-2 text-3xl font-bold">Linha do tempo dos treinos</h2>
            <p className="mt-2 max-w-3xl text-mo-muted">
              Consulte treinos concluídos, adaptações, corridas manuais, volume registrado e sessões puladas para entender a consistência do plano.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-5">
            <label className="text-xs font-semibold uppercase tracking-[0.2em] text-mo-muted">
              De
              <input
                type="date"
                value={dateFrom}
                onChange={(event) => setDateFrom(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-mo-primary"
              />
            </label>
            <label className="text-xs font-semibold uppercase tracking-[0.2em] text-mo-muted">
              Até
              <input
                type="date"
                value={dateTo}
                onChange={(event) => setDateTo(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-mo-primary"
              />
            </label>
            <label className="text-xs font-semibold uppercase tracking-[0.2em] text-mo-muted">
              Tipo
              <select
                value={sessionType}
                onChange={(event) => setSessionType(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-mo-primary"
              >
                <option value="">Todos</option>
                <option value="strength">Musculação</option>
                <option value="running">Corrida</option>
                <option value="mobility">Mobilidade</option>
                <option value="rest">Descanso</option>
              </select>
            </label>
            <label className="text-xs font-semibold uppercase tracking-[0.2em] text-mo-muted">
              Status
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-mo-primary"
              >
                <option value="">Todos</option>
                <option value="planned">Planejado</option>
                <option value="completed">Concluído</option>
                <option value="adapted">Adaptado</option>
                <option value="skipped">Pulado</option>
                <option value="in_progress">Em andamento</option>
              </select>
            </label>
            <button
              onClick={loadHistory}
              className="self-end rounded-2xl bg-mo-primary px-5 py-2 text-sm font-bold text-black shadow-glow transition hover:scale-[1.01]"
            >
              Aplicar filtros
            </button>
          </div>
        </div>
      </section>

      {error && <div className="rounded-3xl border border-red-400/30 bg-red-400/10 p-4 text-sm text-red-200">{error}</div>}
      {loading && <div className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">Carregando histórico...</div>}

      {summary && !loading && (
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5 shadow-soft">
            <p className="text-sm text-mo-muted">Sessões concluídas</p>
            <p className="mt-2 text-3xl font-bold">{summary.completed_sessions}/{summary.total_sessions}</p>
            <p className="mt-1 text-sm text-mo-primary">{formatNumber(summary.completion_rate, 0)}% de conclusão</p>
          </div>
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5 shadow-soft">
            <p className="text-sm text-mo-muted">Volume de musculação</p>
            <p className="mt-2 text-3xl font-bold">{formatNumber(summary.strength_volume, 0)} kg</p>
            <p className="mt-1 text-sm text-mo-muted">{summary.total_sets} séries registradas</p>
          </div>
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5 shadow-soft">
            <p className="text-sm text-mo-muted">Corrida</p>
            <p className="mt-2 text-3xl font-bold">{formatNumber(summary.running_distance_km, 1)} km</p>
            <p className="mt-1 text-sm text-mo-muted">{summary.running_activities} atividades · {formatDuration(summary.running_time_s)}</p>
          </div>
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5 shadow-soft">
            <p className="text-sm text-mo-muted">Adaptações</p>
            <p className="mt-2 text-3xl font-bold">{summary.adapted_sessions}</p>
            <p className="mt-1 text-sm text-mo-muted">{summary.skipped_sessions} sessões puladas</p>
          </div>
        </section>
      )}

      {!loading && (
        <section className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-soft">
            <div className="mb-5 flex items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-bold">Sessões encontradas</h3>
                <p className="text-sm text-mo-muted">Clique em uma sessão para ver os detalhes.</p>
              </div>
              <span className="rounded-full border border-mo-border px-3 py-1 text-sm text-mo-muted">{sessions.length} itens</span>
            </div>

            <div className="space-y-3">
              {sessions.map((session) => {
                const active = selectedSession?.id === session.id
                return (
                  <button
                    key={session.id}
                    onClick={() => setSelectedSession(session)}
                    className={`w-full rounded-3xl border p-4 text-left transition ${
                      active ? 'border-mo-primary bg-mo-primary/10' : 'border-mo-border bg-black/20 hover:border-mo-primary/50'
                    }`}
                  >
                    <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm text-mo-muted">{formatDate(session.scheduled_date)}</span>
                          <span className={`rounded-full border px-3 py-1 text-xs font-semibold ${statusClass(session.status)}`}>
                            {translateStatus(session.status)}
                          </span>
                          <span className="rounded-full border border-mo-border px-3 py-1 text-xs text-mo-muted">
                            {translateSessionType(session.session_type)}
                          </span>
                        </div>
                        <h4 className="mt-2 text-lg font-bold">{session.title}</h4>
                        <p className="mt-1 text-sm text-mo-muted">{session.notes || 'Sem observações.'}</p>
                      </div>
                      <div className="text-right text-sm text-mo-muted">
                        {session.session_type === 'strength' && <p>{formatNumber(sessionVolume(session), 0)} kg</p>}
                        {session.running_activity && <p>{formatNumber(Number(session.running_activity.distance_m) / 1000, 1)} km</p>}
                        <p>{session.strength_exercises?.length ?? 0} exercícios</p>
                      </div>
                    </div>
                  </button>
                )
              })}

              {sessions.length === 0 && (
                <div className="rounded-3xl border border-dashed border-mo-border p-8 text-center text-mo-muted">
                  Nenhuma sessão encontrada para os filtros selecionados.
                </div>
              )}
            </div>
          </div>

          <aside className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-soft">
            {!selectedSession ? (
              <p className="text-mo-muted">Selecione uma sessão para ver detalhes.</p>
            ) : (
              <div className="space-y-5">
                <div>
                  <p className="text-sm uppercase tracking-[0.25em] text-mo-primary">Detalhe</p>
                  <h3 className="mt-2 text-2xl font-bold">{selectedSession.title}</h3>
                  <p className="mt-1 text-sm text-mo-muted">{formatDate(selectedSession.scheduled_date)} · {translateSessionType(selectedSession.session_type)}</p>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <p className="text-xs text-mo-muted">Status</p>
                    <p className="mt-1 font-bold">{translateStatus(selectedSession.status)}</p>
                  </div>
                  <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <p className="text-xs text-mo-muted">Fonte</p>
                    <p className="mt-1 font-bold">{selectedSession.source}</p>
                  </div>
                  <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <p className="text-xs text-mo-muted">Volume</p>
                    <p className="mt-1 font-bold">{formatNumber(selectedVolume, 0)} kg</p>
                  </div>
                  <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <p className="text-xs text-mo-muted">Séries</p>
                    <p className="mt-1 font-bold">{selectedSets}</p>
                  </div>
                </div>

                {selectedSession.running_activity && (
                  <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <h4 className="font-bold">Corrida vinculada</h4>
                    <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                      <p className="text-mo-muted">Distância<br /><span className="text-white">{formatNumber(Number(selectedSession.running_activity.distance_m) / 1000, 2)} km</span></p>
                      <p className="text-mo-muted">Pace<br /><span className="text-white">{formatPace(selectedSession.running_activity.average_pace)}</span></p>
                      <p className="text-mo-muted">Tempo<br /><span className="text-white">{formatDuration(selectedSession.running_activity.moving_time_s)}</span></p>
                      <p className="text-mo-muted">Fonte<br /><span className="text-white">{selectedSession.running_activity.source ?? 'manual'}</span></p>
                    </div>
                  </div>
                )}

                <div className="space-y-3">
                  <h4 className="font-bold">Exercícios registrados</h4>
                  {(selectedSession.strength_exercises ?? []).map((workoutExercise) => (
                    <div key={workoutExercise.id} className="rounded-2xl border border-mo-border bg-black/20 p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-semibold">{workoutExercise.exercise?.name ?? `Exercício #${workoutExercise.exercise_id}`}</p>
                          <p className="text-sm text-mo-muted">Planejado: {workoutExercise.planned_sets}x {workoutExercise.planned_reps}</p>
                        </div>
                        <span className="rounded-full border border-mo-border px-3 py-1 text-xs text-mo-muted">
                          {workoutExercise.set_logs.length}/{workoutExercise.planned_sets}
                        </span>
                      </div>
                      <div className="mt-3 space-y-2 text-sm text-mo-muted">
                        {workoutExercise.set_logs.map((setLog) => (
                          <div key={setLog.id} className="flex justify-between rounded-xl bg-white/[0.03] px-3 py-2">
                            <span>Série {setLog.set_number}</span>
                            <span>{setLog.reps} reps · {formatNumber(setLog.load, 1)} kg · RPE {setLog.rpe ?? '-'}</span>
                          </div>
                        ))}
                        {workoutExercise.set_logs.length === 0 && <p>Nenhuma série registrada.</p>}
                      </div>
                    </div>
                  ))}
                  {(selectedSession.strength_exercises ?? []).length === 0 && <p className="text-sm text-mo-muted">Nenhum exercício registrado nesta sessão.</p>}
                </div>
              </div>
            )}
          </aside>
        </section>
      )}
    </div>
  )
}

import { FormEvent, useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import type { Exercise, TrainingSession } from '../types/api'
import { formatDate, translateSessionType, translateStatus } from '../utils/format'

type SessionForm = {
  title: string
  sessionType: 'strength' | 'running' | 'mobility' | 'rest'
  scheduledDate: string
  notes: string
}

type ExerciseForm = {
  exerciseId: string
  plannedSets: string
  plannedReps: string
  plannedLoad: string
  restSeconds: string
  notes: string
}

const defaultSessionForm: SessionForm = {
  title: 'Novo treino',
  sessionType: 'strength',
  scheduledDate: new Date().toISOString().slice(0, 10),
  notes: '',
}

const defaultExerciseForm: ExerciseForm = {
  exerciseId: '',
  plannedSets: '3',
  plannedReps: '8-12',
  plannedLoad: '',
  restSeconds: '90',
  notes: '',
}

const typeOptions = [
  { value: 'strength', label: 'Musculação' },
  { value: 'running', label: 'Corrida' },
  { value: 'mobility', label: 'Mobilidade' },
  { value: 'rest', label: 'Descanso' },
] as const

const statusOptions = [
  'planned',
  'in_progress',
  'completed',
  'skipped',
  'rescheduled',
  'adapted',
] as const

export function PlanningPage() {
  const [referenceDate, setReferenceDate] = useState('2026-06-29')
  const [sessions, setSessions] = useState<TrainingSession[]>([])
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null)
  const [sessionForm, setSessionForm] = useState<SessionForm>(defaultSessionForm)
  const [exerciseForm, setExerciseForm] = useState<ExerciseForm>(defaultExerciseForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      setLoading(true)
      const [weekSessions, exerciseList] = await Promise.all([
        mo2logApi.weekTrainingSessions(1, referenceDate),
        mo2logApi.exercises(),
      ])
      setSessions(weekSessions)
      setExercises(exerciseList)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar planejamento')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [referenceDate])

  const selectedSession = useMemo(
    () => sessions.find((session) => session.id === selectedSessionId) ?? null,
    [sessions, selectedSessionId],
  )

  const weekSummary = useMemo(() => {
    const strength = sessions.filter((session) => session.session_type === 'strength').length
    const running = sessions.filter((session) => session.session_type === 'running').length
    const completed = sessions.filter((session) => session.status === 'completed').length
    return { strength, running, completed, total: sessions.length }
  }, [sessions])

  function selectSession(session: TrainingSession) {
    setSelectedSessionId(session.id)
    setSessionForm({
      title: session.title,
      sessionType: session.session_type,
      scheduledDate: session.scheduled_date,
      notes: session.notes ?? '',
    })
    setExerciseForm({
      ...defaultExerciseForm,
      exerciseId: exercises[0]?.id ? String(exercises[0].id) : '',
    })
    setMessage(null)
    setError(null)
  }

  function newSession() {
    setSelectedSessionId(null)
    setSessionForm(defaultSessionForm)
    setMessage(null)
    setError(null)
  }

  async function saveSession(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setMessage(null)

    try {
      if (selectedSessionId) {
        await mo2logApi.updateTrainingSession(selectedSessionId, {
          title: sessionForm.title,
          scheduled_date: sessionForm.scheduledDate,
          notes: sessionForm.notes || null,
        })
        setMessage('Sessão atualizada com sucesso.')
      } else {
        const created = await mo2logApi.createTrainingSession({
          user_id: 1,
          training_plan_id: 1,
          title: sessionForm.title,
          session_type: sessionForm.sessionType,
          scheduled_date: sessionForm.scheduledDate,
          source: 'manual_planning',
          notes: sessionForm.notes || null,
        })
        setSelectedSessionId(created.id)
        setMessage('Sessão criada com sucesso.')
      }
      await load()
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao salvar sessão')
    } finally {
      setSaving(false)
    }
  }

  async function updateStatus(status: (typeof statusOptions)[number]) {
    if (!selectedSessionId) return
    setSaving(true)
    try {
      await mo2logApi.updateTrainingSession(selectedSessionId, { status })
      setMessage(`Status alterado para ${translateStatus(status)}.`)
      await load()
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao alterar status')
    } finally {
      setSaving(false)
    }
  }

  async function deleteSession() {
    if (!selectedSessionId) return
    const confirmed = window.confirm('Excluir esta sessão do planejamento semanal?')
    if (!confirmed) return

    setSaving(true)
    try {
      await mo2logApi.deleteTrainingSession(selectedSessionId)
      setSelectedSessionId(null)
      setSessionForm(defaultSessionForm)
      setMessage('Sessão excluída.')
      await load()
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao excluir sessão')
    } finally {
      setSaving(false)
    }
  }

  async function addExercise(event: FormEvent) {
    event.preventDefault()
    if (!selectedSessionId || !exerciseForm.exerciseId) return

    setSaving(true)
    try {
      await mo2logApi.addStrengthExerciseToSession(selectedSessionId, {
        exercise_id: Number(exerciseForm.exerciseId),
        planned_sets: Number(exerciseForm.plannedSets),
        planned_reps: exerciseForm.plannedReps,
        planned_load: exerciseForm.plannedLoad ? Number(exerciseForm.plannedLoad) : null,
        rest_seconds: exerciseForm.restSeconds ? Number(exerciseForm.restSeconds) : null,
        notes: exerciseForm.notes || null,
      })
      setMessage('Exercício adicionado ao treino.')
      setExerciseForm({ ...defaultExerciseForm, exerciseId: exerciseForm.exerciseId })
      await load()
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao adicionar exercício')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="rounded-3xl border border-mo-border bg-mo-surface p-8 text-mo-muted">Carregando planejamento...</div>
  }

  return (
    <main className="space-y-8">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Planejamento</p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight text-white">Semana editável</h2>
            <p className="mt-2 max-w-3xl text-mo-muted">
              Crie, ajuste e acompanhe as sessões de musculação, corrida, mobilidade e descanso da semana.
            </p>
          </div>
          <label className="text-sm text-mo-muted">
            Semana de referência
            <input
              type="date"
              value={referenceDate}
              onChange={(event) => setReferenceDate(event.target.value)}
              className="mt-2 block rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
            />
          </label>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-4">
          <div className="rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Sessões</p>
            <strong className="mt-1 block text-2xl text-white">{weekSummary.total}</strong>
          </div>
          <div className="rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Musculação</p>
            <strong className="mt-1 block text-2xl text-white">{weekSummary.strength}</strong>
          </div>
          <div className="rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Corridas</p>
            <strong className="mt-1 block text-2xl text-white">{weekSummary.running}</strong>
          </div>
          <div className="rounded-2xl bg-white/[0.03] p-4">
            <p className="text-sm text-mo-muted">Concluídas</p>
            <strong className="mt-1 block text-2xl text-white">{weekSummary.completed}</strong>
          </div>
        </div>
      </section>

      {error && <p className="rounded-2xl border border-red-500/40 bg-red-950/30 p-4 text-sm text-red-100">{error}</p>}
      {message && <p className="rounded-2xl border border-mo-primary/40 bg-mo-primary/10 p-4 text-sm text-mo-primary">{message}</p>}

      <section className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h3 className="text-lg font-semibold text-white">Sessões da semana</h3>
            <button onClick={newSession} className="rounded-2xl bg-mo-primary px-4 py-2 text-sm font-semibold text-black">
              Nova sessão
            </button>
          </div>

          {sessions.length === 0 ? (
            <p className="rounded-2xl border border-dashed border-mo-border p-4 text-sm text-mo-muted">
              Nenhuma sessão planejada para esta semana.
            </p>
          ) : (
            <div className="space-y-3">
              {sessions.map((session) => {
                const active = selectedSessionId === session.id
                return (
                  <button
                    key={session.id}
                    onClick={() => selectSession(session)}
                    className={`w-full rounded-2xl border p-4 text-left transition ${
                      active ? 'border-mo-primary bg-mo-primary/10' : 'border-mo-border bg-white/[0.03] hover:border-mo-primary/40'
                    }`}
                  >
                    <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                      <div>
                        <p className="text-sm text-mo-muted">{formatDate(session.scheduled_date)} · {translateSessionType(session.session_type)}</p>
                        <h4 className="mt-1 font-semibold text-white">{session.title}</h4>
                        {session.notes && <p className="mt-1 text-sm text-mo-muted">{session.notes}</p>}
                      </div>
                      <span className="rounded-full bg-white/5 px-3 py-1 text-xs text-mo-muted">{translateStatus(session.status)}</span>
                    </div>
                  </button>
                )
              })}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <form onSubmit={saveSession} className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm text-mo-muted">{selectedSessionId ? 'Editar sessão' : 'Criar sessão'}</p>
                <h3 className="mt-1 text-lg font-semibold text-white">{selectedSessionId ? selectedSession?.title : 'Nova sessão'}</h3>
              </div>
              {selectedSessionId && (
                <button type="button" onClick={deleteSession} className="rounded-xl border border-red-500/40 px-3 py-2 text-xs text-red-200">
                  Excluir
                </button>
              )}
            </div>

            <div className="mt-5 space-y-4">
              <label className="block text-sm text-mo-muted">
                Título
                <input
                  value={sessionForm.title}
                  onChange={(event) => setSessionForm((current) => ({ ...current, title: event.target.value }))}
                  className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                />
              </label>

              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm text-mo-muted">
                  Tipo
                  <select
                    value={sessionForm.sessionType}
                    disabled={Boolean(selectedSessionId)}
                    onChange={(event) =>
                      setSessionForm((current) => ({ ...current, sessionType: event.target.value as SessionForm['sessionType'] }))
                    }
                    className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary disabled:opacity-60"
                  >
                    {typeOptions.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>

                <label className="block text-sm text-mo-muted">
                  Data
                  <input
                    type="date"
                    value={sessionForm.scheduledDate}
                    onChange={(event) => setSessionForm((current) => ({ ...current, scheduledDate: event.target.value }))}
                    className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                  />
                </label>
              </div>

              <label className="block text-sm text-mo-muted">
                Observações
                <textarea
                  value={sessionForm.notes}
                  onChange={(event) => setSessionForm((current) => ({ ...current, notes: event.target.value }))}
                  rows={3}
                  className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                />
              </label>
            </div>

            <button disabled={saving} className="mt-5 w-full rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black disabled:opacity-60">
              {saving ? 'Salvando...' : selectedSessionId ? 'Salvar alterações' : 'Criar sessão'}
            </button>
          </form>

          {selectedSessionId && (
            <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Status rápido</h3>
              <div className="mt-4 flex flex-wrap gap-2">
                {statusOptions.map((status) => (
                  <button
                    key={status}
                    onClick={() => updateStatus(status)}
                    className="rounded-xl border border-mo-border px-3 py-2 text-xs text-mo-muted hover:border-mo-primary hover:text-white"
                  >
                    {translateStatus(status)}
                  </button>
                ))}
              </div>
            </section>
          )}

          {selectedSession?.session_type === 'strength' && (
            <form onSubmit={addExercise} className="rounded-3xl border border-mo-border bg-mo-surface p-5">
              <h3 className="text-lg font-semibold text-white">Adicionar exercício</h3>
              <p className="mt-1 text-sm text-mo-muted">Monte o treino planejado a partir da biblioteca.</p>

              <div className="mt-5 space-y-4">
                <label className="block text-sm text-mo-muted">
                  Exercício
                  <select
                    value={exerciseForm.exerciseId}
                    onChange={(event) => setExerciseForm((current) => ({ ...current, exerciseId: event.target.value }))}
                    className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                  >
                    <option value="">Selecione</option>
                    {exercises.map((exercise) => (
                      <option key={exercise.id} value={exercise.id}>{exercise.name}</option>
                    ))}
                  </select>
                </label>

                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="block text-sm text-mo-muted">
                    Séries
                    <input
                      type="number"
                      min="1"
                      value={exerciseForm.plannedSets}
                      onChange={(event) => setExerciseForm((current) => ({ ...current, plannedSets: event.target.value }))}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                    />
                  </label>
                  <label className="block text-sm text-mo-muted">
                    Repetições
                    <input
                      value={exerciseForm.plannedReps}
                      onChange={(event) => setExerciseForm((current) => ({ ...current, plannedReps: event.target.value }))}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                    />
                  </label>
                  <label className="block text-sm text-mo-muted">
                    Carga planejada
                    <input
                      type="number"
                      min="0"
                      step="0.5"
                      value={exerciseForm.plannedLoad}
                      onChange={(event) => setExerciseForm((current) => ({ ...current, plannedLoad: event.target.value }))}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                    />
                  </label>
                  <label className="block text-sm text-mo-muted">
                    Descanso (s)
                    <input
                      type="number"
                      min="0"
                      value={exerciseForm.restSeconds}
                      onChange={(event) => setExerciseForm((current) => ({ ...current, restSeconds: event.target.value }))}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                    />
                  </label>
                </div>

                <label className="block text-sm text-mo-muted">
                  Notas do exercício
                  <input
                    value={exerciseForm.notes}
                    onChange={(event) => setExerciseForm((current) => ({ ...current, notes: event.target.value }))}
                    className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                  />
                </label>
              </div>

              <button disabled={saving || !exerciseForm.exerciseId} className="mt-5 w-full rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black disabled:opacity-60">
                Adicionar ao treino
              </button>

              <div className="mt-5 space-y-2">
                {(selectedSession.strength_exercises ?? []).map((item) => (
                  <div key={item.id} className="rounded-2xl bg-white/[0.03] p-3 text-sm text-mo-muted">
                    <span className="font-semibold text-white">{item.order_index}. {item.exercise?.name ?? `Exercício #${item.exercise_id}`}</span>
                    <span> · {item.planned_sets}x {item.planned_reps}</span>
                  </div>
                ))}
              </div>
            </form>
          )}
        </div>
      </section>
    </main>
  )
}

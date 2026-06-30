import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { ProgressBar } from '../components/ProgressBar'
import type { AdaptationSuggestion, StrengthWorkoutExercise, TrainingSession } from '../types/api'
import { formatDate, translateStatus } from '../utils/format'

type SetForm = {
  reps: string
  load: string
  rir: string
  rpe: string
}

type AlternativesState = {
  workoutExercise: StrengthWorkoutExercise
  alternatives: AdaptationSuggestion[]
  mode: 'default' | 'all'
} | null

const defaultSetForm: SetForm = {
  reps: '10',
  load: '0',
  rir: '2',
  rpe: '8',
}

const statusStyle: Record<string, string> = {
  planned: 'border-blue-400/40 bg-blue-950/20 text-blue-100',
  in_progress: 'border-mo-primary/50 bg-mo-primary/10 text-mo-primary',
  completed: 'border-emerald-400/40 bg-emerald-950/20 text-emerald-100',
  adapted: 'border-amber-400/40 bg-amber-950/20 text-amber-100',
}

export function WorkoutPage() {
  const [sessionId, setSessionId] = useState(1)
  const [session, setSession] = useState<TrainingSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [forms, setForms] = useState<Record<number, SetForm>>({})
  const [alternativesState, setAlternativesState] = useState<AlternativesState>(null)

  async function loadSession(id = sessionId) {
    try {
      setLoading(true)
      const data = await mo2logApi.trainingSession(id)
      setSession(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar treino')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSession(sessionId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId])

  const strengthExercises = session?.strength_exercises ?? []
  const completedSets = useMemo(
    () => strengthExercises.reduce((total, item) => total + (item.set_logs?.length ?? 0), 0),
    [strengthExercises],
  )
  const plannedSets = useMemo(
    () => strengthExercises.reduce((total, item) => total + item.planned_sets, 0),
    [strengthExercises],
  )
  const progress = plannedSets ? Math.round((completedSets / plannedSets) * 100) : 0

  async function startSession() {
    if (!session) return
    setSaving(true)
    try {
      const updated = await mo2logApi.startTrainingSession(session.id)
      setSession(updated)
      setMessage('Treino iniciado.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao iniciar treino')
    } finally {
      setSaving(false)
    }
  }

  async function finishSession() {
    if (!session) return
    setSaving(true)
    try {
      const updated = await mo2logApi.finishTrainingSession(session.id)
      setSession(updated)
      setMessage('Treino finalizado com sucesso.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao finalizar treino')
    } finally {
      setSaving(false)
    }
  }

  async function saveSet(workoutExercise: StrengthWorkoutExercise) {
    const form = forms[workoutExercise.id] ?? defaultSetForm
    const nextSetNumber = (workoutExercise.set_logs?.length ?? 0) + 1

    setSaving(true)
    try {
      await mo2logApi.createSetLog({
        strength_workout_exercise_id: workoutExercise.id,
        set_number: nextSetNumber,
        reps: Number(form.reps),
        load: Number(form.load),
        rir: form.rir === '' ? null : Number(form.rir),
        rpe: form.rpe === '' ? null : Number(form.rpe),
      })
      setMessage(`Série ${nextSetNumber} registrada para ${workoutExercise.exercise?.name ?? 'exercício'}.`)
      await loadSession(session?.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao registrar série')
    } finally {
      setSaving(false)
    }
  }

  async function loadAlternatives(workoutExercise: StrengthWorkoutExercise, mode: 'default' | 'all' = 'default') {
    if (!workoutExercise.exercise_id) return
    setSaving(true)
    try {
      const alternatives = await mo2logApi.adaptationSuggestions(workoutExercise.exercise_id, 1, mode, 'equipment_busy')
      setAlternativesState({ workoutExercise, alternatives, mode })
      setMessage(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao buscar alternativas')
    } finally {
      setSaving(false)
    }
  }

  async function swapExercise(alternative: AdaptationSuggestion) {
    if (!session || !alternativesState) return
    setSaving(true)
    try {
      const result = await mo2logApi.swapExercise(session.id, {
        strength_workout_exercise_id: alternativesState.workoutExercise.id,
        original_exercise_id: alternativesState.workoutExercise.exercise_id,
        new_exercise_id: alternative.alternative_exercise_id,
        reason: alternative.equipment_status === 'unavailable' ? 'equipment_unavailable' : 'equipment_busy',
      })
      setMessage(`${result.message} Equivalência: ${result.equivalence_score ?? 'N/A'}%.`)
      setAlternativesState(null)
      await loadSession(session.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao trocar exercício')
    } finally {
      setSaving(false)
    }
  }

  function updateForm(id: number, field: keyof SetForm, value: string) {
    setForms((current) => ({
      ...current,
      [id]: {
        ...(current[id] ?? defaultSetForm),
        [field]: value,
      },
    }))
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Execução de treino</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">
              {session?.title ?? 'Carregando treino...'}
            </h2>
            <p className="mt-2 text-mo-muted">
              {session ? `${formatDate(session.scheduled_date)} · ${translateStatus(session.status)}` : 'Buscando sessão'}
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <label className="flex items-center gap-2 rounded-2xl border border-mo-border bg-white/[0.03] px-4 py-2 text-sm text-mo-muted">
              Sessão
              <input
                value={sessionId}
                onChange={(event) => setSessionId(Number(event.target.value || 1))}
                className="w-16 rounded-xl border border-mo-border bg-black/30 px-3 py-1 text-white outline-none focus:border-mo-primary"
                type="number"
                min={1}
              />
            </label>
            <button
              onClick={startSession}
              disabled={!session || saving || session.status === 'completed'}
              className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-40"
            >
              Iniciar
            </button>
            <button
              onClick={finishSession}
              disabled={!session || saving}
              className="rounded-2xl border border-mo-border px-5 py-3 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              Finalizar
            </button>
          </div>
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-[1fr_280px]">
          <ProgressBar value={progress} label={`Séries registradas: ${completedSets}/${plannedSets}`} />
          <div className={`rounded-2xl border px-4 py-3 text-sm ${statusStyle[session?.status ?? ''] ?? 'border-mo-border bg-white/[0.03] text-mo-muted'}`}>
            Status atual: <strong>{translateStatus(session?.status ?? 'planned')}</strong>
          </div>
        </div>
      </section>

      {error && (
        <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>
      )}
      {message && (
        <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{message}</div>
      )}

      {loading ? (
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">Carregando treino...</section>
      ) : strengthExercises.length === 0 ? (
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">
          Esta sessão não possui exercícios de musculação. Use a sessão 1 para testar o treino seedado.
        </section>
      ) : (
        <section className="grid gap-4">
          {strengthExercises.map((item) => {
            const form = forms[item.id] ?? defaultSetForm
            return (
              <article key={item.id} className="rounded-3xl border border-mo-border bg-mo-surface p-5">
                <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-mo-primary/10 px-3 py-1 text-xs font-semibold text-mo-primary">
                        #{item.order_index}
                      </span>
                      <h3 className="text-xl font-semibold text-white">{item.exercise?.name ?? `Exercício ${item.exercise_id}`}</h3>
                    </div>
                    <p className="mt-2 text-sm text-mo-muted">
                      Planejado: {item.planned_sets} séries · {item.planned_reps} reps
                      {item.planned_load ? ` · ${item.planned_load} kg` : ''}
                      {item.rest_seconds ? ` · descanso ${item.rest_seconds}s` : ''}
                    </p>
                    {item.notes && <p className="mt-2 text-sm text-mo-muted">{item.notes}</p>}
                  </div>

                  <button
                    onClick={() => loadAlternatives(item)}
                    className="rounded-2xl border border-mo-border px-4 py-2 text-sm font-semibold text-white hover:border-mo-primary"
                  >
                    Trocar exercício
                  </button>
                </div>

                <div className="mt-5 grid gap-3 md:grid-cols-4">
                  <label className="text-sm text-mo-muted">
                    Reps
                    <input
                      value={form.reps}
                      onChange={(event) => updateForm(item.id, 'reps', event.target.value)}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                      type="number"
                      min={0}
                    />
                  </label>
                  <label className="text-sm text-mo-muted">
                    Carga
                    <input
                      value={form.load}
                      onChange={(event) => updateForm(item.id, 'load', event.target.value)}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                      type="number"
                      min={0}
                      step="0.5"
                    />
                  </label>
                  <label className="text-sm text-mo-muted">
                    RIR
                    <input
                      value={form.rir}
                      onChange={(event) => updateForm(item.id, 'rir', event.target.value)}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                      type="number"
                      min={0}
                      max={10}
                    />
                  </label>
                  <label className="text-sm text-mo-muted">
                    RPE
                    <input
                      value={form.rpe}
                      onChange={(event) => updateForm(item.id, 'rpe', event.target.value)}
                      className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                      type="number"
                      min={0}
                      max={10}
                    />
                  </label>
                </div>

                <div className="mt-4 flex flex-wrap items-center gap-3">
                  <button
                    onClick={() => saveSet(item)}
                    disabled={saving}
                    className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:opacity-50"
                  >
                    Registrar próxima série
                  </button>
                  <span className="text-sm text-mo-muted">
                    Registradas: {item.set_logs?.length ?? 0}/{item.planned_sets}
                  </span>
                </div>

                {!!item.set_logs?.length && (
                  <div className="mt-5 overflow-hidden rounded-2xl border border-mo-border">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-white/[0.03] text-mo-muted">
                        <tr>
                          <th className="px-4 py-3">Série</th>
                          <th className="px-4 py-3">Reps</th>
                          <th className="px-4 py-3">Carga</th>
                          <th className="px-4 py-3">RIR</th>
                          <th className="px-4 py-3">RPE</th>
                        </tr>
                      </thead>
                      <tbody>
                        {item.set_logs.map((setLog) => (
                          <tr key={setLog.id} className="border-t border-mo-border text-white">
                            <td className="px-4 py-3">{setLog.set_number}</td>
                            <td className="px-4 py-3">{setLog.reps}</td>
                            <td className="px-4 py-3">{setLog.load} kg</td>
                            <td className="px-4 py-3">{setLog.rir ?? '-'}</td>
                            <td className="px-4 py-3">{setLog.rpe ?? '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </article>
            )
          })}
        </section>
      )}

      {alternativesState && (
        <section className="fixed inset-0 z-20 grid place-items-center bg-black/70 p-4 backdrop-blur-sm">
          <div className="max-h-[85vh] w-full max-w-3xl overflow-auto rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
              <div>
                <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Trocar exercício</p>
                <h3 className="mt-2 text-2xl font-bold text-white">
                  {alternativesState.workoutExercise.exercise?.name}
                </h3>
                <p className="mt-2 text-sm text-mo-muted">
                  Por padrão, exercícios com equipamento indisponível ficam ocultos. Em “ver todas”, eles aparecem com aviso.
                </p>
              </div>
              <button onClick={() => setAlternativesState(null)} className="rounded-2xl border border-mo-border px-4 py-2 text-white">
                Fechar
              </button>
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                onClick={() => loadAlternatives(alternativesState.workoutExercise, 'default')}
                className={`rounded-2xl px-4 py-2 text-sm font-semibold ${
                  alternativesState.mode === 'default' ? 'bg-mo-primary text-black' : 'border border-mo-border text-white'
                }`}
              >
                Sugestões padrão
              </button>
              <button
                onClick={() => loadAlternatives(alternativesState.workoutExercise, 'all')}
                className={`rounded-2xl px-4 py-2 text-sm font-semibold ${
                  alternativesState.mode === 'all' ? 'bg-mo-primary text-black' : 'border border-mo-border text-white'
                }`}
              >
                Ver todas
              </button>
            </div>

            <div className="mt-5 space-y-3">
              {alternativesState.alternatives.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-mo-border p-4 text-mo-muted">
                  Nenhuma alternativa encontrada para este exercício.
                </p>
              ) : (
                alternativesState.alternatives.map((alternative) => (
                  <article key={alternative.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                    <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
                      <div>
                        <h4 className="font-semibold text-white">{alternative.alternative_exercise.name}</h4>
                        <p className="mt-1 text-sm text-mo-muted">
                          Equivalência {alternative.equivalence_score}% · Score {alternative.recommendation_score}% · {alternative.reason ?? 'Substituição compatível'}
                        </p>
                        {alternative.equipment_status === 'unavailable' && (
                          <p className="mt-2 inline-flex rounded-full border border-red-400/40 bg-red-950/30 px-3 py-1 text-xs text-red-100">
                            Equipamento indisponível na sua academia
                          </p>
                        )}
                        {alternative.equipment_status === 'frequently_busy' && (
                          <p className="mt-2 inline-flex rounded-full border border-amber-400/40 bg-amber-950/30 px-3 py-1 text-xs text-amber-100">
                            Equipamento frequentemente ocupado
                          </p>
                        )}
                        {alternative.equipment_status === 'favorite' && (
                          <p className="mt-2 inline-flex rounded-full border border-mo-primary/40 bg-mo-primary/10 px-3 py-1 text-xs text-mo-primary">
                            Equipamento favorito
                          </p>
                        )}
                      </div>
                      <button
                        onClick={() => swapExercise(alternative)}
                        disabled={saving}
                        className="rounded-2xl bg-mo-primary px-4 py-2 text-sm font-semibold text-black disabled:opacity-50"
                      >
                        Usar este
                      </button>
                    </div>
                  </article>
                ))
              )}
            </div>
          </div>
        </section>
      )}
    </main>
  )
}

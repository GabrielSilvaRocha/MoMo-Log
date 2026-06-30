import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import type { AdaptationSuggestion, Exercise } from '../types/api'

type Reason = 'equipment_busy' | 'equipment_unavailable' | 'pain_discomfort' | 'preference' | 'manual_adjustment'

const reasons: Array<{ value: Reason; label: string }> = [
  { value: 'equipment_busy', label: 'Equipamento ocupado' },
  { value: 'equipment_unavailable', label: 'Equipamento indisponível' },
  { value: 'pain_discomfort', label: 'Dor ou desconforto' },
  { value: 'preference', label: 'Preferência pessoal' },
  { value: 'manual_adjustment', label: 'Ajuste manual' },
]

function scoreStyle(score: number) {
  if (score >= 90) return 'border-mo-primary/50 bg-mo-primary/10 text-mo-primary'
  if (score >= 75) return 'border-blue-400/40 bg-blue-950/20 text-blue-100'
  if (score >= 60) return 'border-amber-400/40 bg-amber-950/20 text-amber-100'
  return 'border-red-400/40 bg-red-950/20 text-red-100'
}

export function AdaptationPage() {
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [exerciseId, setExerciseId] = useState<number>(1)
  const [mode, setMode] = useState<'default' | 'all'>('default')
  const [reason, setReason] = useState<Reason>('equipment_busy')
  const [suggestions, setSuggestions] = useState<AdaptationSuggestion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function loadExercises() {
    try {
      const data = await mo2logApi.exercises()
      setExercises(data)
      if (data.length > 0) setExerciseId(data[0].id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar exercícios')
    }
  }

  async function loadSuggestions(targetExerciseId = exerciseId) {
    try {
      setLoading(true)
      const data = await mo2logApi.adaptationSuggestions(targetExerciseId, 1, mode, reason)
      setSuggestions(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar sugestões')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadExercises()
  }, [])

  useEffect(() => {
    if (exerciseId) loadSuggestions(exerciseId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [exerciseId, mode, reason])

  const selectedExercise = useMemo(
    () => exercises.find((exercise) => exercise.id === exerciseId),
    [exerciseId, exercises],
  )

  const defaultCount = suggestions.filter((item) => item.is_default_suggestion).length
  const unavailableCount = suggestions.filter((item) => item.equipment_status === 'unavailable').length

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Adaptation Engine</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Troca inteligente de exercícios</h2>
        <p className="mt-3 max-w-3xl text-mo-muted">
          O motor de adaptação ranqueia alternativas considerando equivalência, equipamentos da sua academia,
          máquinas inexistentes, equipamentos sempre ocupados e motivo da troca.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}

      <section className="grid gap-4 lg:grid-cols-4">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5 lg:col-span-2">
          <label className="text-sm font-semibold text-white">Exercício original</label>
          <select
            value={exerciseId}
            onChange={(event) => setExerciseId(Number(event.target.value))}
            className="mt-3 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
          >
            {exercises.map((exercise) => (
              <option key={exercise.id} value={exercise.id}>{exercise.name}</option>
            ))}
          </select>
          <p className="mt-3 text-sm text-mo-muted">
            Selecionado: <span className="text-white">{selectedExercise?.name ?? '—'}</span>
          </p>
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <label className="text-sm font-semibold text-white">Motivo</label>
          <select
            value={reason}
            onChange={(event) => setReason(event.target.value as Reason)}
            className="mt-3 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
          >
            {reasons.map((item) => (
              <option key={item.value} value={item.value}>{item.label}</option>
            ))}
          </select>
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <label className="text-sm font-semibold text-white">Modo</label>
          <select
            value={mode}
            onChange={(event) => setMode(event.target.value as 'default' | 'all')}
            className="mt-3 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
          >
            <option value="default">Sugestões padrão</option>
            <option value="all">Ver todas</option>
          </select>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <p className="text-sm text-mo-muted">Sugestões exibidas</p>
          <strong className="mt-2 block text-3xl text-white">{suggestions.length}</strong>
        </div>
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <p className="text-sm text-mo-muted">Padrão</p>
          <strong className="mt-2 block text-3xl text-mo-primary">{defaultCount}</strong>
        </div>
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <p className="text-sm text-mo-muted">Indisponíveis visíveis</p>
          <strong className="mt-2 block text-3xl text-amber-200">{unavailableCount}</strong>
        </div>
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
          <div>
            <h3 className="text-lg font-semibold text-white">Ranking de substituições</h3>
            <p className="mt-1 text-sm text-mo-muted">Pontuação final = equivalência ajustada pelo contexto da sua academia.</p>
          </div>
          <button
            onClick={() => loadSuggestions(exerciseId)}
            className="rounded-2xl border border-mo-border px-4 py-2 text-sm font-semibold text-white hover:border-mo-primary"
          >
            Atualizar
          </button>
        </div>

        {loading ? (
          <p className="mt-6 text-mo-muted">Calculando sugestões...</p>
        ) : suggestions.length === 0 ? (
          <p className="mt-6 text-mo-muted">Nenhuma sugestão encontrada para este exercício.</p>
        ) : (
          <div className="mt-5 grid gap-4">
            {suggestions.map((suggestion) => (
              <article key={suggestion.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="text-lg font-semibold text-white">{suggestion.alternative_exercise.name}</h4>
                      {suggestion.badges.map((badge) => (
                        <span key={badge} className="rounded-full bg-white/5 px-3 py-1 text-xs text-mo-muted">{badge}</span>
                      ))}
                    </div>
                    <p className="mt-2 text-sm text-mo-muted">{suggestion.recommendation_label}</p>
                    <p className="mt-2 text-xs text-mo-muted">
                      Equipamentos: {suggestion.equipment_names.length ? suggestion.equipment_names.join(', ') : 'Não informado'}
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-2 text-sm">
                    <span className="rounded-full border border-mo-border px-3 py-1 text-mo-muted">Equiv. {suggestion.equivalence_score}%</span>
                    <span className={`rounded-full border px-3 py-1 ${scoreStyle(suggestion.recommendation_score)}`}>
                      Score {suggestion.recommendation_score}%
                    </span>
                  </div>
                </div>

                {(suggestion.penalties.length > 0 || suggestion.bonuses.length > 0) && (
                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    {suggestion.penalties.length > 0 && (
                      <div className="rounded-2xl border border-amber-400/20 bg-amber-950/10 p-3 text-sm text-amber-100">
                        <strong>Penalidades</strong>
                        <ul className="mt-2 list-disc space-y-1 pl-5">
                          {suggestion.penalties.map((item) => <li key={item}>{item}</li>)}
                        </ul>
                      </div>
                    )}
                    {suggestion.bonuses.length > 0 && (
                      <div className="rounded-2xl border border-mo-primary/20 bg-mo-primary/10 p-3 text-sm text-mo-primary">
                        <strong>Bônus</strong>
                        <ul className="mt-2 list-disc space-y-1 pl-5">
                          {suggestion.bonuses.map((item) => <li key={item}>{item}</li>)}
                        </ul>
                      </div>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

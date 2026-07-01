import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { EmptyState } from '../components/EmptyState'
import { LoadingState } from '../components/LoadingState'
import { SectionHeader } from '../components/SectionHeader'
import type { Exercise, WorkoutTemplate, WorkoutTemplateExerciseCreatePayload } from '../types/api'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

type TemplateFormState = {
  name: string
  description: string
  goal: string
  difficulty: 'beginner' | 'intermediate' | 'advanced'
  estimatedDurationMinutes: string
  exercises: WorkoutTemplateExerciseCreatePayload[]
}

const emptyTemplateForm: TemplateFormState = {
  name: '',
  description: '',
  goal: 'hipertrofia',
  difficulty: 'intermediate',
  estimatedDurationMinutes: '45',
  exercises: [],
}

const difficultyLabels: Record<TemplateFormState['difficulty'], string> = {
  beginner: 'Iniciante',
  intermediate: 'Intermediário',
  advanced: 'Avançado',
}

export function TemplatesPage() {
  const [templates, setTemplates] = useState<WorkoutTemplate[]>([])
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [scheduledDate, setScheduledDate] = useState(todayIso())
  const [form, setForm] = useState<TemplateFormState>(emptyTemplateForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadTemplates() {
    setLoading(true)
    setError(null)
    try {
      const [templateData, exerciseData] = await Promise.all([
        mo2logApi.workoutTemplates(),
        mo2logApi.exercises(),
      ])
      setTemplates(templateData)
      setExercises(exerciseData)
      setSelectedTemplateId((current) => current ?? templateData[0]?.id ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível carregar os templates.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTemplates()
  }, [])

  const selectedTemplate = templates.find((template) => template.id === selectedTemplateId) ?? templates[0]
  const exerciseOptions = useMemo(() => exercises.filter((item) => item.exercise_type === 'strength'), [exercises])

  function updateTemplateExercise(index: number, updates: Partial<WorkoutTemplateExerciseCreatePayload>) {
    setForm((current) => ({
      ...current,
      exercises: current.exercises.map((item, itemIndex) => itemIndex === index ? { ...item, ...updates } : item),
    }))
  }

  function addExerciseToTemplate() {
    const firstExercise = exerciseOptions[0] ?? exercises[0]
    if (!firstExercise) {
      setError('Cadastre exercícios antes de criar um template.')
      return
    }

    setForm((current) => ({
      ...current,
      exercises: [
        ...current.exercises,
        {
          exercise_id: firstExercise.id,
          planned_sets: 3,
          planned_reps: '8-12',
          rest_seconds: 90,
          notes: '',
        },
      ],
    }))
  }

  function removeExerciseFromTemplate(index: number) {
    setForm((current) => ({
      ...current,
      exercises: current.exercises.filter((_, itemIndex) => itemIndex !== index),
    }))
  }

  async function handleCreateTemplate() {
    setMessage(null)
    setError(null)
    if (!form.name.trim()) {
      setError('Informe um nome para o template.')
      return
    }
    if (!form.exercises.length) {
      setError('Adicione pelo menos um exercício ao template.')
      return
    }

    try {
      setSaving(true)
      const created = await mo2logApi.createWorkoutTemplate({
        user_id: getCurrentUserId(),
        name: form.name.trim(),
        description: form.description.trim() || null,
        goal: form.goal.trim() || null,
        difficulty: form.difficulty,
        estimated_duration_minutes: form.estimatedDurationMinutes ? Number(form.estimatedDurationMinutes) : null,
        exercises: form.exercises.map((item) => ({
          ...item,
          planned_sets: Number(item.planned_sets),
          rest_seconds: item.rest_seconds === null || item.rest_seconds === undefined ? null : Number(item.rest_seconds),
          notes: item.notes?.trim() || null,
        })),
      })
      setTemplates((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)))
      setSelectedTemplateId(created.id)
      setForm(emptyTemplateForm)
      setMessage(`Template "${created.name}" criado com ${created.exercises.length} exercícios.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível criar o template.')
    } finally {
      setSaving(false)
    }
  }

  async function handleArchiveTemplate(template: WorkoutTemplate) {
    setMessage(null)
    setError(null)
    try {
      await mo2logApi.archiveWorkoutTemplate(template.id)
      setTemplates((current) => current.filter((item) => item.id !== template.id))
      setSelectedTemplateId((current) => current === template.id ? null : current)
      setMessage(`Template "${template.name}" arquivado.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível arquivar o template.')
    }
  }

  async function handleSchedule(templateId: number) {
    setMessage(null)
    setError(null)
    try {
      const response = await mo2logApi.scheduleWorkoutTemplate(templateId, {
        user_id: getCurrentUserId(),
        scheduled_date: scheduledDate,
        notes: 'Sessão criada pela tela Templates.',
      })
      setMessage(`${response.message} Sessão #${response.session.id}: ${response.session.title}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível agendar o template.')
    }
  }

  if (loading) return <LoadingState title="Carregando templates de treino..." description="Preparando seus treinos reutilizáveis." />

  return (
    <div className="space-y-6">
      <SectionHeader
        eyebrow="Workout Builder"
        title="Templates de treino"
        description="Crie templates personalizados, organize exercícios e transforme qualquer template em uma sessão planejada."
      />

      {error && <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}
      {message && <div className="rounded-2xl border border-mo-primary/40 bg-mo-primary/10 p-4 text-sm text-mo-primary">{message}</div>}

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <div className="flex flex-col justify-between gap-3 md:flex-row md:items-start">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-mo-primary">Novo template</p>
            <h2 className="mt-2 text-2xl font-bold text-white">Monte um treino reutilizável</h2>
          </div>
          <button
            onClick={handleCreateTemplate}
            disabled={saving}
            className="rounded-2xl bg-mo-primary px-5 py-3 text-sm font-bold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? 'Salvando...' : 'Salvar template'}
          </button>
        </div>

        <div className="mt-5 grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-sm text-mo-muted">
              Nome
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              />
            </label>
            <label className="block text-sm text-mo-muted">
              Objetivo
              <input
                value={form.goal}
                onChange={(event) => setForm((current) => ({ ...current, goal: event.target.value }))}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              />
            </label>
            <label className="block text-sm text-mo-muted">
              Dificuldade
              <select
                value={form.difficulty}
                onChange={(event) => setForm((current) => ({ ...current, difficulty: event.target.value as TemplateFormState['difficulty'] }))}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              >
                {Object.entries(difficultyLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="block text-sm text-mo-muted">
              Duração estimada
              <input
                type="number"
                min={5}
                max={240}
                value={form.estimatedDurationMinutes}
                onChange={(event) => setForm((current) => ({ ...current, estimatedDurationMinutes: event.target.value }))}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              />
            </label>
            <label className="block text-sm text-mo-muted sm:col-span-2">
              Descrição
              <textarea
                value={form.description}
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                rows={3}
                className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              />
            </label>
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between gap-3">
              <h3 className="font-semibold text-white">Exercícios do template</h3>
              <button onClick={addExerciseToTemplate} className="rounded-2xl border border-mo-primary px-4 py-2 text-sm font-semibold text-mo-primary">
                Adicionar exercício
              </button>
            </div>
            {form.exercises.length === 0 ? (
              <EmptyState title="Nenhum exercício adicionado" description="Adicione exercícios da biblioteca para salvar o template." />
            ) : (
              <div className="space-y-3">
                {form.exercises.map((item, index) => (
                  <div key={index} className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <div className="grid gap-3 md:grid-cols-[1fr_0.5fr_0.6fr_0.5fr_auto] md:items-end">
                      <label className="block text-sm text-mo-muted">
                        Exercício #{index + 1}
                        <select
                          value={item.exercise_id}
                          onChange={(event) => updateTemplateExercise(index, { exercise_id: Number(event.target.value) })}
                          className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                        >
                          {exerciseOptions.map((exercise) => <option key={exercise.id} value={exercise.id}>{exercise.name}</option>)}
                        </select>
                      </label>
                      <label className="block text-sm text-mo-muted">
                        Séries
                        <input
                          type="number"
                          min={1}
                          value={item.planned_sets}
                          onChange={(event) => updateTemplateExercise(index, { planned_sets: Number(event.target.value) })}
                          className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                        />
                      </label>
                      <label className="block text-sm text-mo-muted">
                        Reps
                        <input
                          value={item.planned_reps}
                          onChange={(event) => updateTemplateExercise(index, { planned_reps: event.target.value })}
                          className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                        />
                      </label>
                      <label className="block text-sm text-mo-muted">
                        Descanso
                        <input
                          type="number"
                          min={0}
                          value={item.rest_seconds ?? 0}
                          onChange={(event) => updateTemplateExercise(index, { rest_seconds: Number(event.target.value) })}
                          className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                        />
                      </label>
                      <button onClick={() => removeExerciseFromTemplate(index)} className="rounded-2xl border border-red-500/40 px-4 py-3 text-sm font-semibold text-red-200">
                        Remover
                      </button>
                    </div>
                    <input
                      value={item.notes ?? ''}
                      onChange={(event) => updateTemplateExercise(index, { notes: event.target.value })}
                      placeholder="Observações"
                      className="mt-3 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-sm text-white outline-none focus:border-mo-primary"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>

      {templates.length === 0 ? (
        <EmptyState title="Nenhum template encontrado" description="Quando você criar templates de treino, eles aparecerão aqui." />
      ) : (
        <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <section className="space-y-4">
            {templates.map((template) => (
              <button
                key={template.id}
                onClick={() => setSelectedTemplateId(template.id)}
                className={`w-full rounded-3xl border p-5 text-left transition ${
                  selectedTemplate?.id === template.id
                    ? 'border-mo-primary bg-mo-primary/10 shadow-glow'
                    : 'border-mo-border bg-mo-surface hover:border-mo-primary/50'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-bold text-white">{template.name}</h3>
                    <p className="mt-1 text-sm text-mo-muted">{template.description}</p>
                  </div>
                  <span className="rounded-full border border-mo-border px-3 py-1 text-xs text-mo-muted">
                    {template.estimated_duration_minutes ?? 0} min
                  </span>
                </div>
                <div className="mt-4 flex flex-wrap gap-2 text-xs text-mo-muted">
                  <span className="rounded-full bg-white/5 px-3 py-1">{template.goal ?? 'geral'}</span>
                  <span className="rounded-full bg-white/5 px-3 py-1">{difficultyLabels[template.difficulty as TemplateFormState['difficulty']] ?? template.difficulty}</span>
                  <span className="rounded-full bg-white/5 px-3 py-1">{template.exercises.length} exercícios</span>
                </div>
              </button>
            ))}
          </section>

          {selectedTemplate && (
            <section className="rounded-3xl border border-mo-border bg-mo-surface p-6">
              <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
                <div>
                  <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Template selecionado</p>
                  <h2 className="mt-2 text-2xl font-bold text-white">{selectedTemplate.name}</h2>
                  <p className="mt-2 text-sm text-mo-muted">{selectedTemplate.description}</p>
                </div>
                <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
                  <label className="text-xs uppercase tracking-[0.25em] text-mo-muted">Data</label>
                  <input
                    type="date"
                    value={scheduledDate}
                    onChange={(event) => setScheduledDate(event.target.value)}
                    className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
                  />
                  <button
                    onClick={() => handleSchedule(selectedTemplate.id)}
                    className="mt-3 w-full rounded-2xl bg-mo-primary px-4 py-3 text-sm font-bold text-black shadow-glow"
                  >
                    Criar sessão
                  </button>
                  <button
                    onClick={() => handleArchiveTemplate(selectedTemplate)}
                    className="mt-3 w-full rounded-2xl border border-red-500/40 px-4 py-3 text-sm font-bold text-red-200"
                  >
                    Arquivar template
                  </button>
                </div>
              </div>

              <div className="mt-6 space-y-3">
                {selectedTemplate.exercises.map((item) => (
                  <div key={item.id} className="rounded-2xl border border-mo-border bg-black/20 p-4">
                    <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                      <div>
                        <p className="text-sm text-mo-muted">#{item.order_index}</p>
                        <h3 className="font-semibold text-white">{item.exercise?.name ?? `Exercício ${item.exercise_id}`}</h3>
                        <p className="text-sm text-mo-muted">{item.notes}</p>
                      </div>
                      <div className="flex flex-wrap gap-2 text-xs text-mo-muted">
                        <span className="rounded-full bg-white/5 px-3 py-1">{item.planned_sets} séries</span>
                        <span className="rounded-full bg-white/5 px-3 py-1">{item.planned_reps} reps</span>
                        <span className="rounded-full bg-white/5 px-3 py-1">{item.rest_seconds ?? 90}s descanso</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  )
}

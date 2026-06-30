import { useEffect, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { EmptyState } from '../components/EmptyState'
import { LoadingState } from '../components/LoadingState'
import { SectionHeader } from '../components/SectionHeader'
import type { WorkoutTemplate } from '../types/api'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export function TemplatesPage() {
  const [templates, setTemplates] = useState<WorkoutTemplate[]>([])
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [scheduledDate, setScheduledDate] = useState(todayIso())
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadTemplates() {
    setLoading(true)
    setError(null)
    try {
      const data = await mo2logApi.workoutTemplates()
      setTemplates(data)
      setSelectedTemplateId((current) => current ?? data[0]?.id ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível carregar os templates.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTemplates()
  }, [])

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

  const selectedTemplate = templates.find((template) => template.id === selectedTemplateId) ?? templates[0]

  if (loading) return <LoadingState title="Carregando templates de treino..." description="Preparando seus treinos reutilizáveis." />

  return (
    <div className="space-y-6">
      <SectionHeader
        eyebrow="Workout Builder"
        title="Templates de treino"
        description="Transforme treinos padrão em sessões planejadas em poucos segundos. Ideal para montar sua semana sem cadastrar cada exercício manualmente."
      />

      {error && <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}
      {message && <div className="rounded-2xl border border-mo-primary/40 bg-mo-primary/10 p-4 text-sm text-mo-primary">{message}</div>}

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
                  <span className="rounded-full bg-white/5 px-3 py-1">{template.difficulty}</span>
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

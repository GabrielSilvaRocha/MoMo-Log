import { useEffect, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { GoalCard } from '../components/GoalCard'
import type { Goal } from '../types/api'

export function GoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([])
  const [selectedGoalId, setSelectedGoalId] = useState(1)
  const [currentValue, setCurrentValue] = useState('12')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function load() {
    try {
      setLoading(true)
      const data = await mo2logApi.goals(getCurrentUserId())
      setGoals(data)
      if (data[0]) {
        setSelectedGoalId(data[0].id)
        setCurrentValue(String(data[0].current_value))
      }
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar metas')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function updateProgress() {
    setSaving(true)
    try {
      const updated = await mo2logApi.updateGoalProgress(selectedGoalId, Number(currentValue))
      setGoals((current) => current.map((goal) => (goal.id === updated.id ? updated : goal)))
      setMessage(`Meta atualizada: ${updated.progress_percentage ?? 0}% concluída.`)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao atualizar meta')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Goals Core</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Metas</h2>
        <p className="mt-3 max-w-3xl text-mo-muted">
          Acompanhe objetivos semanais e atualize o progresso manualmente enquanto a automação completa evolui nas próximas releases.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {message && <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{message}</div>}

      <section className="grid gap-6 lg:grid-cols-[1fr_0.8fr]">
        <div className="space-y-4">
          {loading && <div className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">Carregando metas...</div>}
          {!loading && goals.map((goal) => <GoalCard key={goal.id} goal={goal} />)}
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-lg font-semibold text-white">Atualizar progresso</h3>
          <p className="mt-1 text-sm text-mo-muted">Use para validar a API de metas pelo frontend.</p>

          <label className="mt-5 block text-sm text-mo-muted">
            Meta
            <select
              value={selectedGoalId}
              onChange={(event) => {
                const id = Number(event.target.value)
                const goal = goals.find((item) => item.id === id)
                setSelectedGoalId(id)
                setCurrentValue(goal?.current_value ?? '0')
              }}
              className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
            >
              {goals.map((goal) => (
                <option key={goal.id} value={goal.id}>
                  {goal.title}
                </option>
              ))}
            </select>
          </label>

          <label className="mt-4 block text-sm text-mo-muted">
            Valor atual
            <input
              value={currentValue}
              onChange={(event) => setCurrentValue(event.target.value)}
              type="number"
              step="0.1"
              className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
            />
          </label>

          <button
            onClick={updateProgress}
            disabled={saving || goals.length === 0}
            className="mt-5 w-full rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? 'Salvando...' : 'Salvar progresso'}
          </button>
        </div>
      </section>
    </main>
  )
}

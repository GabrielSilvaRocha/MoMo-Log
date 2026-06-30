import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import type { Equipment, Exercise, UserGymEquipment } from '../types/api'

type EquipmentStatus = 'available' | 'unavailable' | 'unknown' | 'favorite' | 'frequently_busy'

const statusOptions: Array<{ value: EquipmentStatus; label: string; description: string }> = [
  { value: 'available', label: 'Disponível', description: 'Pode ser sugerido normalmente.' },
  { value: 'unavailable', label: 'Não existe', description: 'Sai das sugestões padrão, mas aparece em “ver todas”.' },
  { value: 'frequently_busy', label: 'Sempre ocupado', description: 'Perde prioridade nas sugestões.' },
  { value: 'favorite', label: 'Favorito', description: 'Ganha prioridade quando compatível.' },
  { value: 'unknown', label: 'Não sei', description: 'Sem preferência definida.' },
]

export function ExerciseLibraryPage() {
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [equipment, setEquipment] = useState<Equipment[]>([])
  const [userEquipment, setUserEquipment] = useState<UserGymEquipment[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function load() {
    try {
      setLoading(true)
      const [exerciseData, equipmentData, userEquipmentData] = await Promise.all([
        mo2logApi.exercises(),
        mo2logApi.equipment(),
        mo2logApi.userGymEquipment(getCurrentUserId()),
      ])
      setExercises(exerciseData)
      setEquipment(equipmentData)
      setUserEquipment(userEquipmentData)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar biblioteca')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const equipmentStatusById = useMemo(() => {
    return new Map(userEquipment.map((item) => [item.equipment_id, item.status]))
  }, [userEquipment])

  const filteredExercises = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    if (!normalized) return exercises
    return exercises.filter((exercise) => exercise.name.toLowerCase().includes(normalized))
  }, [exercises, query])

  async function updateStatus(item: Equipment, status: EquipmentStatus) {
    setSavingId(item.id)
    try {
      const updated = await mo2logApi.updateEquipmentStatus({
        user_id: getCurrentUserId(),
        equipment_id: item.id,
        status,
        notes: status === 'unavailable' ? 'Equipamento não existe na minha academia' : undefined,
      })
      setUserEquipment((current) => {
        const others = current.filter((entry) => entry.equipment_id !== item.id)
        return [...others, updated]
      })
      setMessage(`${item.name} marcado como: ${statusOptions.find((option) => option.value === status)?.label}.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao atualizar equipamento')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Biblioteca e academia</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Exercícios e equipamentos</h2>
        <p className="mt-3 max-w-3xl text-mo-muted">
          Configure o que existe na sua academia. Máquinas marcadas como “não existe” deixam de aparecer nas sugestões padrão,
          mas continuam disponíveis na tela de troca de exercício.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {message && <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{message}</div>}

      {loading ? (
        <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 text-mo-muted">Carregando biblioteca...</section>
      ) : (
        <section className="grid gap-6 lg:grid-cols-[1.1fr_1fr]">
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
              <div>
                <h3 className="text-lg font-semibold text-white">Exercícios cadastrados</h3>
                <p className="mt-1 text-sm text-mo-muted">Base inicial para o Adaptation Engine.</p>
              </div>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar exercício"
                className="rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
              />
            </div>

            <div className="mt-5 grid gap-3">
              {filteredExercises.map((exercise) => (
                <article key={exercise.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <h4 className="font-semibold text-white">{exercise.name}</h4>
                      <p className="mt-1 text-sm text-mo-muted">
                        {exercise.exercise_type} · {exercise.difficulty} · {exercise.is_compound ? 'Composto' : 'Isolado'}
                      </p>
                    </div>
                    {exercise.is_unilateral && <span className="rounded-full bg-white/5 px-3 py-1 text-xs text-mo-muted">Unilateral</span>}
                  </div>
                </article>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <h3 className="text-lg font-semibold text-white">Minha academia</h3>
            <p className="mt-1 text-sm text-mo-muted">Ajuste os equipamentos para personalizar as sugestões.</p>

            <div className="mt-5 space-y-3">
              {equipment.map((item) => {
                const currentStatus = equipmentStatusById.get(item.id) ?? 'unknown'
                return (
                  <article key={item.id} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                    <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
                      <div>
                        <h4 className="font-semibold text-white">{item.name}</h4>
                        <p className="mt-1 text-sm text-mo-muted">{item.category}</p>
                      </div>
                      <select
                        value={currentStatus}
                        disabled={savingId === item.id}
                        onChange={(event) => updateStatus(item, event.target.value as EquipmentStatus)}
                        className="rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-sm text-white outline-none focus:border-mo-primary"
                      >
                        {statusOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <p className="mt-3 text-xs text-mo-muted">
                      {statusOptions.find((option) => option.value === currentStatus)?.description}
                    </p>
                  </article>
                )
              })}
            </div>
          </div>
        </section>
      )}
    </main>
  )
}

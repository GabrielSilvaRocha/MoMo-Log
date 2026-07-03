import { useMemo, useState } from 'react'

import {
  addOfflineWorkoutSet,
  clearOfflineWorkoutSessions,
  defaultOfflineExercises,
  exportOfflineWorkoutSessions,
  finishOfflineWorkoutSession,
  getActiveOfflineWorkoutSession,
  listOfflineWorkoutSessions,
  type OfflineWorkoutSession,
} from '../offline/offlineWorkoutStore'

type SetForm = {
  exerciseName: string
  reps: string
  load: string
  rir: string
  rpe: string
  notes: string
}

const defaultForm: SetForm = {
  exerciseName: defaultOfflineExercises[0],
  reps: '10',
  load: '0',
  rir: '2',
  rpe: '8',
  notes: '',
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function OfflineWorkoutPage() {
  const [session, setSession] = useState<OfflineWorkoutSession>(() => getActiveOfflineWorkoutSession())
  const [history, setHistory] = useState<OfflineWorkoutSession[]>(() => listOfflineWorkoutSessions())
  const [form, setForm] = useState<SetForm>(defaultForm)
  const [message, setMessage] = useState<string | null>(null)

  const totalSets = session.sets.length
  const totalVolume = useMemo(
    () => session.sets.reduce((total, set) => total + set.load * set.reps, 0),
    [session.sets],
  )
  const averageRpe = useMemo(() => {
    const values = session.sets.map((set) => set.rpe).filter((value): value is number => value !== null)
    if (!values.length) return null
    return values.reduce((total, value) => total + value, 0) / values.length
  }, [session.sets])

  function refresh(nextSession = getActiveOfflineWorkoutSession()) {
    setSession(nextSession)
    setHistory(listOfflineWorkoutSessions())
  }

  function registerSet() {
    const nextSession = addOfflineWorkoutSet({
      exerciseName: form.exerciseName.trim() || 'Exercicio livre',
      setNumber: session.sets.filter((set) => set.exerciseName === form.exerciseName).length + 1,
      reps: Number(form.reps || 0),
      load: Number(form.load || 0),
      rir: form.rir === '' ? null : Number(form.rir),
      rpe: form.rpe === '' ? null : Number(form.rpe),
      notes: form.notes.trim(),
    })
    refresh(nextSession)
    setMessage('Serie salva no celular.')
  }

  function finishSession() {
    const finished = finishOfflineWorkoutSession()
    setHistory(listOfflineWorkoutSessions())
    setSession(getActiveOfflineWorkoutSession())
    setMessage(`Treino offline finalizado com ${finished.sets.length} series salvas.`)
  }

  async function copyExport() {
    const payload = exportOfflineWorkoutSessions()
    try {
      await navigator.clipboard.writeText(payload)
      setMessage('Exportacao copiada. Guarde ou envie para importar depois.')
    } catch {
      setMessage('Nao foi possivel copiar automaticamente. Use o bloco de exportacao abaixo.')
    }
  }

  function clearLocalData() {
    clearOfflineWorkoutSessions()
    const nextSession = getActiveOfflineWorkoutSession()
    setSession(nextSession)
    setHistory([nextSession])
    setMessage('Registros offline limpos deste aparelho.')
  }

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-6 shadow-glow">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Modo academia offline</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Treine sem PC, sem Wi-Fi e sem backend</h2>
            <p className="mt-2 max-w-3xl text-mo-muted">
              Os registros ficam salvos somente neste celular. Quando voltar para casa ou publicar em cloud, exporte estes dados para reconciliar com o Mo2 LOG principal.
            </p>
          </div>
          <div className="rounded-2xl border border-mo-border bg-black/20 px-4 py-3 text-sm text-mo-muted">
            <p>Inicio: <strong className="text-white">{formatDateTime(session.startedAt)}</strong></p>
            <p>Estado: <strong className="text-mo-primary">{navigator.onLine ? 'online' : 'offline'}</strong></p>
          </div>
        </div>

        <div className="mt-5 grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-mo-muted">Series hoje</p>
            <p className="mt-2 text-2xl font-bold text-white">{totalSets}</p>
          </div>
          <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-mo-muted">Volume local</p>
            <p className="mt-2 text-2xl font-bold text-white">{totalVolume.toLocaleString('pt-BR')} kg</p>
          </div>
          <div className="rounded-2xl border border-mo-border bg-black/20 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-mo-muted">RPE medio</p>
            <p className="mt-2 text-2xl font-bold text-white">{averageRpe ? averageRpe.toFixed(1) : '-'}</p>
          </div>
        </div>
      </section>

      {message && <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{message}</div>}

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6">
        <h3 className="text-xl font-semibold text-white">Registrar serie</h3>
        <div className="mt-5 grid gap-3 md:grid-cols-3">
          <label className="text-sm text-mo-muted md:col-span-2">
            Exercicio
            <input
              list="offline-exercises"
              value={form.exerciseName}
              onChange={(event) => setForm((current) => ({ ...current, exerciseName: event.target.value }))}
              className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary"
            />
            <datalist id="offline-exercises">
              {defaultOfflineExercises.map((exercise) => <option key={exercise} value={exercise} />)}
            </datalist>
          </label>
          <label className="text-sm text-mo-muted">
            Reps
            <input value={form.reps} onChange={(event) => setForm((current) => ({ ...current, reps: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" type="number" min={0} />
          </label>
          <label className="text-sm text-mo-muted">
            Carga
            <input value={form.load} onChange={(event) => setForm((current) => ({ ...current, load: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" type="number" min={0} step="0.5" />
          </label>
          <label className="text-sm text-mo-muted">
            RIR
            <input value={form.rir} onChange={(event) => setForm((current) => ({ ...current, rir: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" type="number" min={0} max={10} />
          </label>
          <label className="text-sm text-mo-muted">
            RPE
            <input value={form.rpe} onChange={(event) => setForm((current) => ({ ...current, rpe: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" type="number" min={0} max={10} />
          </label>
          <label className="text-sm text-mo-muted md:col-span-3">
            Observacao
            <input value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>
        </div>
        <div className="mt-5 flex flex-wrap gap-3">
          <button onClick={registerSet} className="rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow">Salvar serie no celular</button>
          <button onClick={finishSession} className="rounded-2xl border border-mo-border px-5 py-3 font-semibold text-white">Finalizar treino offline</button>
          <button onClick={copyExport} className="rounded-2xl border border-mo-border px-5 py-3 font-semibold text-white">Copiar exportacao</button>
        </div>
      </section>

      <section className="grid gap-4">
        {session.sets.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-mo-border bg-mo-surface p-6 text-mo-muted">Nenhuma serie salva neste treino ainda.</div>
        ) : session.sets.map((set) => (
          <article key={set.id} className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
              <div>
                <h4 className="font-semibold text-white">{set.exerciseName}</h4>
                <p className="mt-1 text-sm text-mo-muted">
                  Serie {set.setNumber} - {set.reps} reps - {set.load} kg - RIR {set.rir ?? '-'} - RPE {set.rpe ?? '-'}
                </p>
                {set.notes && <p className="mt-1 text-sm text-mo-muted">{set.notes}</p>}
              </div>
              <span className="text-sm text-mo-muted">{formatDateTime(set.completedAt)}</span>
            </div>
          </article>
        ))}
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <h3 className="text-xl font-semibold text-white">Historico salvo neste celular</h3>
            <p className="mt-1 text-sm text-mo-muted">{history.length} sessoes locais. Os dados ficam no armazenamento do navegador/app.</p>
          </div>
          <button onClick={clearLocalData} className="rounded-2xl border border-red-400/40 px-4 py-2 text-sm font-semibold text-red-100">Limpar dados offline</button>
        </div>
        <textarea readOnly value={exportOfflineWorkoutSessions()} className="mt-5 h-48 w-full rounded-2xl border border-mo-border bg-black/30 p-4 font-mono text-xs text-mo-muted outline-none" />
      </section>
    </main>
  )
}

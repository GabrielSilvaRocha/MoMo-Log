import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { LoadingState } from '../components/LoadingState'
import { MetricCard } from '../components/MetricCard'
import type { RunningExecution, RunningGoal, RunningPlanSession, RunningStepLog, RunningWorkoutStep } from '../types/api'
import { formatDateTime, formatDuration, formatNumber } from '../utils/format'

function formatPaceFromSeconds(seconds?: number | null) {
  if (!seconds || seconds <= 0) return '—'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}/km`
}

function formatClock(seconds: number) {
  const safe = Math.max(0, Math.floor(seconds))
  const min = Math.floor(safe / 60)
  const sec = safe % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function timeToSeconds(value: string) {
  const [minutes, seconds] = value.split(':').map((part) => Number(part))
  return (minutes || 0) * 60 + (seconds || 0)
}

function secondsToTime(seconds?: number | null) {
  if (!seconds) return ''
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

function stepTypeLabel(type: string) {
  const labels: Record<string, string> = {
    warmup: 'Aquecimento',
    run: 'Corrida',
    interval: 'Tiro',
    recovery: 'Recuperação',
    rest: 'Descanso',
    cooldown: 'Desaquecimento',
  }
  return labels[type] ?? type
}

function numberFromApi(value?: string | number | null) {
  if (value === null || value === undefined) return null
  const number = typeof value === 'string' ? Number(value) : value
  return Number.isFinite(number) ? number : null
}

function secondsFromDistanceAndSpeed(distanceMeters: number | null, speedKmh: number | null) {
  if (!distanceMeters || !speedKmh || speedKmh <= 0) return 0
  return Math.ceil((distanceMeters / 1000 / speedKmh) * 3600)
}

function paceSecondsFromSpeed(speedKmh: number | null) {
  if (!speedKmh || speedKmh <= 0) return null
  return Math.round(3600 / speedKmh)
}

function formatDistanceMeters(distanceMeters?: number | null, fractionDigits = 2) {
  if (!distanceMeters || distanceMeters <= 0) return '—'
  if (distanceMeters >= 1000) return `${formatNumber(distanceMeters / 1000, fractionDigits)} km`
  return `${Math.ceil(distanceMeters)} m`
}

function describeStepTarget(step: RunningWorkoutStep) {
  const speed = numberFromApi(step.target_speed_kmh)
  const distance = step.target_distance_m ? formatDistanceMeters(step.target_distance_m) : null
  const estimated = step.target_distance_m
    ? formatClock(secondsFromDistanceAndSpeed(step.target_distance_m, speed))
    : step.target_duration_seconds
      ? formatClock(step.target_duration_seconds)
      : '—'
  return `${stepTypeLabel(step.step_type)} · ${distance ?? 'por tempo'} · ${step.target_speed_kmh ?? '—'} km/h · ${estimated}`
}

export function RunningPage() {
  const [goal, setGoal] = useState<RunningGoal | null>(null)
  const [plan, setPlan] = useState<RunningPlanSession[]>([])
  const [selectedSession, setSelectedSession] = useState<RunningPlanSession | null>(null)
  const [execution, setExecution] = useState<RunningExecution | null>(null)
  const [activeStep, setActiveStep] = useState<RunningWorkoutStep | null>(null)
  const [activeStepLog, setActiveStepLog] = useState<RunningStepLog | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState(0)
  const [remainingDistanceMeters, setRemainingDistanceMeters] = useState<number | null>(null)
  const [preStartCountdown, setPreStartCountdown] = useState<number | null>(null)
  const [pendingFirstStep, setPendingFirstStep] = useState<{ executionId: number; step: RunningWorkoutStep } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [goalForm, setGoalForm] = useState({
    raceDate: '2026-08-16',
    current5k: '24:58',
    target5k: '23:30',
  })

  async function load() {
    try {
      setLoading(true)
      let currentGoal: RunningGoal | null = null
      try {
        currentGoal = await mo2logApi.currentRunningGoal(getCurrentUserId())
      } catch {
        currentGoal = null
      }
      const week = await mo2logApi.runningPlanWeek(getCurrentUserId(), '2026-06-29')
      setGoal(currentGoal)
      setPlan(week)
      if (!selectedSession && week.length) setSelectedSession(week[0])
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar plano de corrida')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!pendingFirstStep || preStartCountdown === null) return

    if (preStartCountdown <= 0) {
      const firstStep = pendingFirstStep
      setPendingFirstStep(null)
      setPreStartCountdown(null)
      void startStep(firstStep.executionId, firstStep.step)
      return
    }

    const timer = window.setTimeout(() => {
      setPreStartCountdown((current) => (current === null ? null : Math.max(0, current - 1)))
    }, 1000)

    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingFirstStep, preStartCountdown])

  const currentSpeed = numberFromApi(activeStepLog?.actual_speed_kmh ?? activeStep?.target_speed_kmh ?? selectedSession?.target_speed_kmh ?? null)
  const activeStepIsDistanceBased = Boolean(activeStep?.target_distance_m && activeStep.target_distance_m > 0)
  const estimatedRemainingSeconds = activeStepIsDistanceBased
    ? secondsFromDistanceAndSpeed(remainingDistanceMeters, currentSpeed)
    : remainingSeconds
  const currentPaceSeconds = paceSecondsFromSpeed(currentSpeed) ?? activeStep?.target_pace_seconds_per_km ?? null

  useEffect(() => {
    if (!activeStep) return

    if (activeStepIsDistanceBased) {
      if (!remainingDistanceMeters || remainingDistanceMeters <= 0) return
      const timer = window.setInterval(() => {
        const metersPerSecond = currentSpeed && currentSpeed > 0 ? (currentSpeed * 1000) / 3600 : 0
        setRemainingDistanceMeters((current) => Math.max(0, (current ?? 0) - metersPerSecond))
      }, 1000)
      return () => window.clearInterval(timer)
    }

    if (remainingSeconds <= 0) return
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [activeStep, activeStepIsDistanceBased, currentSpeed, remainingDistanceMeters, remainingSeconds])

  const weeklySummary = useMemo(() => {
    const distance = plan.reduce((total, session) => total + Number(session.target_distance_km ?? 0), 0)
    const duration = plan.reduce((total, session) => total + Number(session.target_duration_seconds ?? 0), 0)
    const intervals = plan.filter((session) => session.session_type === 'interval').length
    return { distance, duration, intervals, sessions: plan.length }
  }, [plan])

  async function saveGoalAndGenerate() {
    try {
      const createdGoal = await mo2logApi.createRunningGoal({
        user_id: getCurrentUserId(),
        race_distance_km: 5,
        race_date: `${goalForm.raceDate}T09:00:00Z`,
        current_5k_time_seconds: timeToSeconds(goalForm.current5k),
        target_5k_time_seconds: timeToSeconds(goalForm.target5k),
        training_location: 'treadmill',
        available_weekdays: 'mon,tue,wed,thu,fri',
      })
      await mo2logApi.generateRunningPlan(createdGoal.id)
      setMessage('Objetivo salvo e plano de esteira gerado com sucesso.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao gerar plano')
    }
  }

  async function regeneratePlan() {
    if (!goal) return
    try {
      const result = await mo2logApi.generateRunningPlan(goal.id)
      setMessage(result.message)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao gerar plano')
    }
  }

  async function startSession(session: RunningPlanSession) {
    try {
      const started = await mo2logApi.startRunningExecution(session.id)
      setExecution(started)
      setSelectedSession(session)
      setActiveStep(null)
      setActiveStepLog(null)
      setRemainingDistanceMeters(null)
      setRemainingSeconds(0)
      const firstStep = session.steps[0]
      if (firstStep) {
        setPendingFirstStep({ executionId: started.id, step: firstStep })
        setPreStartCountdown(5)
        setMessage('Prepare a esteira. O treino começa em 5 segundos.')
      } else {
        setMessage('Execução iniciada. Nenhuma etapa encontrada para este treino.')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao iniciar corrida')
    }
  }

  async function startStep(executionId: number, step: RunningWorkoutStep) {
    const log = await mo2logApi.startRunningStep(executionId, step.id)
    const speed = numberFromApi(log.actual_speed_kmh ?? step.target_speed_kmh)
    const distanceMeters = step.target_distance_m ?? null
    setActiveStep(step)
    setActiveStepLog(log)
    setRemainingDistanceMeters(distanceMeters)
    setRemainingSeconds(
      distanceMeters
        ? secondsFromDistanceAndSpeed(distanceMeters, speed)
        : step.target_duration_seconds ?? step.rest_seconds ?? 0,
    )
  }

  async function adjustSpeed(direction: 'up' | 'down') {
    if (!activeStepLog) return
    const adjustment = direction === 'up'
      ? await mo2logApi.speedUpRunningStep(activeStepLog.id)
      : await mo2logApi.speedDownRunningStep(activeStepLog.id)
    setActiveStepLog((current) => current ? { ...current, actual_speed_kmh: adjustment.new_speed_kmh } : current)
  }

  async function finishExecution() {
    if (!execution) return
    const finished = await mo2logApi.finishRunningExecution(execution.id)
    setExecution(finished)
    setMessage('Treino finalizado e registrado no Running Coach.')
    await load()
  }

  if (loading) return <LoadingState title="Carregando corrida" description="Montando objetivo, plano e execução guiada." />

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <p className="text-sm uppercase tracking-[0.3em] text-mo-primary">Running Coach</p>
        <h2 className="mt-3 text-3xl font-bold tracking-tight text-white">Plano de corrida para 5 km na esteira</h2>
        <p className="mt-3 max-w-4xl text-mo-muted">
          O módulo de corridas agora é guiado por objetivo e estruturado por distância. O tempo é estimado pela velocidade atual da esteira: se você ajustar a velocidade com + ou -, o Mo² LOG recalcula o tempo restante automaticamente.
        </p>
      </section>

      {error && <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-5 text-red-100">{error}</div>}
      {message && <div className="rounded-3xl border border-mo-primary/40 bg-mo-primary/10 p-5 text-mo-primary">{message}</div>}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Sessões da semana" value={String(weeklySummary.sessions)} hint="Plano atual" icon="📅" />
        <MetricCard label="Km planejados" value={`${formatNumber(weeklySummary.distance)} km`} hint="Base do plano" icon="📏" />
        <MetricCard label="Tempo estimado" value={formatDuration(weeklySummary.duration)} hint="Calculado pelo pace" icon="⏱️" />
        <MetricCard label="Intervalados" value={String(weeklySummary.intervals)} hint="Treinos com etapas" icon="⚡" />
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-xl font-semibold text-white">Objetivo atual</h3>
          {goal ? (
            <div className="mt-4 space-y-3 text-sm text-mo-muted">
              <p>Prova: <strong className="text-white">{goal.race_distance_km} km</strong></p>
              <p>Data: <strong className="text-white">{formatDateTime(goal.race_date)}</strong></p>
              <p>Tempo atual 5 km: <strong className="text-white">{secondsToTime(goal.current_5k_time_seconds)}</strong></p>
              <p>Meta 5 km: <strong className="text-white">{secondsToTime(goal.target_5k_time_seconds)}</strong></p>
              <p>Local: <strong className="text-white">Esteira</strong></p>
              <button onClick={regeneratePlan} className="mt-2 rounded-2xl bg-mo-primary px-5 py-3 font-bold text-black shadow-glow">Gerar/atualizar plano</button>
            </div>
          ) : (
            <div className="mt-4 space-y-4">
              <label className="block text-sm text-mo-muted">Data da prova
                <input type="date" value={goalForm.raceDate} onChange={(event) => setGoalForm((current) => ({ ...current, raceDate: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white" />
              </label>
              <label className="block text-sm text-mo-muted">Seu 5 km atual
                <input value={goalForm.current5k} onChange={(event) => setGoalForm((current) => ({ ...current, current5k: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white" />
              </label>
              <label className="block text-sm text-mo-muted">Meta de 5 km
                <input value={goalForm.target5k} onChange={(event) => setGoalForm((current) => ({ ...current, target5k: event.target.value }))} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white" />
              </label>
              <button onClick={saveGoalAndGenerate} className="rounded-2xl bg-mo-primary px-5 py-3 font-bold text-black shadow-glow">Salvar objetivo e gerar plano</button>
            </div>
          )}
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <h3 className="text-xl font-semibold text-white">Plano da semana</h3>
          <div className="mt-4 space-y-3">
            {plan.map((session) => (
              <button key={session.id} onClick={() => setSelectedSession(session)} className={`w-full rounded-2xl border p-4 text-left transition ${selectedSession?.id === session.id ? 'border-mo-primary bg-mo-primary/10' : 'border-mo-border bg-white/[0.03] hover:border-mo-primary/40'}`}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-white">{session.title}</p>
                    <p className="mt-1 text-sm text-mo-muted">{formatDateTime(session.scheduled_date)} · {stepTypeLabel(session.session_type)}</p>
                  </div>
                  <span className="rounded-full bg-black/30 px-3 py-1 text-xs text-mo-primary">{session.steps.length} etapas</span>
                </div>
                <p className="mt-2 text-sm text-mo-muted">{session.target_distance_km ?? '—'} km · {formatPaceFromSeconds(session.target_pace_seconds_per_km)} · {session.target_speed_kmh ?? '—'} km/h</p>
              </button>
            ))}
            {!plan.length && <p className="text-sm text-mo-muted">Nenhuma sessão gerada para esta semana.</p>}
          </div>
        </div>
      </section>

      {selectedSession && (
        <section className="grid gap-4 xl:grid-cols-[1fr_0.9fr]">
          <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
              <div>
                <p className="text-sm uppercase tracking-[0.25em] text-mo-primary">Execução guiada</p>
                <h3 className="mt-2 text-2xl font-bold text-white">{selectedSession.title}</h3>
                <p className="mt-2 text-mo-muted">{selectedSession.description}</p>
              </div>
              {!execution || execution.status === 'completed' ? (
                <button onClick={() => startSession(selectedSession)} className="rounded-2xl bg-mo-primary px-5 py-3 font-bold text-black shadow-glow">Iniciar treino</button>
              ) : (
                <button onClick={finishExecution} className="rounded-2xl border border-mo-primary px-5 py-3 font-bold text-mo-primary">Finalizar treino</button>
              )}
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-4">
              <MetricCard label="Pace alvo" value={formatPaceFromSeconds(selectedSession.target_pace_seconds_per_km)} hint="Ritmo planejado" icon="⚡" />
              <MetricCard label="Velocidade" value={`${selectedSession.target_speed_kmh ?? '—'} km/h`} hint="Esteira" icon="🏟️" />
              <MetricCard label="Distância" value={`${selectedSession.target_distance_km ?? '—'} km`} hint="Base do treino" icon="📏" />
              <MetricCard label="Tempo estimado" value={formatDuration(selectedSession.target_duration_seconds ?? 0)} hint="Pela velocidade" icon="⏱️" />
            </div>

            <div className="mt-6 space-y-3">
              {selectedSession.steps.map((step) => (
                <button key={step.id} disabled={!execution || execution.status === 'completed' || preStartCountdown !== null} onClick={() => execution && startStep(execution.id, step)} className={`w-full rounded-2xl border p-4 text-left transition ${activeStep?.id === step.id ? 'border-mo-primary bg-mo-primary/10' : 'border-mo-border bg-white/[0.03] hover:border-mo-primary/40'} disabled:cursor-not-allowed disabled:opacity-60`}>
                  <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                    <p className="font-semibold text-white">{step.order_index}. {step.title}</p>
                    <p className="text-sm text-mo-muted">{describeStepTarget(step)}</p>
                  </div>
                  {step.notes && <p className="mt-2 text-sm text-mo-muted">{step.notes}</p>}
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-mo-border bg-mo-surface p-6">
            <p className="text-sm uppercase tracking-[0.25em] text-mo-primary">Esteira</p>
            <h3 className="mt-2 text-2xl font-bold text-white">Painel de controle</h3>
            {preStartCountdown !== null ? (
              <div className="mt-6 rounded-3xl border border-mo-primary/30 bg-mo-primary/10 p-8 text-center">
                <p className="text-sm uppercase tracking-[0.25em] text-mo-muted">Prepare a esteira</p>
                <p className="mt-4 text-7xl font-black text-mo-primary">{preStartCountdown}</p>
                <p className="mt-3 text-sm text-mo-muted">A primeira etapa começa automaticamente.</p>
              </div>
            ) : activeStep ? (
              <div className="mt-6 space-y-5">
                <div className="rounded-3xl border border-mo-primary/30 bg-mo-primary/10 p-5 text-center">
                  <p className="text-sm text-mo-muted">Etapa atual</p>
                  <p className="mt-1 text-xl font-bold text-white">{activeStep.title}</p>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <div className="rounded-2xl bg-black/20 p-4">
                      <p className="text-sm text-mo-muted">Km restantes</p>
                      <p className="mt-2 text-4xl font-black text-mo-primary">{activeStepIsDistanceBased ? formatDistanceMeters(remainingDistanceMeters) : '—'}</p>
                    </div>
                    <div className="rounded-2xl bg-black/20 p-4">
                      <p className="text-sm text-mo-muted">Tempo estimado</p>
                      <p className="mt-2 text-4xl font-black text-white">{formatClock(estimatedRemainingSeconds)}</p>
                    </div>
                  </div>
                  <p className="mt-3 text-sm text-mo-muted">
                    {activeStepIsDistanceBased ? 'O tempo muda conforme a velocidade usada.' : 'Etapa controlada por tempo.'}
                  </p>
                </div>

                <div className="rounded-3xl border border-mo-border bg-black/20 p-5 text-center">
                  <p className="text-sm text-mo-muted">Velocidade atual da esteira</p>
                  <div className="mt-4 flex items-center justify-center gap-4">
                    <button onClick={() => adjustSpeed('down')} className="h-14 w-14 rounded-2xl border border-mo-border text-2xl font-bold text-white">−</button>
                    <strong className="min-w-32 text-4xl text-white">{currentSpeed?.toFixed(2) ?? '—'}</strong>
                    <button onClick={() => adjustSpeed('up')} className="h-14 w-14 rounded-2xl bg-mo-primary text-2xl font-bold text-black shadow-glow">+</button>
                  </div>
                  <p className="mt-3 text-sm text-mo-muted">Ajustes de 0,1 km/h recalculam o tempo restante e ficam registrados.</p>
                </div>

                <div className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">
                  <p>Pace pela velocidade atual: <strong className="text-white">{formatPaceFromSeconds(currentPaceSeconds)}</strong></p>
                  <p>Pace planejado: <strong className="text-white">{formatPaceFromSeconds(activeStep.target_pace_seconds_per_km)}</strong></p>
                  <p>Distância da etapa: <strong className="text-white">{activeStep.target_distance_m ? formatDistanceMeters(activeStep.target_distance_m) : 'por tempo'}</strong></p>
                  <p>Próxima etapa: <strong className="text-white">{selectedSession.steps.find((step) => step.order_index === activeStep.order_index + 1)?.title ?? 'finalizar treino'}</strong></p>
                </div>
              </div>
            ) : (
              <p className="mt-5 text-mo-muted">Inicie uma sessão para liberar o timer de largada e o controle de velocidade.</p>
            )}
          </div>
        </section>
      )}
    </main>
  )
}

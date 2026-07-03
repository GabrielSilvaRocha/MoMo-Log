export type OfflineWorkoutSet = {
  id: string
  exerciseName: string
  setNumber: number
  reps: number
  load: number
  rir: number | null
  rpe: number | null
  notes: string
  completedAt: string
}

export type OfflineWorkoutSession = {
  id: string
  title: string
  startedAt: string
  finishedAt: string | null
  sets: OfflineWorkoutSet[]
}

const SESSIONS_KEY = 'mo2log.offlineWorkoutSessions'
const ACTIVE_SESSION_KEY = 'mo2log.activeOfflineWorkoutSession'

export const defaultOfflineExercises = [
  'Agachamento ou leg press',
  'Supino ou maquina peitoral',
  'Remada ou puxada',
  'Desenvolvimento de ombros',
  'Stiff ou flexora',
  'Abdominal ou prancha',
]

function safeParseSessions(raw: string | null): OfflineWorkoutSession[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function listOfflineWorkoutSessions(): OfflineWorkoutSession[] {
  return safeParseSessions(localStorage.getItem(SESSIONS_KEY)).sort((a, b) => b.startedAt.localeCompare(a.startedAt))
}

function saveSessions(sessions: OfflineWorkoutSession[]) {
  localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions))
}

export function getActiveOfflineWorkoutSession(): OfflineWorkoutSession {
  const activeId = localStorage.getItem(ACTIVE_SESSION_KEY)
  const sessions = listOfflineWorkoutSessions()
  const active = sessions.find((session) => session.id === activeId && !session.finishedAt)
  if (active) return active

  const now = new Date().toISOString()
  const session: OfflineWorkoutSession = {
    id: crypto.randomUUID(),
    title: 'Treino offline na academia',
    startedAt: now,
    finishedAt: null,
    sets: [],
  }
  saveSessions([session, ...sessions])
  localStorage.setItem(ACTIVE_SESSION_KEY, session.id)
  return session
}

export function addOfflineWorkoutSet(payload: Omit<OfflineWorkoutSet, 'id' | 'completedAt'>): OfflineWorkoutSession {
  const session = getActiveOfflineWorkoutSession()
  const sessions = listOfflineWorkoutSessions()
  const nextSession = {
    ...session,
    sets: [
      ...session.sets,
      {
        ...payload,
        id: crypto.randomUUID(),
        completedAt: new Date().toISOString(),
      },
    ],
  }
  saveSessions(sessions.map((item) => (item.id === session.id ? nextSession : item)))
  return nextSession
}

export function finishOfflineWorkoutSession(): OfflineWorkoutSession {
  const session = getActiveOfflineWorkoutSession()
  const sessions = listOfflineWorkoutSessions()
  const nextSession = { ...session, finishedAt: new Date().toISOString() }
  saveSessions(sessions.map((item) => (item.id === session.id ? nextSession : item)))
  localStorage.removeItem(ACTIVE_SESSION_KEY)
  return nextSession
}

export function clearOfflineWorkoutSessions() {
  localStorage.removeItem(SESSIONS_KEY)
  localStorage.removeItem(ACTIVE_SESSION_KEY)
}

export function exportOfflineWorkoutSessions() {
  const payload = {
    exported_at: new Date().toISOString(),
    source: 'mo2log_android_offline',
    sessions: listOfflineWorkoutSessions(),
  }
  return JSON.stringify(payload, null, 2)
}

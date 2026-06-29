import type { TrainingSession } from '../types/api'
import { formatDate, translateSessionType, translateStatus } from '../utils/format'

type SessionListProps = {
  title: string
  sessions: TrainingSession[]
  emptyText: string
}

const typeMarker: Record<string, string> = {
  strength: '🏋️',
  running: '🏃',
  mobility: '🧘',
  rest: '🌙',
}

export function SessionList({ title, sessions, emptyText }: SessionListProps) {
  return (
    <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-white">{title}</h2>
        <span className="rounded-full bg-white/5 px-3 py-1 text-xs text-mo-muted">{sessions.length}</span>
      </div>

      {sessions.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-mo-border p-4 text-sm text-mo-muted">{emptyText}</p>
      ) : (
        <div className="space-y-3">
          {sessions.map((session) => (
            <article key={session.id} className="rounded-2xl bg-white/[0.03] p-4">
              <div className="flex items-start gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-mo-primary/10 text-lg">
                  {typeMarker[session.session_type] ?? '•'}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-medium text-white">{session.title}</h3>
                    <span className="rounded-full border border-mo-border px-2 py-0.5 text-xs text-mo-muted">
                      {translateSessionType(session.session_type)}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-mo-muted">
                    {formatDate(session.scheduled_date)} · {translateStatus(session.status)}
                  </p>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export function formatNumber(value: string | number, fractionDigits = 1): string {
  const number = typeof value === 'string' ? Number(value) : value
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(Number.isFinite(number) ? number : 0)
}

export function formatDate(date: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
  }).format(new Date(`${date}T12:00:00`))
}

export function formatDateTime(date: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

export function formatDuration(seconds: number): string {
  const safeSeconds = Number.isFinite(seconds) ? seconds : 0
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const remainingSeconds = Math.floor(safeSeconds % 60)

  if (hours > 0) return `${hours}h ${String(minutes).padStart(2, '0')}min`
  return `${minutes}min ${String(remainingSeconds).padStart(2, '0')}s`
}

export function formatPace(value: string | number): string {
  const numeric = typeof value === 'string' ? Number(value) : value
  if (!Number.isFinite(numeric) || numeric <= 0) return '0:00/km'
  const minutes = Math.floor(numeric)
  const seconds = Math.round((numeric - minutes) * 60)
  return `${minutes}:${String(seconds).padStart(2, '0')}/km`
}

export function translateSessionType(type: string): string {
  const labels: Record<string, string> = {
    strength: 'Musculação',
    running: 'Corrida',
    mobility: 'Mobilidade',
    rest: 'Descanso',
  }
  return labels[type] ?? type
}

export function translateStatus(status: string): string {
  const labels: Record<string, string> = {
    planned: 'Planejado',
    in_progress: 'Em andamento',
    completed: 'Concluído',
    skipped: 'Pulado',
    rescheduled: 'Remarcado',
    adapted: 'Adaptado',
    active: 'Ativa',
    completed_goal: 'Concluída',
  }
  return labels[status] ?? status
}

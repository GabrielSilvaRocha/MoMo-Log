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
  }
  return labels[status] ?? status
}

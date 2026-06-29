type MetricCardProps = {
  label: string
  value: string
  hint?: string
  icon?: string
}

export function MetricCard({ label, value, hint, icon }: MetricCardProps) {
  return (
    <article className="rounded-3xl border border-mo-border bg-mo-surfaceElevated p-5 shadow-glow">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm text-mo-muted">{label}</p>
          <strong className="mt-2 block text-3xl font-semibold tracking-tight text-white">{value}</strong>
        </div>
        <span className="grid h-11 w-11 place-items-center rounded-2xl bg-mo-primary/10 text-xl text-mo-primary">
          {icon ?? '●'}
        </span>
      </div>
      {hint && <p className="mt-4 text-sm text-mo-muted">{hint}</p>}
    </article>
  )
}

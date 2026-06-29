type ProgressBarProps = {
  value: number
  label?: string
}

export function ProgressBar({ value, label }: ProgressBarProps) {
  const normalized = Math.max(0, Math.min(100, value))

  return (
    <div>
      {label && (
        <div className="mb-2 flex justify-between text-sm">
          <span className="text-mo-muted">{label}</span>
          <span className="font-medium text-white">{normalized.toFixed(0)}%</span>
        </div>
      )}
      <div className="h-3 overflow-hidden rounded-full bg-white/10">
        <div className="h-full rounded-full bg-mo-primary shadow-glow" style={{ width: `${normalized}%` }} />
      </div>
    </div>
  )
}

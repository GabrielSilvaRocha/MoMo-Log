type EmptyStateProps = {
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
}

export function EmptyState({ title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="rounded-3xl border border-dashed border-mo-border bg-white/[0.02] p-8 text-center">
      <p className="text-lg font-semibold text-white">{title}</p>
      {description && <p className="mt-2 text-sm text-mo-muted">{description}</p>}
      {actionLabel && onAction && (
        <button onClick={onAction} className="mt-5 rounded-2xl bg-mo-primary px-5 py-3 font-semibold text-black shadow-glow">
          {actionLabel}
        </button>
      )}
    </div>
  )
}

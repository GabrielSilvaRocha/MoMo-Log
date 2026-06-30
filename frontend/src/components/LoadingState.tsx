type LoadingStateProps = {
  title?: string
  description?: string
}

export function LoadingState({ title = 'Carregando dados...', description = 'Sincronizando informações do Mo² LOG.' }: LoadingStateProps) {
  return (
    <div className="rounded-3xl border border-mo-border bg-mo-surface p-8 text-mo-muted">
      <div className="h-2 w-32 animate-pulse rounded-full bg-mo-primary/40" />
      <h2 className="mt-5 text-xl font-semibold text-white">{title}</h2>
      <p className="mt-2 text-sm text-mo-muted">{description}</p>
    </div>
  )
}

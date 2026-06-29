import type { Goal } from '../types/api'
import { formatNumber } from '../utils/format'
import { ProgressBar } from './ProgressBar'

type GoalCardProps = {
  goal: Goal
}

export function GoalCard({ goal }: GoalCardProps) {
  return (
    <article className="rounded-3xl border border-mo-border bg-mo-surface p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm text-mo-muted">Meta ativa</p>
          <h3 className="mt-1 text-lg font-semibold text-white">{goal.title}</h3>
        </div>
        <span className="rounded-full bg-mo-primary/10 px-3 py-1 text-xs font-medium text-mo-primary">
          {goal.status}
        </span>
      </div>

      <div className="mt-5">
        <ProgressBar value={goal.progress_percentage ?? 0} label="Progresso" />
      </div>

      <p className="mt-4 text-sm text-mo-muted">
        {formatNumber(goal.current_value)} / {formatNumber(goal.target_value)} {goal.unit}
      </p>
    </article>
  )
}

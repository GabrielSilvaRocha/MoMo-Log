import { useEffect, useMemo, useState } from 'react'

import { getApiBaseUrl } from '../api/client'
import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId } from '../auth/session'
import { MetricCard } from '../components/MetricCard'
import type { ReportOverview } from '../types/api'
import { formatDuration, formatNumber, formatPace } from '../utils/format'

function getDefaultDateFrom() {
  const date = new Date()
  date.setDate(date.getDate() - 30)
  return date.toISOString().slice(0, 10)
}

function getDefaultDateTo() {
  return new Date().toISOString().slice(0, 10)
}

function buildExportUrl(kind: 'sessions' | 'running' | 'strength', dateFrom: string, dateTo: string) {
  const params = new URLSearchParams({ user_id: String(getCurrentUserId()) })
  if (dateFrom) params.set('date_from', dateFrom)
  if (dateTo) params.set('date_to', dateTo)
  return `${getApiBaseUrl()}/reports/export/${kind}.csv?${params.toString()}`
}

export function ReportsPage() {
  const [dateFrom, setDateFrom] = useState(getDefaultDateFrom())
  const [dateTo, setDateTo] = useState(getDefaultDateTo())
  const [overview, setOverview] = useState<ReportOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      setLoading(true)
      const data = await mo2logApi.reportOverview(getCurrentUserId(), dateFrom, dateTo)
      setOverview(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar relatório')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const exportLinks = useMemo(
    () => [
      {
        label: 'Exportar sessões',
        description: 'Planejamento, status e observações dos treinos.',
        href: buildExportUrl('sessions', dateFrom, dateTo),
      },
      {
        label: 'Exportar corridas',
        description: 'Corridas guiadas por objetivo, esteira e métricas de pace.',
        href: buildExportUrl('running', dateFrom, dateTo),
      },
      {
        label: 'Exportar musculação',
        description: 'Séries, cargas, repetições, RIR, RPE e volume.',
        href: buildExportUrl('strength', dateFrom, dateTo),
      },
    ],
    [dateFrom, dateTo],
  )

  return (
    <section className="space-y-6">
      <div className="rounded-3xl border border-mo-border bg-mo-surface p-6">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <p className="text-sm uppercase tracking-[0.35em] text-mo-primary">Relatórios</p>
            <h2 className="mt-2 text-2xl font-bold">Resumo exportável do período</h2>
            <p className="mt-2 max-w-3xl text-sm text-mo-muted">
              Use esta tela para analisar um período específico e exportar CSVs para Excel, Power BI ou portfólio.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <label className="text-sm text-mo-muted">
              De
              <input
                type="date"
                value={dateFrom}
                onChange={(event) => setDateFrom(event.target.value)}
                className="mt-1 w-full rounded-2xl border border-mo-border bg-black/20 px-3 py-2 text-white"
              />
            </label>
            <label className="text-sm text-mo-muted">
              Até
              <input
                type="date"
                value={dateTo}
                onChange={(event) => setDateTo(event.target.value)}
                className="mt-1 w-full rounded-2xl border border-mo-border bg-black/20 px-3 py-2 text-white"
              />
            </label>
            <button
              onClick={load}
              className="self-end rounded-2xl bg-mo-primary px-4 py-2 text-sm font-bold text-black shadow-glow"
            >
              Atualizar
            </button>
          </div>
        </div>
      </div>

      {error && <div className="rounded-2xl border border-red-500/40 bg-red-950/30 p-4 text-red-100">{error}</div>}
      {loading && <p className="text-mo-muted">Carregando relatório...</p>}

      {overview && (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Consistência"
              value={`${formatNumber(overview.completion_rate, 0)}%`}
              hint={`${overview.completed_sessions}/${overview.total_sessions} sessões concluídas`}
              icon="✅"
            />
            <MetricCard
              label="Volume de musculação"
              value={`${formatNumber(overview.strength_volume, 0)} kg`}
              hint={`${overview.total_sets} séries registradas`}
              icon="🏋️"
            />
            <MetricCard
              label="Corridas"
              value={`${formatNumber(overview.running_distance_km)} km`}
              hint={`${overview.running_activities} atividades · ${overview.treadmill_runs} na esteira`}
              icon="🏃"
            />
            <MetricCard
              label="Pace médio"
              value={overview.average_pace ? formatPace(overview.average_pace) : '0:00/km'}
              hint={formatDuration(overview.running_time_s)}
              icon="⏱️"
            />
          </div>

          <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <article className="rounded-3xl border border-mo-border bg-mo-surface p-6">
              <h3 className="text-lg font-semibold">Exportações disponíveis</h3>
              <p className="mt-1 text-sm text-mo-muted">
                Os arquivos são gerados pelo backend e baixados em CSV no período selecionado.
              </p>
              <div className="mt-5 grid gap-3">
                {exportLinks.map((link) => (
                  <a
                    key={link.href}
                    href={link.href}
                    className="rounded-2xl border border-mo-border bg-white/[0.03] p-4 transition hover:border-mo-primary/60 hover:bg-mo-primary/10"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <strong className="text-white">{link.label}</strong>
                        <p className="mt-1 text-sm text-mo-muted">{link.description}</p>
                      </div>
                      <span className="rounded-full bg-mo-primary px-3 py-1 text-xs font-bold text-black">CSV</span>
                    </div>
                  </a>
                ))}
              </div>
            </article>

            <article className="rounded-3xl border border-mo-border bg-mo-surface p-6">
              <h3 className="text-lg font-semibold">Insights do período</h3>
              <div className="mt-4 space-y-3">
                {overview.insights.length === 0 && <p className="text-sm text-mo-muted">Sem insights para este período.</p>}
                {overview.insights.map((insight) => (
                  <p key={insight} className="rounded-2xl border border-mo-border bg-black/20 p-4 text-sm text-mo-muted">
                    {insight}
                  </p>
                ))}
              </div>
            </article>
          </div>
        </>
      )}
    </section>
  )
}

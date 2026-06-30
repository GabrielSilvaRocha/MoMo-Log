import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { LoadingState } from '../components/LoadingState'
import { MetricCard } from '../components/MetricCard'
import { ProgressBar } from '../components/ProgressBar'
import { SectionHeader } from '../components/SectionHeader'
import type { ProductMvpStatus, ProductReleaseNotes } from '../types/api'

type MvpState = {
  status: ProductMvpStatus | null
  releaseNotes: ProductReleaseNotes | null
}

function statusLabel(value: string) {
  if (value === 'stable') return 'Estável'
  if (value === 'beta') return 'Beta'
  if (value === 'planned') return 'Planejado'
  return value
}

export function MvpPage() {
  const [state, setState] = useState<MvpState>({ status: null, releaseNotes: null })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        setLoading(true)
        const [status, releaseNotes] = await Promise.all([mo2logApi.productStatus(), mo2logApi.releaseNotes()])
        if (mounted) {
          setState({ status, releaseNotes })
          setError(null)
        }
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Erro ao carregar status do MVP')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    load()
    return () => {
      mounted = false
    }
  }, [])

  const averageCoverage = useMemo(() => {
    const flows = state.status?.user_flows ?? []
    if (!flows.length) return 0
    return Math.round(flows.reduce((total, flow) => total + flow.coverage, 0) / flows.length)
  }, [state.status])

  if (loading) return <LoadingState title="Carregando consolidação do MVP" description="Validando módulos e fluxos principais." />

  if (error) {
    return <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-8 text-red-100">{error}</div>
  }

  const status = state.status

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <SectionHeader
          eyebrow="MVP Consolidado"
          title={`${status?.app ?? 'Mo² LOG'} ${status?.version ?? ''}`}
          description="Esta tela resume o estado funcional do produto: módulos disponíveis, fluxos cobertos e prioridades da próxima milestone."
        />
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          <MetricCard label="Módulos ativos" value={String(status?.modules.length ?? 0)} hint="Cobertura funcional do MVP" icon="🧩" />
          <MetricCard label="Fluxos mapeados" value={String(status?.user_flows.length ?? 0)} hint="Jornadas principais do usuário" icon="✅" />
          <MetricCard label="Cobertura média" value={`${averageCoverage}%`} hint="Estimativa da maturidade funcional" icon="📌" />
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <SectionHeader title="Módulos do MVP" description="Todos os blocos abaixo já possuem backend e interface inicial no frontend." />
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            {(status?.modules ?? []).map((module) => (
              <article key={module.key} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="font-semibold text-white">{module.label}</h3>
                  <span className="rounded-full bg-mo-primary/10 px-3 py-1 text-xs font-semibold text-mo-primary">
                    {statusLabel(module.status)}
                  </span>
                </div>
                <p className="mt-3 text-sm leading-6 text-mo-muted">{module.description}</p>
              </article>
            ))}
          </div>
        </div>

        <div className="space-y-4">
          <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <SectionHeader title="Notas da release" />
            <p className="mt-3 font-semibold text-white">{state.releaseNotes?.title}</p>
            <div className="mt-4 space-y-3">
              {(state.releaseNotes?.highlights ?? []).map((item) => (
                <p key={item} className="rounded-2xl bg-white/[0.03] p-3 text-sm text-mo-muted">{item}</p>
              ))}
            </div>
          </section>

          <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
            <SectionHeader title="Próximas prioridades" />
            <div className="mt-4 space-y-3">
              {(status?.next_priorities ?? []).map((item) => (
                <p key={item} className="rounded-2xl bg-white/[0.03] p-3 text-sm text-mo-muted">{item}</p>
              ))}
            </div>
          </section>
        </div>
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <SectionHeader title="Checklist dos fluxos principais" description="A cobertura indica quanto do fluxo já está implementado no MVP atual." />
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {(status?.user_flows ?? []).map((flow) => (
            <div key={flow.key} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
              <div className="flex items-center justify-between gap-4">
                <p className="font-semibold text-white">{flow.label}</p>
                <span className="text-sm font-semibold text-mo-primary">{flow.coverage}%</span>
              </div>
              <div className="mt-3">
                <ProgressBar value={flow.coverage} label="" />
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}

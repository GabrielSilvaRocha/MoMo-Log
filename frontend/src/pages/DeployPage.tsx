import { useEffect, useMemo, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { LoadingState } from '../components/LoadingState'
import { MetricCard } from '../components/MetricCard'
import { SectionHeader } from '../components/SectionHeader'
import type { DeploymentChecklist, MobileSyncReadiness, OpsStatus } from '../types/api'

type DeployState = {
  ops: OpsStatus | null
  checklist: DeploymentChecklist | null
  mobile: MobileSyncReadiness | null
}

function statusStyle(status: string) {
  if (status === 'ok' || status === 'ready' || status === 'operational') return 'bg-mo-primary/10 text-mo-primary'
  if (status === 'planned' || status === 'optional' || status === 'external') return 'bg-amber-400/10 text-amber-200'
  return 'bg-red-500/10 text-red-200'
}

export function DeployPage() {
  const [state, setState] = useState<DeployState>({ ops: null, checklist: null, mobile: null })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        setLoading(true)
        const [ops, checklist, mobile] = await Promise.all([mo2logApi.opsStatus(), mo2logApi.deploymentChecklist(), mo2logApi.mobileSyncReadiness()])
        if (mounted) {
          setState({ ops, checklist, mobile })
          setError(null)
        }
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Erro ao carregar status operacional')
      } finally {
        if (mounted) setLoading(false)
      }
    }

    load()
    return () => {
      mounted = false
    }
  }, [])

  const readyItems = useMemo(() => {
    const items = state.checklist?.items ?? []
    return items.filter((item) => item.status === 'ready').length
  }, [state.checklist])

  if (loading) return <LoadingState title="Carregando status operacional" description="Validando serviços, checklist e prontidão para deploy." />

  if (error) {
    return <div className="rounded-3xl border border-red-500/40 bg-red-950/30 p-8 text-red-100">{error}</div>
  }

  const totalItems = state.checklist?.items.length ?? 0

  return (
    <main className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6 shadow-glow">
        <SectionHeader
          eyebrow="Deploy e portfólio"
          title="Prontidão operacional do Mo² LOG"
          description="Esta tela resume o estado técnico da aplicação para demonstração, publicação e apresentação como projeto de portfólio."
        />
        <div className="mt-6 grid gap-4 md:grid-cols-4">
          <MetricCard label="Versão" value={`v${state.ops?.version ?? '-'}`} hint={state.ops?.environment ?? 'ambiente'} icon="🚀" />
          <MetricCard label="Status" value={state.ops?.status ?? '-'} hint="Estado operacional da API" icon="🟢" />
          <MetricCard label="Serviços" value={String(state.ops?.services.length ?? 0)} hint="Backend, banco, frontend e integrações" icon="🧩" />
          <MetricCard label="Checklist" value={`${readyItems}/${totalItems}`} hint="Itens prontos para deploy" icon="✅" />
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-[1fr_1fr]">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <SectionHeader title="Serviços" description="Leitura operacional dos principais blocos técnicos." />
          <div className="mt-5 space-y-3">
            {(state.ops?.services ?? []).map((service) => (
              <article key={service.key} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="font-semibold text-white">{service.label}</h3>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle(service.status)}`}>{service.status}</span>
                </div>
                <p className="mt-2 text-sm text-mo-muted">{service.detail}</p>
              </article>
            ))}
          </div>
        </div>

        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <SectionHeader title={state.checklist?.title ?? 'Checklist'} description="Itens que reduzem risco antes de publicar o projeto." />
          <div className="mt-5 space-y-3">
            {(state.checklist?.items ?? []).map((item) => (
              <article key={item.key} className="flex items-center justify-between gap-4 rounded-2xl border border-mo-border bg-white/[0.03] p-4">
                <div>
                  <p className="font-semibold text-white">{item.label}</p>
                  {item.detail && <p className="mt-1 text-sm text-mo-muted">{item.detail}</p>}
                </div>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle(item.status)}`}>{item.status}</span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <SectionHeader title="Roteiro mobile" description="Preparação para Health Connect e Samsung Health." />
          <div className="mt-5 space-y-3">
            <p className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">Status: <strong className="text-white">{state.mobile?.status}</strong></p>
            {(state.mobile?.implementation_steps ?? []).map((step) => (
              <p key={step} className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">{step}</p>
            ))}
          </div>
        </div>
        <div className="rounded-3xl border border-mo-border bg-mo-surface p-5">
          <SectionHeader title="Roteiro de demo" description="Sequência pronta para apresentar o projeto." />
          <div className="mt-5 space-y-3">
            {(state.checklist?.demo_script ?? []).map((step, index) => (
              <p key={step} className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">{index + 1}. {step}</p>
            ))}
          </div>
        </div>
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <SectionHeader title="Screenshots-alvo" description={state.checklist?.portfolio_summary ?? 'Pacote visual para portfólio.'} />
        <div className="mt-5 grid gap-3 md:grid-cols-2 lg:grid-cols-5">
          {(state.checklist?.screenshot_targets ?? []).map((target) => (
            <div key={target.key} className="rounded-2xl border border-mo-border bg-white/[0.03] p-4">
              <p className="font-semibold text-white">{target.label}</p>
              <p className="mt-2 text-sm text-mo-muted">{target.route}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-3xl border border-mo-border bg-mo-surface p-5">
        <SectionHeader title="Próximos alvos recomendados" description="Passos para transformar o Mo² LOG em uma demonstração pública." />
        <div className="mt-5 grid gap-3 md:grid-cols-2">
          {(state.checklist?.recommended_next_targets ?? []).map((target) => (
            <p key={target} className="rounded-2xl bg-white/[0.03] p-4 text-sm text-mo-muted">{target}</p>
          ))}
        </div>
      </section>
    </main>
  )
}

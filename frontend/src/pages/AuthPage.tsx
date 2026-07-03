import { FormEvent, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { saveAuthSession, saveOfflineSession } from '../auth/session'
import type { AuthToken } from '../types/api'

type AuthPageProps = {
  onAuthenticated: (auth: AuthToken) => void
}

type Mode = 'login' | 'register'

export function AuthPage({ onAuthenticated }: AuthPageProps) {
  const [mode, setMode] = useState<Mode>('login')
  const [name, setName] = useState('Gabriel Rocha')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function completeAuth(auth: AuthToken) {
    saveAuthSession(auth)
    onAuthenticated(auth)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      setLoading(true)
      const auth = mode === 'login'
        ? await mo2logApi.login({ email, password })
        : await mo2logApi.register({ name, email, password })
      await completeAuth(auth)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível autenticar')
    } finally {
      setLoading(false)
    }
  }

  function handleOfflineLogin() {
    const user = saveOfflineSession()
    onAuthenticated({
      access_token: 'offline-local-token',
      token_type: 'bearer',
      user,
    })
  }

  async function handleDemoLogin() {
    try {
      setLoading(true)
      const auth = await mo2logApi.demoLogin()
      await completeAuth(auth)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível entrar com demo')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen bg-mo-background px-4 py-10 text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_right,rgba(119,255,107,0.18),transparent_30rem)]" />
      <section className="relative mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
        <div className="rounded-[2rem] border border-mo-border bg-mo-surface p-8 shadow-glow">
          <p className="text-sm uppercase tracking-[0.4em] text-mo-primary">Mo² LOG</p>
          <h1 className="mt-4 text-4xl font-black tracking-tight md:text-6xl">Seu centro de treino híbrido.</h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-mo-muted">
            Planeje musculação, registre corridas na esteira, adapte exercícios quando a academia estiver cheia e acompanhe sua evolução com dados próprios.
          </p>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            {['Musculação', 'Corrida de esteira', 'Adaptação inteligente'].map((item) => (
              <div key={item} className="rounded-3xl border border-mo-border bg-black/20 p-4">
                <span className="text-2xl">●</span>
                <p className="mt-3 font-semibold text-white">{item}</p>
              </div>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="relative rounded-[2rem] border border-mo-border bg-mo-surfaceElevated p-6 shadow-glow">
          <div className="mb-6 flex rounded-2xl bg-black/30 p-1">
            <button type="button" onClick={() => setMode('login')} className={`flex-1 rounded-xl px-4 py-3 text-sm font-semibold ${mode === 'login' ? 'bg-mo-primary text-black' : 'text-mo-muted'}`}>
              Entrar
            </button>
            <button type="button" onClick={() => setMode('register')} className={`flex-1 rounded-xl px-4 py-3 text-sm font-semibold ${mode === 'register' ? 'bg-mo-primary text-black' : 'text-mo-muted'}`}>
              Cadastrar
            </button>
          </div>

          <h2 className="text-2xl font-bold">{mode === 'login' ? 'Entrar no Mo² LOG' : 'Criar conta'}</h2>
          <p className="mt-2 text-sm text-mo-muted">Use suas credenciais locais ou o botão demo para acessar o planejamento já populado.</p>

          {mode === 'register' && (
            <label className="mt-5 block text-sm text-mo-muted">
              Nome
              <input value={name} onChange={(event) => setName(event.target.value)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
            </label>
          )}

          <label className="mt-5 block text-sm text-mo-muted">
            E-mail
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>

          <label className="mt-4 block text-sm text-mo-muted">
            Senha
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>

          {error && <p className="mt-4 rounded-2xl border border-red-400/30 bg-red-950/30 p-3 text-sm text-red-200">{error}</p>}

          <button disabled={loading} className="mt-6 w-full rounded-2xl bg-mo-primary px-5 py-3 font-bold text-black shadow-glow disabled:opacity-60">
            {loading ? 'Processando...' : mode === 'login' ? 'Entrar' : 'Criar conta'}
          </button>

          <button type="button" disabled={loading} onClick={handleDemoLogin} className="mt-3 w-full rounded-2xl border border-mo-border bg-white/[0.03] px-5 py-3 font-semibold text-white hover:border-mo-primary disabled:opacity-60">
            Entrar como Demo Local
          </button>

          <button type="button" disabled={loading} onClick={handleOfflineLogin} className="mt-3 w-full rounded-2xl border border-mo-primary/50 bg-mo-primary/10 px-5 py-3 font-semibold text-mo-primary hover:bg-mo-primary hover:text-black disabled:opacity-60">
            Usar modo academia offline
          </button>
        </form>
      </section>
    </main>
  )
}

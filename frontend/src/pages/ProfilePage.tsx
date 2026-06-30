import { FormEvent, useEffect, useState } from 'react'

import { mo2logApi } from '../api/mo2log'
import { getCurrentUserId, saveAuthenticatedUser, savePreferences } from '../auth/session'
import { LoadingState } from '../components/LoadingState'
import type { AuthenticatedUser, UserPreference } from '../types/api'

const sourceOptions = [
  { value: 'manual_treadmill', label: 'Esteira manual' },
  { value: 'manual_outdoor', label: 'Corrida manual externa' },
  { value: 'strava', label: 'Strava opcional' },
  { value: 'samsung_health', label: 'Samsung Health futuro' },
  { value: 'health_connect', label: 'Health Connect futuro' },
]

export function ProfilePage() {
  const [profile, setProfile] = useState<AuthenticatedUser | null>(null)
  const [name, setName] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [preferences, setPreferences] = useState<UserPreference | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      setLoading(true)
      const data = await mo2logApi.profile(getCurrentUserId())
      setProfile(data)
      setName(data.user.name)
      setAvatarUrl(data.user.avatar_url ?? '')
      setPreferences(data.preferences)
      saveAuthenticatedUser(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar perfil')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      setSaving(true)
      const user = await mo2logApi.updateProfile(getCurrentUserId(), { name, avatar_url: avatarUrl || null })
      if (profile) {
        const updated = { ...profile, user }
        setProfile(updated)
        saveAuthenticatedUser(updated)
      }
      setMessage('Perfil atualizado com sucesso.')
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao salvar perfil')
    } finally {
      setSaving(false)
    }
  }

  async function savePreferencePatch(patch: Partial<UserPreference>) {
    try {
      setSaving(true)
      const updated = await mo2logApi.updatePreferences(getCurrentUserId(), patch)
      setPreferences(updated)
      savePreferences(updated)
      setMessage('Preferências atualizadas.')
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao salvar preferências')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingState title="Carregando perfil" description="Buscando dados e preferências do usuário." />

  return (
    <div className="space-y-6">
      <section className="rounded-3xl border border-mo-border bg-mo-surface p-6">
        <p className="text-sm uppercase tracking-[0.28em] text-mo-primary">Perfil</p>
        <h2 className="mt-2 text-3xl font-bold text-white">Conta e preferências</h2>
        <p className="mt-2 text-sm text-mo-muted">Configure sua fonte padrão de corrida, metas semanais e observações da academia.</p>
      </section>

      {error && <p className="rounded-2xl border border-red-400/30 bg-red-950/30 p-4 text-red-200">{error}</p>}
      {message && <p className="rounded-2xl border border-mo-primary/30 bg-mo-primary/10 p-4 text-mo-primary">{message}</p>}

      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={saveProfile} className="rounded-3xl border border-mo-border bg-mo-surface p-6">
          <h3 className="text-xl font-semibold text-white">Dados pessoais</h3>
          <label className="mt-5 block text-sm text-mo-muted">
            Nome
            <input value={name} onChange={(event) => setName(event.target.value)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>
          <label className="mt-4 block text-sm text-mo-muted">
            Avatar URL
            <input value={avatarUrl} onChange={(event) => setAvatarUrl(event.target.value)} placeholder="Opcional" className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>
          <p className="mt-4 text-sm text-mo-muted">E-mail: <span className="text-white">{profile?.user.email}</span></p>
          <button disabled={saving} className="mt-5 rounded-2xl bg-mo-primary px-5 py-3 font-bold text-black shadow-glow disabled:opacity-60">Salvar perfil</button>
        </form>

        <section className="rounded-3xl border border-mo-border bg-mo-surface p-6">
          <h3 className="text-xl font-semibold text-white">Preferências de treino</h3>
          <label className="mt-5 block text-sm text-mo-muted">
            Fonte padrão de corrida
            <select value={preferences?.default_running_source ?? 'manual_treadmill'} onChange={(event) => savePreferencePatch({ default_running_source: event.target.value } as Partial<UserPreference>)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary">
              {sourceOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <label className="block text-sm text-mo-muted">
              Meta corrida semanal (km)
              <input type="number" value={preferences?.weekly_running_goal_km ?? ''} onChange={(event) => savePreferencePatch({ weekly_running_goal_km: Number(event.target.value) } as Partial<UserPreference>)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
            </label>
            <label className="block text-sm text-mo-muted">
              Musculação semanal
              <input type="number" value={preferences?.weekly_strength_goal_sessions ?? ''} onChange={(event) => savePreferencePatch({ weekly_strength_goal_sessions: Number(event.target.value) } as Partial<UserPreference>)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
            </label>
          </div>
          <label className="mt-5 block text-sm text-mo-muted">
            Dias preferenciais
            <input value={preferences?.preferred_training_days ?? ''} onChange={(event) => savePreferencePatch({ preferred_training_days: event.target.value } as Partial<UserPreference>)} className="mt-2 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>
          <label className="mt-5 block text-sm text-mo-muted">
            Observações da academia
            <textarea value={preferences?.gym_notes ?? ''} onChange={(event) => savePreferencePatch({ gym_notes: event.target.value } as Partial<UserPreference>)} className="mt-2 min-h-28 w-full rounded-2xl border border-mo-border bg-black/30 px-4 py-3 text-white outline-none focus:border-mo-primary" />
          </label>
        </section>
      </div>
    </div>
  )
}

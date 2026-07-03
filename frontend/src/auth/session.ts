import type { AuthToken, AuthenticatedUser } from '../types/api'

const TOKEN_KEY = 'mo2log.accessToken'
const USER_KEY = 'mo2log.user'
const PREFERENCES_KEY = 'mo2log.preferences'

export function saveAuthSession(auth: AuthToken) {
  localStorage.setItem(TOKEN_KEY, auth.access_token)
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
}

export function saveOfflineSession() {
  const user: AuthToken['user'] = {
    id: 1,
    name: 'Modo Offline',
    email: 'offline@mo2log.local',
    avatar_url: null,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  }
  localStorage.setItem(TOKEN_KEY, 'offline-local-token')
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function saveAuthenticatedUser(authenticated: AuthenticatedUser) {
  localStorage.setItem(USER_KEY, JSON.stringify(authenticated.user))
  if (authenticated.preferences) {
    localStorage.setItem(PREFERENCES_KEY, JSON.stringify(authenticated.preferences))
  }
}

export function savePreferences(preferences: unknown) {
  localStorage.setItem(PREFERENCES_KEY, JSON.stringify(preferences))
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getCurrentUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthToken['user']
  } catch {
    return null
  }
}

export function getCurrentUserId(): number {
  return getCurrentUser()?.id ?? 1
}

export function clearAuthSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(PREFERENCES_KEY)
}

export function hasAuthSession(): boolean {
  return Boolean(getAccessToken() && getCurrentUser())
}

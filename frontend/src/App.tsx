import { useEffect, useState } from 'react'

import { AppShell } from './layouts/AppShell'
import { clearAuthSession, getCurrentUser, hasAuthSession, saveAuthenticatedUser, saveOfflineSession } from './auth/session'
import { AdaptationPage } from './pages/AdaptationPage'
import { AuthPage } from './pages/AuthPage'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { DashboardPage } from './pages/DashboardPage'
import { DeployPage } from './pages/DeployPage'
import { ExerciseLibraryPage } from './pages/ExerciseLibraryPage'
import { GoalsPage } from './pages/GoalsPage'
import { HistoryPage } from './pages/HistoryPage'
import { IntelligencePage } from './pages/IntelligencePage'
import { ReportsPage } from './pages/ReportsPage'
import { TemplatesPage } from './pages/TemplatesPage'
import { MvpPage } from './pages/MvpPage'
import { PlanningPage } from './pages/PlanningPage'
import { ProfilePage } from './pages/ProfilePage'
import { RunningPage } from './pages/RunningPage'
import { WorkoutPage } from './pages/WorkoutPage'
import { OfflineWorkoutPage } from './pages/OfflineWorkoutPage'
import { mo2logApi } from './api/mo2log'
import type { AuthToken, User } from './types/api'

export type AppView = 'dashboard' | 'planning' | 'templates' | 'workout' | 'offline-workout' | 'running' | 'analytics' | 'intelligence' | 'history' | 'reports' | 'goals' | 'exercises' | 'adaptation' | 'profile' | 'mvp' | 'deploy'

const views: AppView[] = ['dashboard', 'planning', 'templates', 'workout', 'offline-workout', 'running', 'analytics', 'intelligence', 'history', 'reports', 'goals', 'exercises', 'adaptation', 'profile', 'mvp', 'deploy']
const OFFLINE_BOOT_VIEW: AppView = 'offline-workout'
const isAndroidAssetHost = window.location.hostname === 'appassets.androidplatform.net'

export default function App() {
  const [view, setView] = useState<AppView>(() => isAndroidAssetHost ? OFFLINE_BOOT_VIEW : 'dashboard')
  const [user, setUser] = useState<User | null>(getCurrentUser())
  const [authReady, setAuthReady] = useState(false)

  useEffect(() => {
    if (isAndroidAssetHost) {
      setView(OFFLINE_BOOT_VIEW)
      setUser(saveOfflineSession())
      window.history.replaceState({}, '', window.location.pathname)
      return
    }

    const queryView = new URLSearchParams(window.location.search).get('view')
    if (queryView && views.includes(queryView as AppView)) {
      const nextView = queryView as AppView
      setView(nextView)
      if (nextView === OFFLINE_BOOT_VIEW && !getCurrentUser()) {
        setUser(saveOfflineSession())
      }
      window.history.replaceState({}, '', window.location.pathname)
    }
  }, [])

  useEffect(() => {
    let mounted = true
    async function loadSession() {
      if (isAndroidAssetHost) {
        if (mounted) setUser(saveOfflineSession())
        setAuthReady(true)
        return
      }

      const queryView = new URLSearchParams(window.location.search).get('view')
      if (queryView === OFFLINE_BOOT_VIEW && !hasAuthSession()) {
        if (mounted) setUser(saveOfflineSession())
        setAuthReady(true)
        return
      }

      if (!hasAuthSession()) {
        setAuthReady(true)
        return
      }
      try {
        const data = await mo2logApi.me()
        if (mounted) {
          saveAuthenticatedUser(data)
          setUser(data.user)
        }
      } catch {
        const cachedUser = getCurrentUser()
        if (mounted && cachedUser) {
          setUser(cachedUser)
        } else {
          clearAuthSession()
          if (mounted) setUser(null)
        }
      } finally {
        if (mounted) setAuthReady(true)
      }
    }
    loadSession()
    return () => {
      mounted = false
    }
  }, [])

  function handleAuthenticated(auth: AuthToken) {
    setUser(auth.user)
  }

  function handleLogout() {
    if (isAndroidAssetHost) {
      setUser(saveOfflineSession())
      setView(OFFLINE_BOOT_VIEW)
      return
    }

    clearAuthSession()
    setUser(null)
    setView('dashboard')
  }

  function handleNavigate(nextView: AppView) {
    setView(isAndroidAssetHost ? OFFLINE_BOOT_VIEW : nextView)
  }

  if (!authReady) {
    return <div className="min-h-screen bg-mo-background p-10 text-mo-muted">Carregando sessão do Mo² LOG...</div>
  }

  if (!user) {
    return <AuthPage onAuthenticated={handleAuthenticated} />
  }

  return (
    <AppShell currentView={view} onNavigate={handleNavigate} user={user} onLogout={handleLogout} offlineOnly={isAndroidAssetHost}>
      {view === 'dashboard' && <DashboardPage onStartWorkout={() => setView('workout')} />}
      {view === 'planning' && <PlanningPage />}
      {view === 'templates' && <TemplatesPage />}
      {view === 'workout' && <WorkoutPage />}
      {view === 'offline-workout' && <OfflineWorkoutPage />}
      {view === 'running' && <RunningPage />}
      {view === 'analytics' && <AnalyticsPage />}
      {view === 'intelligence' && <IntelligencePage />}
      {view === 'history' && <HistoryPage />}
      {view === 'reports' && <ReportsPage />}
      {view === 'goals' && <GoalsPage />}
      {view === 'exercises' && <ExerciseLibraryPage />}
      {view === 'adaptation' && <AdaptationPage />}
      {view === 'profile' && <ProfilePage />}
      {view === 'mvp' && <MvpPage />}
      {view === 'deploy' && <DeployPage />}
    </AppShell>
  )
}

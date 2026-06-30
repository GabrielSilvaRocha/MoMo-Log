import { useEffect, useState } from 'react'

import { AppShell } from './layouts/AppShell'
import { clearAuthSession, getCurrentUser, hasAuthSession, saveAuthenticatedUser } from './auth/session'
import { AdaptationPage } from './pages/AdaptationPage'
import { AuthPage } from './pages/AuthPage'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExerciseLibraryPage } from './pages/ExerciseLibraryPage'
import { GoalsPage } from './pages/GoalsPage'
import { HistoryPage } from './pages/HistoryPage'
import { ReportsPage } from './pages/ReportsPage'
import { MvpPage } from './pages/MvpPage'
import { PlanningPage } from './pages/PlanningPage'
import { ProfilePage } from './pages/ProfilePage'
import { RunningPage } from './pages/RunningPage'
import { WorkoutPage } from './pages/WorkoutPage'
import { mo2logApi } from './api/mo2log'
import type { AuthToken, User } from './types/api'

export type AppView = 'dashboard' | 'planning' | 'workout' | 'running' | 'analytics' | 'history' | 'reports' | 'goals' | 'exercises' | 'adaptation' | 'profile' | 'mvp'

const views: AppView[] = ['dashboard', 'planning', 'workout', 'running', 'analytics', 'history', 'reports', 'goals', 'exercises', 'adaptation', 'profile', 'mvp']

export default function App() {
  const [view, setView] = useState<AppView>('dashboard')
  const [user, setUser] = useState<User | null>(getCurrentUser())
  const [authReady, setAuthReady] = useState(false)

  useEffect(() => {
    const queryView = new URLSearchParams(window.location.search).get('view')
    if (queryView && views.includes(queryView as AppView)) {
      setView(queryView as AppView)
      window.history.replaceState({}, '', window.location.pathname)
    }
  }, [])

  useEffect(() => {
    let mounted = true
    async function loadSession() {
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
        clearAuthSession()
        if (mounted) setUser(null)
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
    clearAuthSession()
    setUser(null)
    setView('dashboard')
  }

  if (!authReady) {
    return <div className="min-h-screen bg-mo-background p-10 text-mo-muted">Carregando sessão do Mo² LOG...</div>
  }

  if (!user) {
    return <AuthPage onAuthenticated={handleAuthenticated} />
  }

  return (
    <AppShell currentView={view} onNavigate={setView} user={user} onLogout={handleLogout}>
      {view === 'dashboard' && <DashboardPage onStartWorkout={() => setView('workout')} />}
      {view === 'planning' && <PlanningPage />}
      {view === 'workout' && <WorkoutPage />}
      {view === 'running' && <RunningPage />}
      {view === 'analytics' && <AnalyticsPage />}
      {view === 'history' && <HistoryPage />}
      {view === 'reports' && <ReportsPage />}
      {view === 'goals' && <GoalsPage />}
      {view === 'exercises' && <ExerciseLibraryPage />}
      {view === 'adaptation' && <AdaptationPage />}
      {view === 'profile' && <ProfilePage />}
      {view === 'mvp' && <MvpPage />}
    </AppShell>
  )
}

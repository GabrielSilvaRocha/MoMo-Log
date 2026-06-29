import { useEffect, useState } from 'react'

import { AppShell } from './layouts/AppShell'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExerciseLibraryPage } from './pages/ExerciseLibraryPage'
import { GoalsPage } from './pages/GoalsPage'
import { RunningPage } from './pages/RunningPage'
import { WorkoutPage } from './pages/WorkoutPage'

export type AppView = 'dashboard' | 'workout' | 'running' | 'analytics' | 'goals' | 'exercises'

export default function App() {
  const [view, setView] = useState<AppView>('dashboard')

  useEffect(() => {
    const queryView = new URLSearchParams(window.location.search).get('view')
    if (queryView && ['dashboard', 'workout', 'running', 'analytics', 'goals', 'exercises'].includes(queryView)) {
      setView(queryView as AppView)
      window.history.replaceState({}, '', window.location.pathname)
    }
  }, [])

  return (
    <AppShell currentView={view} onNavigate={setView}>
      {view === 'dashboard' && <DashboardPage onStartWorkout={() => setView('workout')} />}
      {view === 'workout' && <WorkoutPage />}
      {view === 'running' && <RunningPage />}
      {view === 'analytics' && <AnalyticsPage />}
      {view === 'goals' && <GoalsPage />}
      {view === 'exercises' && <ExerciseLibraryPage />}
    </AppShell>
  )
}

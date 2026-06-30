import { useEffect, useState } from 'react'

import { AppShell } from './layouts/AppShell'
import { AdaptationPage } from './pages/AdaptationPage'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExerciseLibraryPage } from './pages/ExerciseLibraryPage'
import { GoalsPage } from './pages/GoalsPage'
import { HistoryPage } from './pages/HistoryPage'
import { ReportsPage } from './pages/ReportsPage'
import { PlanningPage } from './pages/PlanningPage'
import { RunningPage } from './pages/RunningPage'
import { WorkoutPage } from './pages/WorkoutPage'

export type AppView = 'dashboard' | 'planning' | 'workout' | 'running' | 'analytics' | 'history' | 'reports' | 'goals' | 'exercises' | 'adaptation'

export default function App() {
  const [view, setView] = useState<AppView>('dashboard')

  useEffect(() => {
    const queryView = new URLSearchParams(window.location.search).get('view')
    if (queryView && ['dashboard', 'planning', 'workout', 'running', 'analytics', 'history', 'reports', 'goals', 'exercises', 'adaptation'].includes(queryView)) {
      setView(queryView as AppView)
      window.history.replaceState({}, '', window.location.pathname)
    }
  }, [])

  return (
    <AppShell currentView={view} onNavigate={setView}>
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
    </AppShell>
  )
}

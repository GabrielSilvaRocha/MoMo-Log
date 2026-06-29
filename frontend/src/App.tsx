import { useState } from 'react'

import { AppShell } from './layouts/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { ExerciseLibraryPage } from './pages/ExerciseLibraryPage'
import { WorkoutPage } from './pages/WorkoutPage'

export type AppView = 'dashboard' | 'workout' | 'exercises'

export default function App() {
  const [view, setView] = useState<AppView>('dashboard')

  return (
    <AppShell currentView={view} onNavigate={setView}>
      {view === 'dashboard' && <DashboardPage onStartWorkout={() => setView('workout')} />}
      {view === 'workout' && <WorkoutPage />}
      {view === 'exercises' && <ExerciseLibraryPage />}
    </AppShell>
  )
}

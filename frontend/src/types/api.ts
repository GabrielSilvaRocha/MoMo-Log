export type ApiStatus = {
  status: string
  app: string
  environment: string
  version: string
}

export type TrainingSession = {
  id: number
  user_id: number
  training_plan_id: number | null
  session_type: 'strength' | 'running' | 'mobility' | 'rest'
  title: string
  scheduled_date: string
  status: string
  source: string
  notes: string | null
}

export type WeekDashboard = {
  user_id: number
  completed_sessions: TrainingSession[]
  today_sessions: TrainingSession[]
  upcoming_sessions: TrainingSession[]
  weekly_strength_volume: string
  weekly_running_distance_km: string
  completion_rate: number
}

export type Goal = {
  id: number
  user_id: number
  goal_type: string
  title: string
  target_value: string
  current_value: string
  unit: string
  deadline: string | null
  status: string
  progress_percentage: number | null
}

export type PersonalRecord = {
  id: number
  user_id: number
  record_type: string
  title: string
  value: string
  unit: string
}

export type WeeklyStatistics = {
  user_id: number
  reference_date: string
  week_start: string
  week_end: string
  completed_sessions: number
  upcoming_sessions: number
  adapted_sessions: number
  strength_sessions_completed: number
  running_sessions_completed: number
  weekly_strength_volume: string
  weekly_running_distance_km: string
  completion_rate: number
  active_goals: Goal[]
  personal_records: PersonalRecord[]
  insights: string[]
}

export type StravaStatus = {
  connected: boolean
  user_id: number
  strava_athlete_id?: string
  token_expires_at?: string
}

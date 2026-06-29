export type ApiStatus = {
  status: string
  app: string
  environment: string
  version: string
}

export type Exercise = {
  id: number
  name: string
  slug: string
  description: string | null
  execution_instructions: string | null
  difficulty: string
  exercise_type: string
  is_unilateral: boolean
  is_compound: boolean
}

export type ExerciseAlternative = {
  id: number
  exercise_id: number
  alternative_exercise_id: number
  alternative_exercise: Exercise
  equivalence_score: number
  reason: string | null
  equipment_status: 'unavailable' | 'frequently_busy' | 'favorite' | null
  is_default_suggestion: boolean
}

export type StrengthSetLog = {
  id: number
  strength_workout_exercise_id: number
  set_number: number
  reps: number
  load: string
  rir: number | null
  rpe: number | null
  completed_at: string
}

export type StrengthWorkoutExercise = {
  id: number
  training_session_id: number
  exercise_id: number
  order_index: number
  planned_sets: number
  planned_reps: string
  planned_load: string | null
  rest_seconds: number | null
  notes: string | null
  exercise: Exercise | null
  set_logs: StrengthSetLog[]
}

export type RunningActivity = {
  id: number
  user_id: number
  training_session_id: number | null
  strava_activity_id: string | null
  name: string
  distance_m: string
  moving_time_s: number
  elapsed_time_s: number
  average_speed: string
  average_pace: string
  max_speed: string | null
  total_elevation_gain: string | null
  activity_type?: string
  source?: string
  start_date: string
}

export type TrainingSession = {
  id: number
  user_id: number
  training_plan_id: number | null
  session_type: 'strength' | 'running' | 'mobility' | 'rest'
  title: string
  scheduled_date: string
  started_at?: string | null
  finished_at?: string | null
  status: string
  source: string
  notes: string | null
  strength_exercises?: StrengthWorkoutExercise[]
  running_activity?: RunningActivity | null
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

export type StravaSyncResult = {
  imported: number
  updated: number
  ignored: number
  status: string
  message: string
}

export type Equipment = {
  id: number
  name: string
  category: string
}

export type UserGymEquipment = {
  id: number
  user_id: number
  equipment_id: number
  status: string
  notes: string | null
}

export type ExerciseSwapResult = {
  status: string
  equivalence_score: number | null
  message: string
}

export type StravaAuthorizeResponse = {
  authorization_url: string
  configured: boolean
  scope: string
  redirect_uri: string
}


export type ManualRunningActivityPayload = {
  user_id: number
  training_session_id?: number | null
  name: string
  distance_m: number
  moving_time_s: number
  elapsed_time_s: number
  activity_type?: string
  source?: string
  start_date: string
  total_elevation_gain?: number | null
}


export type TrainingSessionCreatePayload = {
  user_id: number
  training_plan_id?: number | null
  session_type: 'strength' | 'running' | 'mobility' | 'rest'
  title: string
  scheduled_date: string
  source?: string
  notes?: string | null
}

export type TrainingSessionUpdatePayload = {
  title?: string
  scheduled_date?: string
  status?: 'planned' | 'in_progress' | 'completed' | 'skipped' | 'rescheduled' | 'adapted'
  source?: string
  notes?: string | null
}

export type StrengthWorkoutExerciseCreatePayload = {
  exercise_id: number
  order_index?: number | null
  planned_sets: number
  planned_reps: string
  planned_load?: number | null
  rest_seconds?: number | null
  notes?: string | null
}

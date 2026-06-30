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


export type AdaptationSuggestion = ExerciseAlternative & {
  recommendation_score: number
  recommendation_label: string
  badges: string[]
  equipment_names: string[]
  penalties: string[]
  bonuses: string[]
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


export type RunningGoal = {
  id: number
  user_id: number
  goal_type: string
  race_distance_km: string
  race_date: string
  current_5k_time_seconds: number | null
  target_5k_time_seconds: number | null
  training_location: string
  available_weekdays: string
  status: string
}

export type RunningWorkoutStep = {
  id: number
  running_plan_session_id: number
  order_index: number
  step_type: string
  title: string
  target_distance_m: number | null
  target_duration_seconds: number | null
  target_pace_seconds_per_km: number | null
  target_speed_kmh: string | null
  rest_seconds: number | null
  notes: string | null
}

export type RunningPlanSession = {
  id: number
  user_id: number
  goal_id: number
  training_session_id: number | null
  session_type: string
  title: string
  scheduled_date: string
  description: string | null
  target_distance_km: string | null
  target_duration_seconds: number | null
  target_pace_seconds_per_km: number | null
  target_speed_kmh: string | null
  status: string
  steps: RunningWorkoutStep[]
}

export type RunningExecution = {
  id: number
  running_plan_session_id: number
  started_at: string
  finished_at: string | null
  status: string
  total_distance_km: string | null
  total_duration_seconds: number | null
  average_speed_kmh: string | null
  average_pace_seconds_per_km: number | null
}

export type RunningStepLog = {
  id: number
  running_execution_log_id: number
  running_workout_step_id: number
  planned_speed_kmh: string | null
  actual_speed_kmh: string | null
  started_at: string
  finished_at: string | null
  completed: boolean
}

export type RunningSpeedAdjustment = {
  id: number
  running_step_log_id: number
  adjustment_type: string
  previous_speed_kmh: string | null
  new_speed_kmh: string | null
  created_at: string
}

export type RunningGoalPayload = {
  user_id: number
  goal_type?: string
  race_distance_km: number
  race_date: string
  current_5k_time_seconds?: number | null
  target_5k_time_seconds?: number | null
  training_location?: string
  available_weekdays?: string
}

export type RunningPlanGenerateResponse = {
  goal_id: number
  created_sessions: number
  message: string
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

export type HistorySummary = {
  user_id: number
  date_from: string
  date_to: string
  total_sessions: number
  completed_sessions: number
  adapted_sessions: number
  skipped_sessions: number
  strength_sessions: number
  running_sessions: number
  total_sets: number
  strength_volume: string
  running_activities: number
  running_distance_km: string
  running_time_s: number
  average_pace: string | null
  completion_rate: number
}


export type ReportOverview = {
  user_id: number
  date_from: string
  date_to: string
  total_sessions: number
  completed_sessions: number
  adapted_sessions: number
  skipped_sessions: number
  strength_sessions: number
  running_sessions: number
  strength_volume: string
  total_sets: number
  average_rpe: number | null
  running_activities: number
  treadmill_runs: number
  running_distance_km: string
  running_time_s: number
  average_pace: string | null
  completion_rate: number
  insights: string[]
}


export type ProductModuleStatus = {
  key: string
  label: string
  status: 'stable' | 'beta' | 'planned' | string
  description: string
}

export type ProductUserFlow = {
  key: string
  label: string
  coverage: number
}

export type ProductMvpStatus = {
  app: string
  version: string
  milestone: string
  status: string
  modules: ProductModuleStatus[]
  user_flows: ProductUserFlow[]
  next_priorities: string[]
}

export type ProductReleaseNotes = {
  version: string
  title: string
  highlights: string[]
}


export type IntelligenceInsight = {
  type: string
  severity: 'positive' | 'attention' | 'info' | string
  title: string
  message: string
}

export type IntelligenceRecommendation = {
  title: string
  action: string
}

export type WeeklyIntelligence = {
  user_id: number
  reference_date: string
  week_start: string
  week_end: string
  hybrid_score: number
  summary: {
    planned_sessions: number
    completed_sessions: number
    completion_rate: number
    adapted_sessions: number
    skipped_sessions: number
    strength_sessions: number
    running_sessions: number
    strength_volume: string
    running_distance_km: string
    running_activities: number
    average_pace: string | null
    average_pace_label: string | null
    average_rpe: number | null
  }
  insights: IntelligenceInsight[]
  recommendations: IntelligenceRecommendation[]
}

export type PlannedVsDone = {
  user_id: number
  week_start: string
  week_end: string
  by_type: Record<string, { planned: number; completed: number; adapted: number; skipped: number }>
  running_registered_km: string
  days: Array<{
    date: string
    sessions: Array<{ id: number; title: string; session_type: string; status: string }>
  }>
}

export type FiveKForecast = {
  user_id: number
  confidence: string
  predicted_5k_time_s: number
  predicted_5k_time_label: string | null
  predicted_pace_label: string | null
  based_on_runs: number
  notes: string[]
  samples: Array<{
    activity_id: number
    name: string
    source: string
    distance_km: number
    pace_label: string | null
    relevance: string
  }>
}

export type User = {
  id: number
  name: string
  email: string
  avatar_url: string | null
  created_at: string
  updated_at: string
}

export type UserPreference = {
  id: number
  user_id: number
  default_running_source: string
  preferred_training_days: string | null
  weekly_running_goal_km: number | null
  weekly_strength_goal_sessions: number | null
  gym_notes: string | null
  created_at: string
  updated_at: string
}

export type AuthToken = {
  access_token: string
  token_type: string
  user: User
}

export type AuthenticatedUser = {
  user: User
  preferences: UserPreference | null
}

export type RegisterPayload = {
  name: string
  email: string
  password: string
  avatar_url?: string | null
}

export type LoginPayload = {
  email: string
  password: string
}

export type UserProfileUpdatePayload = {
  name?: string
  avatar_url?: string | null
}

export type UserPreferenceUpdatePayload = {
  default_running_source?: string
  preferred_training_days?: string | null
  weekly_running_goal_km?: number | null
  weekly_strength_goal_sessions?: number | null
  gym_notes?: string | null
}

export type OpsServiceStatus = {
  key: string
  label: string
  status: string
  detail: string
}

export type OpsStatus = {
  app: string
  version: string
  environment: string
  status: string
  services: OpsServiceStatus[]
}

export type DeploymentChecklistItem = {
  key: string
  label: string
  status: string
}

export type DeploymentChecklist = {
  version: string
  title: string
  items: DeploymentChecklistItem[]
  recommended_next_targets: string[]
}

export type WorkoutTemplateExercise = {
  id: number
  workout_template_id: number
  exercise_id: number
  order_index: number
  planned_sets: number
  planned_reps: string
  rest_seconds: number | null
  notes: string | null
  exercise: Exercise | null
}

export type WorkoutTemplate = {
  id: number
  user_id: number
  name: string
  description: string | null
  goal: string | null
  difficulty: string
  estimated_duration_minutes: number | null
  status: string
  exercises: WorkoutTemplateExercise[]
}

export type WorkoutTemplateSchedulePayload = {
  user_id: number
  scheduled_date: string
  training_plan_id?: number | null
  title?: string | null
  notes?: string | null
}

export type WorkoutTemplateScheduleResponse = {
  status: string
  message: string
  session: TrainingSession
}

import { apiDelete, apiGet, apiPatch, apiPost } from './client'
import { getCurrentUserId } from '../auth/session'
import type {
  AdaptationSuggestion,
  ApiStatus,
  AuthenticatedUser,
  AuthToken,
  Equipment,
  Exercise,
  ExerciseAlternative,
  ExerciseSwapResult,
  Goal,
  LoginPayload,
  HistorySummary,
  WeeklyIntelligence,
  PlannedVsDone,
  FiveKForecast,
  ReportOverview,
  ProductMvpStatus,
  ProductReleaseNotes,
  OpsStatus,
  DeploymentChecklist,
  AndroidSyncPlan,
  CloudDemoReadiness,
  MobileSyncReadiness,
  NextWorkoutReadiness,
  PersonalRecord,
  ManualRunningActivityPayload,
  RegisterPayload,
  RaceForecast,
  RunningActivity,
  RunningGoal,
  RunningGoalPayload,
  RunningPlanGenerateResponse,
  RunningPlanSession,
  RunningExecution,
  RunningStepLog,
  RunningSpeedAdjustment,
  RunningStepAdvance,
  StrengthLoadProgression,
  StrengthSetLog,
  StrengthWorkoutExercise,
  TrainingSession,
  TrainingSessionCreatePayload,
  TrainingSessionUpdatePayload,
  UserPreference,
  UserPreferenceUpdatePayload,
  UserProfileUpdatePayload,
  StrengthWorkoutExerciseCreatePayload,
  UserGymEquipment,
  WeekDashboard,
  WeeklyStatistics,
  WorkoutTemplate,
  WorkoutTemplateCreatePayload,
  WorkoutTemplateSchedulePayload,
  WorkoutTemplateScheduleResponse,
} from '../types/api'

export const mo2logApi = {
  health: () => apiGet<ApiStatus>('/health'),
  productStatus: () => apiGet<ProductMvpStatus>('/product/mvp-status'),
  releaseNotes: () => apiGet<ProductReleaseNotes>('/product/release-notes'),
  opsStatus: () => apiGet<OpsStatus>('/ops/status'),
  deploymentChecklist: () => apiGet<DeploymentChecklist>('/ops/deployment-checklist'),
  cloudDemoReadiness: () => apiGet<CloudDemoReadiness>('/ops/cloud-demo-readiness'),
  mobileSyncReadiness: () => apiGet<MobileSyncReadiness>('/mobile-sync/readiness'),
  androidSyncPlan: () => apiGet<AndroidSyncPlan>('/mobile-sync/android-plan'),
  register: (payload: RegisterPayload) => apiPost<AuthToken>('/auth/register', payload),
  login: (payload: LoginPayload) => apiPost<AuthToken>('/auth/login', payload),
  demoLogin: () => apiPost<AuthToken>('/auth/demo-login'),
  me: () => apiGet<AuthenticatedUser>('/auth/me'),
  profile: (userId = getCurrentUserId()) => apiGet<AuthenticatedUser>(`/profile/${userId}`),
  updateProfile: (userId: number, payload: UserProfileUpdatePayload) => apiPatch<AuthenticatedUser['user']>(`/profile/${userId}`, payload),
  preferences: (userId = getCurrentUserId()) => apiGet<UserPreference>(`/profile/${userId}/preferences`),
  updatePreferences: (userId: number, payload: UserPreferenceUpdatePayload) => apiPatch<UserPreference>(`/profile/${userId}/preferences`, payload),
  weekDashboard: (userId = getCurrentUserId(), referenceDate = '2026-06-29') =>
    apiGet<WeekDashboard>(`/dashboard/week?user_id=${userId}&reference_date=${referenceDate}`),
  weekStatistics: (userId = getCurrentUserId(), referenceDate = '2026-06-29') =>
    apiGet<WeeklyStatistics>(`/statistics/week?user_id=${userId}&reference_date=${referenceDate}`),
  weeklyIntelligence: (userId = getCurrentUserId(), referenceDate = '2026-06-29') =>
    apiGet<WeeklyIntelligence>(`/intelligence/weekly-insights?user_id=${userId}&reference_date=${referenceDate}`),
  plannedVsDone: (userId = getCurrentUserId(), referenceDate = '2026-06-29') =>
    apiGet<PlannedVsDone>(`/intelligence/planned-vs-done?user_id=${userId}&reference_date=${referenceDate}`),
  forecast5k: (userId = getCurrentUserId()) =>
    apiGet<FiveKForecast>(`/intelligence/forecast-5k?user_id=${userId}`),
  forecastRace: (userId = getCurrentUserId(), targetDistanceKm = 5) =>
    apiGet<RaceForecast>(`/intelligence/forecast-race?user_id=${userId}&target_distance_km=${targetDistanceKm}`),
  reportOverview: (userId = getCurrentUserId(), dateFrom?: string, dateTo?: string) => {
    const params = new URLSearchParams({ user_id: String(userId) })
    if (dateFrom) params.set('date_from', dateFrom)
    if (dateTo) params.set('date_to', dateTo)
    return apiGet<ReportOverview>(`/reports/overview?${params.toString()}`)
  },
  historySummary: (userId = getCurrentUserId(), dateFrom?: string, dateTo?: string) => {
    const params = new URLSearchParams({ user_id: String(userId) })
    if (dateFrom) params.set('date_from', dateFrom)
    if (dateTo) params.set('date_to', dateTo)
    return apiGet<HistorySummary>(`/history/summary?${params.toString()}`)
  },
  sessionHistory: (userId = getCurrentUserId(), filters?: { dateFrom?: string; dateTo?: string; sessionType?: string; status?: string; limit?: number }) => {
    const params = new URLSearchParams({ user_id: String(userId), limit: String(filters?.limit ?? 50) })
    if (filters?.dateFrom) params.set('date_from', filters.dateFrom)
    if (filters?.dateTo) params.set('date_to', filters.dateTo)
    if (filters?.sessionType) params.set('session_type', filters.sessionType)
    if (filters?.status) params.set('status', filters.status)
    return apiGet<TrainingSession[]>(`/history/sessions?${params.toString()}`)
  },
  goals: (userId = getCurrentUserId()) => apiGet<Goal[]>(`/goals?user_id=${userId}`),
  personalRecords: (userId = getCurrentUserId()) => apiGet<PersonalRecord[]>(`/personal-records?user_id=${userId}`),
  runningActivities: (userId = getCurrentUserId()) => apiGet<RunningActivity[]>(`/running-activities?user_id=${userId}`),
  runningActivity: (activityId: number) => apiGet<RunningActivity>(`/running-activities/${activityId}`),
  createManualRun: (payload: ManualRunningActivityPayload) => apiPost<RunningActivity>('/running-activities', payload),
  currentRunningGoal: (userId = getCurrentUserId()) => apiGet<RunningGoal>(`/running-goals/current?user_id=${userId}`),
  createRunningGoal: (payload: RunningGoalPayload) => apiPost<RunningGoal>('/running-goals', payload),
  generateRunningPlan: (goalId: number) => apiPost<RunningPlanGenerateResponse>(`/running-goals/${goalId}/generate-plan`),
  runningPlanWeek: (userId = getCurrentUserId(), referenceDate = '2026-06-29') => apiGet<RunningPlanSession[]>(`/running-plan/week?user_id=${userId}&reference_date=${referenceDate}T00:00:00Z`),
  runningPlanSession: (sessionId: number) => apiGet<RunningPlanSession>(`/running-plan/sessions/${sessionId}`),
  startRunningExecution: (sessionId: number) => apiPost<RunningExecution>(`/running-plan/sessions/${sessionId}/start`),
  startRunningStep: (executionId: number, stepId: number) => apiPost<RunningStepLog>(`/running-executions/${executionId}/steps/${stepId}/start`),
  speedUpRunningStep: (stepLogId: number) => apiPost<RunningSpeedAdjustment>(`/running-step-logs/${stepLogId}/speed-up`),
  speedDownRunningStep: (stepLogId: number) => apiPost<RunningSpeedAdjustment>(`/running-step-logs/${stepLogId}/speed-down`),
  completeRunningStep: (stepLogId: number) => apiPost<RunningStepAdvance>(`/running-step-logs/${stepLogId}/complete`),
  finishRunningExecution: (executionId: number) => apiPost<RunningExecution>(`/running-executions/${executionId}/finish`),
  exercises: () => apiGet<Exercise[]>('/exercises'),
  equipment: () => apiGet<Equipment[]>('/equipment'),
  userGymEquipment: (userId = getCurrentUserId()) => apiGet<UserGymEquipment[]>(`/user-gym-equipment?user_id=${userId}`),
  updateEquipmentStatus: (payload: { user_id: number; equipment_id: number; status: string; notes?: string }) =>
    apiPost<UserGymEquipment>('/user-gym-equipment', payload),
  workoutTemplates: (userId = getCurrentUserId()) => apiGet<WorkoutTemplate[]>(`/workout-templates?user_id=${userId}`),
  workoutTemplate: (templateId: number) => apiGet<WorkoutTemplate>(`/workout-templates/${templateId}`),
  createWorkoutTemplate: (payload: WorkoutTemplateCreatePayload) => apiPost<WorkoutTemplate>('/workout-templates', payload),
  archiveWorkoutTemplate: (templateId: number, userId = getCurrentUserId()) => apiDelete(`/workout-templates/${templateId}?user_id=${userId}`),
  scheduleWorkoutTemplate: (templateId: number, payload: WorkoutTemplateSchedulePayload) =>
    apiPost<WorkoutTemplateScheduleResponse>(`/workout-templates/${templateId}/schedule`, payload),
  nextWorkoutReadiness: (userId = getCurrentUserId(), referenceDate = new Date().toISOString().slice(0, 10)) =>
    apiGet<NextWorkoutReadiness>(`/training-sessions/next-ready?user_id=${userId}&reference_date=${referenceDate}`),
  weekTrainingSessions: (userId = getCurrentUserId(), referenceDate = '2026-06-29') =>
    apiGet<TrainingSession[]>(`/training-sessions/week?user_id=${userId}&reference_date=${referenceDate}`),
  createTrainingSession: (payload: TrainingSessionCreatePayload) =>
    apiPost<TrainingSession>('/training-sessions', payload),
  updateTrainingSession: (sessionId: number, payload: TrainingSessionUpdatePayload) =>
    apiPatch<TrainingSession>(`/training-sessions/${sessionId}`, payload),
  deleteTrainingSession: (sessionId: number) => apiDelete(`/training-sessions/${sessionId}`),
  addStrengthExerciseToSession: (sessionId: number, payload: StrengthWorkoutExerciseCreatePayload) =>
    apiPost<StrengthWorkoutExercise>(`/training-sessions/${sessionId}/strength-exercises`, payload),
  trainingSession: (sessionId: number) => apiGet<TrainingSession>(`/training-sessions/${sessionId}`),
  startTrainingSession: (sessionId: number) => apiPost<TrainingSession>(`/training-sessions/${sessionId}/start`),
  finishTrainingSession: (sessionId: number) => apiPost<TrainingSession>(`/training-sessions/${sessionId}/finish`),
  strengthLoadProgression: (exerciseId: number, userId = getCurrentUserId()) => apiGet<StrengthLoadProgression>(`/strength/exercises/${exerciseId}/load-progression?user_id=${userId}`),
  createSetLog: (payload: {
    strength_workout_exercise_id: number
    set_number: number
    reps: number
    load: number
    rir?: number | null
    rpe?: number | null
  }) => apiPost<StrengthSetLog>('/strength/set-logs', payload),
  exerciseAlternatives: (exerciseId: number, userId = getCurrentUserId(), mode: 'default' | 'all' = 'default') =>
    apiGet<ExerciseAlternative[]>(`/exercises/${exerciseId}/alternatives?user_id=${userId}&mode=${mode}`),
  adaptationSuggestions: (
    exerciseId: number,
    userId = getCurrentUserId(),
    mode: 'default' | 'all' = 'default',
    reason: 'equipment_busy' | 'equipment_unavailable' | 'pain_discomfort' | 'preference' | 'manual_adjustment' = 'equipment_busy',
  ) =>
    apiGet<AdaptationSuggestion[]>(
      `/adaptation/exercises/${exerciseId}/suggestions?user_id=${userId}&mode=${mode}&reason=${reason}`,
    ),
  swapExercise: (
    sessionId: number,
    payload: {
      strength_workout_exercise_id: number
      original_exercise_id: number
      new_exercise_id: number
      reason: 'equipment_busy' | 'equipment_unavailable' | 'pain_discomfort' | 'preference' | 'manual_adjustment'
    },
  ) => apiPost<ExerciseSwapResult>(`/training-sessions/${sessionId}/swap-exercise`, payload),
  updateGoalProgress: (goalId: number, currentValue: number) =>
    apiPatch<Goal>(`/goals/${goalId}/progress`, { current_value: currentValue }),
}

import { apiDelete, apiGet, apiPatch, apiPost } from './client'
import type {
  AdaptationSuggestion,
  ApiStatus,
  Equipment,
  Exercise,
  ExerciseAlternative,
  ExerciseSwapResult,
  Goal,
  PersonalRecord,
  ManualRunningActivityPayload,
  RunningActivity,
  StravaStatus,
  StravaSyncResult,
  StravaAuthorizeResponse,
  StrengthSetLog,
  StrengthWorkoutExercise,
  TrainingSession,
  TrainingSessionCreatePayload,
  TrainingSessionUpdatePayload,
  StrengthWorkoutExerciseCreatePayload,
  UserGymEquipment,
  WeekDashboard,
  WeeklyStatistics,
} from '../types/api'

export const mo2logApi = {
  health: () => apiGet<ApiStatus>('/health'),
  weekDashboard: (userId = 1, referenceDate = '2026-06-29') =>
    apiGet<WeekDashboard>(`/dashboard/week?user_id=${userId}&reference_date=${referenceDate}`),
  weekStatistics: (userId = 1, referenceDate = '2026-06-29') =>
    apiGet<WeeklyStatistics>(`/statistics/week?user_id=${userId}&reference_date=${referenceDate}`),
  goals: (userId = 1) => apiGet<Goal[]>(`/goals?user_id=${userId}`),
  personalRecords: (userId = 1) => apiGet<PersonalRecord[]>(`/personal-records?user_id=${userId}`),
  stravaStatus: (userId = 1) => apiGet<StravaStatus>(`/strava/status?user_id=${userId}`),
  stravaAuthorize: (userId = 1) => apiGet<StravaAuthorizeResponse>(`/auth/strava/authorize?user_id=${userId}`),
  stravaSync: (userId = 1) => apiPost<StravaSyncResult>(`/strava/sync?user_id=${userId}`),
  runningActivities: (userId = 1) => apiGet<RunningActivity[]>(`/running-activities?user_id=${userId}`),
  runningActivity: (activityId: number) => apiGet<RunningActivity>(`/running-activities/${activityId}`),
  createManualRun: (payload: ManualRunningActivityPayload) => apiPost<RunningActivity>('/running-activities', payload),
  exercises: () => apiGet<Exercise[]>('/exercises'),
  equipment: () => apiGet<Equipment[]>('/equipment'),
  userGymEquipment: (userId = 1) => apiGet<UserGymEquipment[]>(`/user-gym-equipment?user_id=${userId}`),
  updateEquipmentStatus: (payload: { user_id: number; equipment_id: number; status: string; notes?: string }) =>
    apiPost<UserGymEquipment>('/user-gym-equipment', payload),
  weekTrainingSessions: (userId = 1, referenceDate = '2026-06-29') =>
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
  createSetLog: (payload: {
    strength_workout_exercise_id: number
    set_number: number
    reps: number
    load: number
    rir?: number | null
    rpe?: number | null
  }) => apiPost<StrengthSetLog>('/strength/set-logs', payload),
  exerciseAlternatives: (exerciseId: number, userId = 1, mode: 'default' | 'all' = 'default') =>
    apiGet<ExerciseAlternative[]>(`/exercises/${exerciseId}/alternatives?user_id=${userId}&mode=${mode}`),
  adaptationSuggestions: (
    exerciseId: number,
    userId = 1,
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

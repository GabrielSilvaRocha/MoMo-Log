import { apiGet } from './client'
import type { ApiStatus, Goal, PersonalRecord, StravaStatus, WeekDashboard, WeeklyStatistics } from '../types/api'

export const mo2logApi = {
  health: () => apiGet<ApiStatus>('/health'),
  weekDashboard: (userId = 1, referenceDate = '2026-06-29') =>
    apiGet<WeekDashboard>(`/dashboard/week?user_id=${userId}&reference_date=${referenceDate}`),
  weekStatistics: (userId = 1, referenceDate = '2026-06-29') =>
    apiGet<WeeklyStatistics>(`/statistics/week?user_id=${userId}&reference_date=${referenceDate}`),
  goals: (userId = 1) => apiGet<Goal[]>(`/goals?user_id=${userId}`),
  personalRecords: (userId = 1) => apiGet<PersonalRecord[]>(`/personal-records?user_id=${userId}`),
  stravaStatus: (userId = 1) => apiGet<StravaStatus>(`/strava/status?user_id=${userId}`),
}

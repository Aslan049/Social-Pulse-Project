import { api } from './client'
import type { Metric, MetricSummary, MonthlyChange, Platform, PlatformComparison } from './types'

export const fetchSummary = (platform?: Platform) =>
  api
    .get<MetricSummary[]>('/api/analytics/summary', { params: platform ? { platform } : {} })
    .then((r) => r.data)

export const fetchPlatformComparison = () =>
  api.get<PlatformComparison>('/api/analytics/platforms').then((r) => r.data)

export const fetchMonthlyChange = (platform: Platform, metric: Metric) =>
  api.get<MonthlyChange>('/api/analytics/monthly-change', { params: { platform, metric } }).then((r) => r.data)

import { api } from './client'
import type { MetricSummary, Platform } from './types'

export const fetchSummary = (platform?: Platform) =>
  api
    .get<MetricSummary[]>('/api/analytics/summary', { params: platform ? { platform } : {} })
    .then((r) => r.data)

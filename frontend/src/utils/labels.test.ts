import { describe, expect, it } from 'vitest'
import { METRICS, PLATFORMS } from '../api/types'
import { METRIC_LABELS, PLATFORM_LABELS, metricLabel } from './labels'

describe('labels', () => {
  it('has a Turkish label for every metric and platform', () => {
    for (const metric of METRICS) {
      expect(METRIC_LABELS[metric]).toBeTruthy()
    }
    for (const platform of PLATFORMS) {
      expect(PLATFORM_LABELS[platform]).toBeTruthy()
    }
  })

  it('falls back to the key for unknown metrics and to a dash for empty values', () => {
    expect(metricLabel('REACH')).toBe('Erişim')
    expect(metricLabel('SOME_NEW_METRIC')).toBe('SOME_NEW_METRIC')
    expect(metricLabel(null)).toBe('—')
    expect(metricLabel(undefined)).toBe('—')
  })
})

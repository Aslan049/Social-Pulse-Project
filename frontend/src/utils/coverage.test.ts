import { describe, expect, it } from 'vitest'
import type { MetricSummary } from '../api/types'
import { coverageNotes } from './coverage'

const base: MetricSummary = {
  platform: 'INSTAGRAM',
  metric: 'NEW_FOLLOWERS',
  granularity: 'DAY',
  periods: 321,
  total: 9550,
  average: 29.75,
  min: 0,
  max: 120,
  firstPeriod: '2025-10-15',
  lastPeriod: '2026-08-31',
  coverage: 1,
  internalMissing: 0,
  windowStart: '2025-09-01',
  windowEnd: '2026-08-31',
  windowCoverage: 0.8795,
  leadingMissing: 44,
  trailingMissing: 0,
  negativeCount: 0,
}

describe('coverageNotes', () => {
  it('explains a late start as a metric availability note, not as a gap', () => {
    const notes = coverageNotes(base)

    expect(notes).toEqual([
      'Pencerenin ilk 44 gün için veri yok (metrik 15.10.2025 tarihinde başlıyor; eksik veri olmayabilir)',
    ])
  })

  it('reports real gaps inside the own range separately', () => {
    const notes = coverageNotes({ ...base, leadingMissing: 0, internalMissing: 3, coverage: 0.99 })

    expect(notes).toEqual(['Kendi aralığında 3 gün eksik'])
  })

  it('uses months for monthly series', () => {
    const notes = coverageNotes({
      ...base,
      granularity: 'MONTH',
      firstPeriod: '2025-10-01',
      lastPeriod: '2026-08-01',
      leadingMissing: 1,
    })

    expect(notes).toEqual([
      'Pencerenin ilk 1 ay için veri yok (metrik 10.2025 tarihinde başlıyor; eksik veri olmayabilir)',
    ])
  })

  it('mentions trailing gaps and excluded negative values', () => {
    const notes = coverageNotes({ ...base, leadingMissing: 0, trailingMissing: 10, negativeCount: 1 })

    expect(notes).toEqual([
      'Pencerenin son 10 gün için veri yok (metrik 31.08.2026 tarihinde bitiyor)',
      '1 negatif değer hariç tutuldu',
    ])
  })

  it('has no notes for a complete series', () => {
    expect(coverageNotes({ ...base, leadingMissing: 0, windowCoverage: 1 })).toEqual([])
  })
})

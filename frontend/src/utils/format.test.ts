import { describe, expect, it } from 'vitest'
import { formatDate, formatNumber, formatPercent, formatPeriod } from './format'

describe('format helpers', () => {
  it('formats numbers with Turkish separators', () => {
    expect(formatNumber(1234.5)).toBe('1.234,5')
    expect(formatNumber(1219106)).toBe('1.219.106')
    expect(formatNumber(0)).toBe('0')
  })

  it('formats ratios as percentages', () => {
    expect(formatPercent(0.8794)).toBe('%88')
    expect(formatPercent(1)).toBe('%100')
  })

  it('formats ISO dates without timezone shifts', () => {
    expect(formatDate('2025-10-15')).toBe('15.10.2025')
    expect(formatDate('2026-01-01')).toBe('01.01.2026')
  })

  it('formats periods by granularity', () => {
    expect(formatPeriod('2025-10-01', 'MONTH')).toBe('10.2025')
    expect(formatPeriod('2025-10-15', 'DAY')).toBe('15.10.2025')
  })
})

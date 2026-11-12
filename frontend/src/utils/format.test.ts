import { describe, expect, it } from 'vitest'
import { formatChange, formatDate, formatMonth, formatNumber, formatPercent, formatPeriod } from './format'

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

  it('never shows an incomplete ratio as 100 percent', () => {
    expect(formatPercent(0.9973)).toBe('%99,7')
    expect(formatPercent(364 / 365)).toBe('%99,7')
  })

  it('formats ISO dates without timezone shifts', () => {
    expect(formatDate('2025-10-15')).toBe('15.10.2025')
    expect(formatDate('2026-01-01')).toBe('01.01.2026')
  })

  it('formats periods by granularity', () => {
    expect(formatPeriod('2025-10-01', 'MONTH')).toBe('10.2025')
    expect(formatPeriod('2025-10-15', 'DAY')).toBe('15.10.2025')
  })

  it('formats months with Turkish names', () => {
    expect(formatMonth('2025-09-01')).toBe('Eylül 2025')
    expect(formatMonth('2026-08-01')).toBe('Ağustos 2026')
  })

  it('formats percentage changes with sign and one decimal', () => {
    expect(formatChange(127.8)).toBe('+127,8%')
    expect(formatChange(-33.9)).toBe('-33,9%')
    expect(formatChange(0)).toBe('0%')
  })
})

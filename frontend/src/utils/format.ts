import type { Granularity } from '../api/types'

const numberFormat = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 2 })
const percentFormat = new Intl.NumberFormat('tr-TR', { style: 'percent', maximumFractionDigits: 0 })

export const formatNumber = (value: number) => numberFormat.format(value)

const preciseFormat = new Intl.NumberFormat('tr-TR', { style: 'percent', maximumFractionDigits: 1 })

/** %100'e yuvarlanan ama tam olmayan oranlar (örn. 364/365) "%100" yerine "%99,7" gösterilir. */
export function formatPercent(ratio: number): string {
  if (ratio < 1 && Math.round(ratio * 100) === 100) {
    return preciseFormat.format(ratio)
  }
  return percentFormat.format(ratio)
}

/** "2025-10-15" → "15.10.2025" (saat dilimi kayması olmaması için metin olarak ayrıştırılır). */
export function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-')
  return `${day}.${month}.${year}`
}

/** Aylık seride "10.2025", günlük seride "15.10.2025". */
export function formatPeriod(iso: string, granularity: Granularity): string {
  const [year, month, day] = iso.split('-')
  return granularity === 'MONTH' ? `${month}.${year}` : `${day}.${month}.${year}`
}

export const formatDateTime = (iso: string) => new Date(iso).toLocaleString('tr-TR')

/** "2025-09-01" → "Eylül 2025" */
export function formatMonth(iso: string): string {
  const [year, month] = iso.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, 1)).toLocaleDateString('tr-TR', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })
}

const changeFormat = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 1 })

/** 127.8 → "+127,8%", -33.9 → "-33,9%" */
export function formatChange(percent: number): string {
  return `${percent > 0 ? '+' : ''}${changeFormat.format(percent)}%`
}

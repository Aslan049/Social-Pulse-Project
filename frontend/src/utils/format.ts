import type { Granularity } from '../api/types'

const numberFormat = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 2 })
const percentFormat = new Intl.NumberFormat('tr-TR', { style: 'percent', maximumFractionDigits: 0 })

export const formatNumber = (value: number) => numberFormat.format(value)

export const formatPercent = (ratio: number) => percentFormat.format(ratio)

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

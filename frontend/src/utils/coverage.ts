import type { MetricSummary } from '../api/types'
import { formatPeriod } from './format'

/**
 * Kapsama göstergelerinin yanındaki açıklamalar. Pencerenin başında/sonunda eksik dönemler, metriğin o tarihte
 * başlamasından/bitmesinden kaynaklanabilir ve eksik veri sayılmaz; kendi aralığı içindeki boşluklar ise gerçek
 * eksik veridir. Bu ikisi ayrı cümlelerle belirtilir.
 */
export function coverageNotes(row: MetricSummary): string[] {
  const unit = row.granularity === 'DAY' ? 'gün' : 'ay'
  const notes: string[] = []
  if (row.leadingMissing > 0) {
    notes.push(
      `Pencerenin ilk ${row.leadingMissing} ${unit} için veri yok (metrik ${formatPeriod(row.firstPeriod, row.granularity)} tarihinde başlıyor; eksik veri olmayabilir)`,
    )
  }
  if (row.trailingMissing > 0) {
    notes.push(
      `Pencerenin son ${row.trailingMissing} ${unit} için veri yok (metrik ${formatPeriod(row.lastPeriod, row.granularity)} tarihinde bitiyor)`,
    )
  }
  if (row.internalMissing > 0) {
    notes.push(`Kendi aralığında ${row.internalMissing} ${unit} eksik`)
  }
  if (row.negativeCount > 0) {
    notes.push(`${row.negativeCount} negatif değer hariç tutuldu`)
  }
  return notes
}

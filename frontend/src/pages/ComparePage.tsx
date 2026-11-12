import { useEffect, useState } from 'react'
import { fetchMonthlyChange, fetchPlatformComparison, fetchSummary } from '../api/analytics'
import { getErrorMessage } from '../api/errors'
import type { Metric, MetricSummary, MonthCell, MonthlyChange, Platform, PlatformComparison } from '../api/types'
import { formatChange, formatDate, formatMonth, formatNumber, formatPercent } from '../utils/format'
import { GRANULARITY_LABELS, metricLabel, PLATFORM_LABELS } from '../utils/labels'

function pairKey(platform: Platform, metric: Metric) {
  return `${platform}|${metric}`
}

function uniquePairs(summary: MetricSummary[]) {
  const seen = new Set<string>()
  const pairs: { platform: Platform; metric: Metric }[] = []
  for (const row of summary) {
    const key = pairKey(row.platform, row.metric)
    if (!seen.has(key)) {
      seen.add(key)
      pairs.push({ platform: row.platform, metric: row.metric })
    }
  }
  return pairs
}

function ValueCell({ cell }: { cell: MonthCell | undefined }) {
  if (!cell || cell.status === 'MISSING' || cell.value === null) {
    return <td className="missing-cell">Veri yok</td>
  }
  if (cell.status === 'PARTIAL') {
    return (
      <td className="partial-cell">
        {formatNumber(cell.value)}{' '}
        <small>(kısmi{cell.coverage !== null ? `, ${formatPercent(cell.coverage)}` : ''})</small>
      </td>
    )
  }
  return <td>{formatNumber(cell.value)}</td>
}

export default function ComparePage() {
  const [comparison, setComparison] = useState<PlatformComparison | null>(null)
  const [summary, setSummary] = useState<MetricSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [metric, setMetric] = useState<Metric | ''>('')
  const [pair, setPair] = useState('')
  const [change, setChange] = useState<MonthlyChange | null>(null)
  const [changeError, setChangeError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchPlatformComparison(), fetchSummary()])
      .then(([comparisonData, summaryData]) => {
        if (cancelled) {
          return
        }
        setComparison(comparisonData)
        setSummary(summaryData)
        if (comparisonData.metrics.length > 0) {
          setMetric(comparisonData.metrics[0].metric)
        }
        const pairs = uniquePairs(summaryData)
        if (pairs.length > 0) {
          setPair(pairKey(pairs[0].platform, pairs[0].metric))
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!pair) {
      return
    }
    const [platform, selectedMetric] = pair.split('|') as [Platform, Metric]
    let cancelled = false
    fetchMonthlyChange(platform, selectedMetric)
      .then((data) => {
        if (!cancelled) {
          setChange(data)
          setChangeError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setChangeError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [pair])

  const selected = comparison?.metrics.find((m) => m.metric === metric)

  return (
    <div className="page">
      <h2>Karşılaştırma</h2>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {comparison === null && !error && <p className="muted">Yükleniyor…</p>}

      {comparison !== null && comparison.metrics.length === 0 && (
        <p className="empty">
          Platform karşılaştırması için en az iki platformdan, ortak bir metrikte veri gerekir.
        </p>
      )}

      {comparison !== null && comparison.metrics.length > 0 && comparison.windowStart && comparison.windowEnd && (
        <section className="card-wide" aria-label="Platform karşılaştırması">
          <h3>Platform karşılaştırması (aylık)</h3>
          <p className="muted">
            Analiz penceresi: <strong>{formatDate(comparison.windowStart)}</strong> –{' '}
            <strong>{formatDate(comparison.windowEnd)}</strong> (platformların aralığının birleşimi). Bir platformda
            veri olmayan aylar 0 sayılmaz, &quot;Veri yok&quot; olarak gösterilir.
          </p>

          <div className="toolbar">
            <label htmlFor="compare-metric">Metrik</label>
            <select
              id="compare-metric"
              value={metric}
              onChange={(event) => setMetric(event.target.value as Metric)}
            >
              {comparison.metrics.map((m) => (
                <option key={m.metric} value={m.metric}>
                  {metricLabel(m.metric)}
                </option>
              ))}
            </select>
          </div>

          {selected && (
            <>
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr>
                      <th>Ay</th>
                      {selected.platforms.map((p) => (
                        <th key={p.platform}>{PLATFORM_LABELS[p.platform]}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {comparison.months.map((month, index) => (
                      <tr key={month}>
                        <td>{formatMonth(month)}</td>
                        {selected.platforms.map((p) => (
                          <ValueCell key={p.platform} cell={p.cells[index]} />
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <ul className="notes">
                {selected.platforms.map((p) => (
                  <li key={`source-${p.platform}`}>
                    {PLATFORM_LABELS[p.platform]}:{' '}
                    {p.sourceGranularity === 'DAY' ? 'günlük veriden aya toplandı' : 'platformun aylık değeri'}
                  </li>
                ))}
                {selected.platforms
                  .filter((p) => p.missingMonths.length > 0)
                  .map((p) => (
                    <li key={`missing-${p.platform}`} className="warn">
                      {PLATFORM_LABELS[p.platform]}: {p.missingMonths.map(formatMonth).join(', ')} için veri yok
                    </li>
                  ))}
                {selected.platforms
                  .filter((p) => p.partialMonths.length > 0)
                  .map((p) => (
                    <li key={`partial-${p.platform}`} className="info-text">
                      {PLATFORM_LABELS[p.platform]}: {p.partialMonths.map(formatMonth).join(', ')} için kısmi veri
                    </li>
                  ))}
              </ul>
            </>
          )}
        </section>
      )}

      {summary !== null && summary.length > 0 && (
        <section className="card-wide" aria-label="Aylık değişim">
          <h3>Aylık değişim</h3>
          <p className="muted">
            Önceki aya göre yüzde değişim. Önceki ay yoksa, 0 ise ya da kapsamı eksikse yüzde ya gösterilmez ya da
            uyarıyla gösterilir.
          </p>
          <div className="toolbar">
            <label htmlFor="change-pair">Seri</label>
            <select id="change-pair" value={pair} onChange={(event) => setPair(event.target.value)}>
              {uniquePairs(summary).map((p) => (
                <option key={pairKey(p.platform, p.metric)} value={pairKey(p.platform, p.metric)}>
                  {PLATFORM_LABELS[p.platform]} – {metricLabel(p.metric)}
                </option>
              ))}
            </select>
          </div>

          {changeError && (
            <p className="error" role="alert">
              {changeError}
            </p>
          )}
          {change && change.rows.length > 0 && (
            <>
              {change.sourceGranularity && (
                <p className="muted">Kaynak veri: {GRANULARITY_LABELS[change.sourceGranularity]}</p>
              )}
              <div className="table-wrap">
                <table className="data">
                  <thead>
                    <tr>
                      <th>Ay</th>
                      <th>Değer</th>
                      <th>Önceki ay</th>
                      <th>Değişim</th>
                      <th>Not</th>
                    </tr>
                  </thead>
                  <tbody>
                    {change.rows.map((row) => (
                      <tr key={row.month}>
                        <td>{formatMonth(row.month)}</td>
                        <ValueCell cell={{ month: row.month, value: row.value, coverage: row.coverage, status: row.status }} />
                        <td>{row.previousValue === null ? '—' : formatNumber(row.previousValue)}</td>
                        <td className={row.changePercent !== null && !row.comparable ? 'info-text' : undefined}>
                          {row.changePercent === null ? '—' : formatChange(row.changePercent)}
                        </td>
                        <td>{row.note ?? ''}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>
      )}
    </div>
  )
}

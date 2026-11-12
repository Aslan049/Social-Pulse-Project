import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchSummary } from '../api/analytics'
import { getErrorMessage } from '../api/errors'
import { PLATFORMS } from '../api/types'
import type { MetricSummary, Platform } from '../api/types'
import { coverageNotes } from '../utils/coverage'
import { formatDate, formatNumber, formatPercent, formatPeriod } from '../utils/format'
import { GRANULARITY_LABELS, METRIC_LABELS, PLATFORM_LABELS } from '../utils/labels'

export default function DashboardPage() {
  const [platform, setPlatform] = useState<Platform | ''>('')
  const [rows, setRows] = useState<MetricSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchSummary(platform === '' ? undefined : platform)
      .then((data) => {
        if (!cancelled) {
          setRows(data)
          setError(null)
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
  }, [platform])

  return (
    <div className="page">
      <h2>Panel</h2>
      <p className="muted">
        Yüklediğiniz verilerin özeti. Kapsama iki ayrı ölçüyle verilir: <strong>aralıkta</strong> (metriğin kendi ilk ve
        son dönemi arasında boşluk var mı) ve <strong>pencerede</strong> (tüm platformların aralığının birleşimine
        göre). Pencerenin başında eksik dönemler, metriğin o tarihte başlamasından kaynaklanabilir ve eksik veri
        sayılmaz. Negatif değerler veri hatası sayılır ve istatistiklere dahil edilmez.
      </p>

      <div className="toolbar">
        <label htmlFor="platform-filter">Platform</label>
        <select
          id="platform-filter"
          value={platform}
          onChange={(event) => setPlatform(event.target.value as Platform | '')}
        >
          <option value="">Tümü</option>
          {PLATFORMS.map((p) => (
            <option key={p} value={p}>
              {PLATFORM_LABELS[p]}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {rows === null && !error && <p className="muted">Yükleniyor…</p>}
      {rows !== null && rows.length === 0 && (
        <p className="empty">
          Henüz veri yok. Başlamak için <Link to="/upload">bir dosya yükleyin</Link>.
        </p>
      )}
      {rows !== null && rows.length > 0 && (
        <>
          <p className="window-note">
            Analiz penceresi: <strong>{formatDate(rows[0].windowStart)}</strong> –{' '}
            <strong>{formatDate(rows[0].windowEnd)}</strong>
          </p>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Platform</th>
                  <th>Metrik</th>
                  <th>Ayrıntı</th>
                  <th>Dönem sayısı</th>
                  <th>Toplam</th>
                  <th>Ortalama</th>
                  <th>En düşük</th>
                  <th>En yüksek</th>
                  <th>Aralık</th>
                  <th>Kapsama</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const notes = coverageNotes(row)
                  return (
                    <tr key={`${row.platform}-${row.metric}-${row.granularity}`}>
                      <td>{PLATFORM_LABELS[row.platform]}</td>
                      <td>{METRIC_LABELS[row.metric] ?? row.metric}</td>
                      <td>{GRANULARITY_LABELS[row.granularity]}</td>
                      <td>{formatNumber(row.periods)}</td>
                      <td>{formatNumber(row.total)}</td>
                      <td>{formatNumber(row.average)}</td>
                      <td>{formatNumber(row.min)}</td>
                      <td>{formatNumber(row.max)}</td>
                      <td>
                        {formatPeriod(row.firstPeriod, row.granularity)} –{' '}
                        {formatPeriod(row.lastPeriod, row.granularity)}
                      </td>
                      <td>
                        <div className={row.coverage < 1 ? 'warn' : undefined}>
                          Aralıkta: {formatPercent(row.coverage)}
                        </div>
                        <div className={row.windowCoverage < 1 ? 'info-text' : undefined}>
                          Pencerede: {formatPercent(row.windowCoverage)}
                        </div>
                        {notes.length > 0 && (
                          <ul className="notes">
                            {notes.map((note) => (
                              <li key={note}>{note}</li>
                            ))}
                          </ul>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

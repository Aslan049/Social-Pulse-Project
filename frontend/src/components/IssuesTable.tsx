import type { IssueSeverity, UploadIssue } from '../api/types'
import { metricLabel } from '../utils/labels'

const SEVERITY_LABELS: Record<IssueSeverity, string> = { ERROR: 'Hata', WARNING: 'Uyarı', INFO: 'Bilgi' }
const SEVERITY_CLASSES: Record<IssueSeverity, string> = {
  ERROR: 'badge-error',
  WARNING: 'badge-warning',
  INFO: 'badge-info',
}

export function IssuesTable({ issues }: { issues: UploadIssue[] }) {
  if (issues.length === 0) {
    return <p className="muted">Hata veya uyarı yok.</p>
  }
  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            <th>Tür</th>
            <th>Sayfa</th>
            <th>Satır</th>
            <th>Metrik</th>
            <th>Açıklama</th>
          </tr>
        </thead>
        <tbody>
          {issues.map((issue, index) => (
            <tr key={index}>
              <td>
                <span className={`badge ${SEVERITY_CLASSES[issue.severity]}`}>{SEVERITY_LABELS[issue.severity]}</span>
              </td>
              <td>{issue.sheet ?? '—'}</td>
              <td>{issue.row > 0 ? issue.row : '—'}</td>
              <td>{issue.field ? metricLabel(issue.field) : '—'}</td>
              <td>{issue.message}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

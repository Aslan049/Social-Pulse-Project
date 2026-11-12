import type { UploadIssue } from '../api/types'
import { metricLabel } from '../utils/labels'

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
                <span className={`badge ${issue.severity === 'ERROR' ? 'badge-error' : 'badge-warning'}`}>
                  {issue.severity === 'ERROR' ? 'Hata' : 'Uyarı'}
                </span>
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

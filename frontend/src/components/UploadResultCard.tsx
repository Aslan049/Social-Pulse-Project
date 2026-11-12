import type { UploadResult } from '../api/types'
import { formatDateTime, formatNumber } from '../utils/format'
import { PLATFORM_LABELS, STATUS_LABELS } from '../utils/labels'
import { IssuesTable } from './IssuesTable'

export function UploadResultCard({ result, title }: { result: UploadResult; title: string }) {
  return (
    <section className="card-wide" aria-label={title}>
      <h3>{title}</h3>
      <p>
        <strong>{result.filename}</strong> · {PLATFORM_LABELS[result.platform]} · {STATUS_LABELS[result.status]} ·{' '}
        {formatDateTime(result.uploadedAt)}
      </p>

      <dl className="stats">
        <div>
          <dt>Toplam ölçüm</dt>
          <dd>{formatNumber(result.totalRows)}</dd>
        </div>
        <div>
          <dt>Aktarılan</dt>
          <dd>{formatNumber(result.importedRows)}</dd>
        </div>
        <div>
          <dt>Reddedilen</dt>
          <dd>{formatNumber(result.rejectedRows)}</dd>
        </div>
        <div>
          <dt>Uyarı</dt>
          <dd>{formatNumber(result.warningCount)}</dd>
        </div>
      </dl>

      {result.sheets.length > 0 && (
        <p className="muted">
          Okunan sayfalar:{' '}
          {result.sheets.map((sheet) => `${sheet.name} (${formatNumber(sheet.rowCount)} satır)`).join(', ')}
        </p>
      )}
      {result.ignoredSheets.length > 0 && (
        <p className="muted">Okunmayan sayfalar: {result.ignoredSheets.join(', ')}</p>
      )}
      {result.ignoredColumns.length > 0 && (
        <p className="muted">Tanınmayan sütunlar: {result.ignoredColumns.join(', ')}</p>
      )}

      <h4>Hata ve uyarılar</h4>
      <IssuesTable issues={result.issues} />
    </section>
  )
}

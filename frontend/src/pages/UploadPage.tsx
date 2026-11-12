import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { deleteUpload, getUpload, listUploads, uploadFile } from '../api/uploads'
import { getErrorMessage } from '../api/errors'
import { PLATFORMS } from '../api/types'
import type { Platform, UploadResult } from '../api/types'
import { DropZone } from '../components/DropZone'
import { UploadResultCard } from '../components/UploadResultCard'
import { formatDateTime, formatNumber } from '../utils/format'
import { validateFile } from '../utils/files'
import { PLATFORM_LABELS, STATUS_LABELS } from '../utils/labels'

export default function UploadPage() {
  const [platform, setPlatform] = useState<Platform>('INSTAGRAM')
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<UploadResult | null>(null)

  const [history, setHistory] = useState<UploadResult[] | null>(null)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [detail, setDetail] = useState<UploadResult | null>(null)

  const refreshHistory = useCallback(async () => {
    try {
      setHistory(await listUploads())
      setHistoryError(null)
    } catch (err) {
      setHistoryError(getErrorMessage(err))
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    listUploads()
      .then((data) => {
        if (!cancelled) {
          setHistory(data)
          setHistoryError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setHistoryError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  function handleFileSelected(selected: File) {
    setResult(null)
    setError(null)
    const problem = validateFile(selected)
    if (problem) {
      setFile(null)
      setFileError(problem)
      return
    }
    setFileError(null)
    setFile(selected)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) {
      return
    }
    setUploading(true)
    setProgress(0)
    setError(null)
    try {
      const uploaded = await uploadFile(platform, file, setProgress)
      setResult(uploaded)
      setFile(null)
      void refreshHistory()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setUploading(false)
    }
  }

  async function handleDelete(item: UploadResult) {
    if (!window.confirm(`"${item.filename}" yüklemesi ve ona bağlı ölçümler silinsin mi?`)) {
      return
    }
    try {
      await deleteUpload(item.id)
      if (detail?.id === item.id) {
        setDetail(null)
      }
      await refreshHistory()
    } catch (err) {
      setHistoryError(getErrorMessage(err))
    }
  }

  async function handleDetail(item: UploadResult) {
    try {
      setDetail(await getUpload(item.id))
    } catch (err) {
      setHistoryError(getErrorMessage(err))
    }
  }

  return (
    <div className="page">
      <h2>Veri yükle</h2>
      <p className="muted">
        Platformdan dışa aktardığınız CSV veya Excel dosyasını yükleyin. Dosyanın kendisi saklanmaz, yalnızca
        ayrıştırılmış ölçümler tutulur.
      </p>

      <form className="card-wide" onSubmit={handleSubmit}>
        <label htmlFor="platform">Platform</label>
        <select
          id="platform"
          value={platform}
          disabled={uploading}
          onChange={(event) => setPlatform(event.target.value as Platform)}
        >
          {PLATFORMS.map((p) => (
            <option key={p} value={p}>
              {PLATFORM_LABELS[p]}
            </option>
          ))}
        </select>

        <DropZone file={file} disabled={uploading} onFileSelected={handleFileSelected} />

        {fileError && (
          <p className="error" role="alert">
            {fileError}
          </p>
        )}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {uploading && (
          <div className="progress" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>
        )}

        <button type="submit" disabled={!file || uploading}>
          {uploading ? `Yükleniyor… %${progress}` : 'Yükle'}
        </button>
      </form>

      {result && <UploadResultCard result={result} title="Yükleme sonucu" />}

      <h3>Yüklemelerim</h3>
      {historyError && (
        <p className="error" role="alert">
          {historyError}
        </p>
      )}
      {history === null && !historyError && <p className="muted">Yükleniyor…</p>}
      {history !== null && history.length === 0 && <p className="muted">Henüz yükleme yok.</p>}
      {history !== null && history.length > 0 && (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Dosya</th>
                <th>Platform</th>
                <th>Durum</th>
                <th>Aktarılan</th>
                <th>Reddedilen</th>
                <th>Uyarı</th>
                <th>Tarih</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{item.filename}</td>
                  <td>{PLATFORM_LABELS[item.platform]}</td>
                  <td>{STATUS_LABELS[item.status]}</td>
                  <td>{formatNumber(item.importedRows)}</td>
                  <td>{formatNumber(item.rejectedRows)}</td>
                  <td>{formatNumber(item.warningCount)}</td>
                  <td>{formatDateTime(item.uploadedAt)}</td>
                  <td className="actions">
                    <button type="button" className="secondary" onClick={() => void handleDetail(item)}>
                      Detay
                    </button>
                    <button type="button" className="secondary danger" onClick={() => void handleDelete(item)}>
                      Sil
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {detail && <UploadResultCard result={detail} title="Yükleme ayrıntısı" />}
    </div>
  )
}

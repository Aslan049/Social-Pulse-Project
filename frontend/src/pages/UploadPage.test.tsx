import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import type { AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { UploadResult } from '../api/types'
import UploadPage from './UploadPage'

vi.mock('../api/uploads', () => ({
  uploadFile: vi.fn(),
  listUploads: vi.fn(),
  getUpload: vi.fn(),
  deleteUpload: vi.fn(),
}))

import * as uploads from '../api/uploads'

const api = vi.mocked(uploads)

function makeResult(overrides: Partial<UploadResult> = {}): UploadResult {
  return {
    id: 1,
    platform: 'INSTAGRAM',
    filename: 'instagram_gunluk.xlsx',
    status: 'COMPLETED',
    totalRows: 2146,
    importedRows: 2146,
    rejectedRows: 0,
    warningCount: 1,
    uploadedAt: '2026-11-01T10:00:00Z',
    sheets: [{ name: 'Ziyaretler', rowCount: 366, columnCount: 2, header: ['Tarih', 'Değer'] }],
    ignoredSheets: ['İstatistikler', 'Tüm Veriler'],
    ignoredColumns: [],
    issues: [
      {
        severity: 'WARNING',
        sheet: null,
        row: 0,
        field: 'NEW_FOLLOWERS',
        message: '44 gün için veri yok (321/365 gün).',
      },
    ],
    ...overrides,
  }
}

function xlsxFile(name = 'rapor.xlsx') {
  return new File(['dummy'], name, {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
}

beforeEach(() => {
  api.listUploads.mockResolvedValue([])
})

afterEach(() => {
  vi.clearAllMocks()
  vi.restoreAllMocks()
})

describe('UploadPage', () => {
  it('shows an empty state when there are no uploads yet', async () => {
    render(<UploadPage />)

    expect(await screen.findByText('Henüz yükleme yok.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Yükle' })).toBeDisabled()
  })

  it('rejects unsupported files before anything is sent', async () => {
    const user = userEvent.setup({ applyAccept: false })
    render(<UploadPage />)
    await screen.findByText('Henüz yükleme yok.')

    await user.upload(screen.getByLabelText('Dosya seç'), new File(['x'], 'notlar.txt'))

    expect(await screen.findByRole('alert')).toHaveTextContent('Yalnızca .csv ve .xlsx dosyaları desteklenir')
    expect(screen.getByRole('button', { name: 'Yükle' })).toBeDisabled()
    expect(api.uploadFile).not.toHaveBeenCalled()
  })

  it('uploads the selected file with the chosen platform and shows the summary', async () => {
    const user = userEvent.setup()
    api.uploadFile.mockResolvedValue(makeResult())
    render(<UploadPage />)
    await screen.findByText('Henüz yükleme yok.')

    const file = xlsxFile('instagram_gunluk.xlsx')
    await user.upload(screen.getByLabelText('Dosya seç'), file)
    expect(screen.getByText('instagram_gunluk.xlsx')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Yükle' }))

    await waitFor(() => expect(api.uploadFile).toHaveBeenCalledWith('INSTAGRAM', file, expect.any(Function)))
    const card = await screen.findByRole('region', { name: 'Yükleme sonucu' })
    expect(within(card).getByText('Tamamlandı', { exact: false })).toBeInTheDocument()
    expect(within(card).getAllByText('2.146', { selector: 'dd' })).toHaveLength(2) // toplam ve aktarılan
    expect(within(card).getByText('Okunmayan sayfalar: İstatistikler, Tüm Veriler')).toBeInTheDocument()
    expect(within(card).getByText('Yeni takip')).toBeInTheDocument()
    expect(within(card).getByText('44 gün için veri yok (321/365 gün).')).toBeInTheDocument()
    expect(api.listUploads).toHaveBeenCalledTimes(2) // açılışta ve yüklemeden sonra
  })

  it('sends the selected platform', async () => {
    const user = userEvent.setup()
    api.uploadFile.mockResolvedValue(makeResult({ platform: 'X', filename: 'x.xlsx' }))
    render(<UploadPage />)
    await screen.findByText('Henüz yükleme yok.')

    await user.selectOptions(screen.getByLabelText('Platform'), 'X')
    const file = xlsxFile('x.xlsx')
    await user.upload(screen.getByLabelText('Dosya seç'), file)
    await user.click(screen.getByRole('button', { name: 'Yükle' }))

    await waitFor(() => expect(api.uploadFile).toHaveBeenCalledWith('X', file, expect.any(Function)))
  })

  it('shows the server error message when the upload fails', async () => {
    const user = userEvent.setup()
    api.uploadFile.mockRejectedValue(
      new AxiosError('fail', 'ERR_BAD_REQUEST', undefined, undefined, {
        status: 400,
        data: { detail: 'Instagram dosyasında okunabilir bir metrik tablosu bulunamadı.' },
      } as AxiosResponse),
    )
    render(<UploadPage />)
    await screen.findByText('Henüz yükleme yok.')

    await user.upload(screen.getByLabelText('Dosya seç'), xlsxFile())
    await user.click(screen.getByRole('button', { name: 'Yükle' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('okunabilir bir metrik tablosu bulunamadı')
    expect(screen.queryByRole('region', { name: 'Yükleme sonucu' })).not.toBeInTheDocument()
  })

  it('lists previous uploads and deletes one after confirmation', async () => {
    const user = userEvent.setup()
    api.listUploads.mockResolvedValueOnce([makeResult({ id: 7, filename: 'eski.xlsx' })]).mockResolvedValue([])
    api.deleteUpload.mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<UploadPage />)

    expect(await screen.findByText('eski.xlsx')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Sil' }))

    await waitFor(() => expect(api.deleteUpload).toHaveBeenCalledWith(7))
    expect(await screen.findByText('Henüz yükleme yok.')).toBeInTheDocument()
  })

  it('does not delete when the confirmation is cancelled', async () => {
    const user = userEvent.setup()
    api.listUploads.mockResolvedValue([makeResult({ id: 7, filename: 'eski.xlsx' })])
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<UploadPage />)

    await user.click(await screen.findByRole('button', { name: 'Sil' }))

    expect(api.deleteUpload).not.toHaveBeenCalled()
  })

  it('shows the stored issues of an earlier upload', async () => {
    const user = userEvent.setup()
    api.listUploads.mockResolvedValue([makeResult({ id: 7, filename: 'eski.xlsx', issues: [] })])
    api.getUpload.mockResolvedValue(
      makeResult({
        id: 7,
        filename: 'eski.xlsx',
        sheets: [],
        ignoredSheets: [],
        issues: [{ severity: 'ERROR', sheet: 'Ziyaretler', row: 3, field: null, message: 'Geçersiz tarih: abc' }],
      }),
    )
    render(<UploadPage />)

    await user.click(await screen.findByRole('button', { name: 'Detay' }))

    const card = await screen.findByRole('region', { name: 'Yükleme ayrıntısı' })
    expect(within(card).getByText('Hata')).toBeInTheDocument()
    expect(within(card).getByText('Geçersiz tarih: abc')).toBeInTheDocument()
    expect(api.getUpload).toHaveBeenCalledWith(7)
  })
})

import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { MetricSummary } from '../api/types'
import DashboardPage from './DashboardPage'

vi.mock('../api/analytics', () => ({
  fetchSummary: vi.fn(),
}))

import * as analytics from '../api/analytics'

const api = vi.mocked(analytics)

const followers: MetricSummary = {
  platform: 'INSTAGRAM',
  metric: 'NEW_FOLLOWERS',
  granularity: 'DAY',
  periods: 321,
  total: 9550,
  average: 29.75,
  min: 0,
  max: 120,
  firstPeriod: '2025-10-15',
  lastPeriod: '2026-08-31',
  coverage: 1,
  internalMissing: 0,
  windowStart: '2025-09-01',
  windowEnd: '2026-08-31',
  windowCoverage: 0.8795,
  leadingMissing: 44,
  trailingMissing: 0,
  negativeCount: 0,
}

const xViews: MetricSummary = {
  platform: 'X',
  metric: 'VIEWS',
  granularity: 'MONTH',
  periods: 11,
  total: 100000,
  average: 9090.91,
  min: 3255,
  max: 20000,
  firstPeriod: '2025-10-01',
  lastPeriod: '2026-08-01',
  coverage: 1,
  internalMissing: 0,
  windowStart: '2025-09-01',
  windowEnd: '2026-08-31',
  windowCoverage: 0.9167,
  leadingMissing: 1,
  trailingMissing: 0,
  negativeCount: 0,
}

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('DashboardPage', () => {
  it('shows an empty state with a link to the upload page', async () => {
    api.fetchSummary.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText(/Henüz veri yok/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'bir dosya yükleyin' })).toHaveAttribute('href', '/upload')
  })

  it('renders summary rows with Turkish labels and formatted numbers', async () => {
    api.fetchSummary.mockResolvedValue([followers])
    renderPage()

    expect(await screen.findByText('Instagram', { selector: 'td' })).toBeInTheDocument()
    expect(screen.getByText('Yeni takip', { selector: 'td' })).toBeInTheDocument()
    expect(screen.getByText('Günlük')).toBeInTheDocument()
    expect(screen.getByText('9.550')).toBeInTheDocument()
    expect(screen.getByText('29,75')).toBeInTheDocument()
    expect(screen.getByText('15.10.2025 – 31.08.2026')).toBeInTheDocument()
  })

  it('shows the analysis window above the table', async () => {
    api.fetchSummary.mockResolvedValue([followers])
    renderPage()

    expect(await screen.findByText(/Analiz penceresi/)).toHaveTextContent('01.09.2025 – 31.08.2026')
  })

  it('reports own-range and window coverage separately and explains a late start', async () => {
    api.fetchSummary.mockResolvedValue([followers])
    renderPage()

    const ownRange = await screen.findByText('Aralıkta: %100')
    expect(ownRange).not.toHaveClass('warn')
    expect(screen.getByText('Pencerede: %88')).toHaveClass('info-text')
    expect(
      screen.getByText('Pencerenin ilk 44 gün için veri yok (metrik 15.10.2025 tarihinde başlıyor; eksik veri olmayabilir)'),
    ).toBeInTheDocument()
  })

  it('shows that a monthly series lacks a month of the common window', async () => {
    api.fetchSummary.mockResolvedValue([xViews])
    renderPage()

    expect(await screen.findByText('Aralıkta: %100')).toBeInTheDocument()
    expect(screen.getByText('Pencerede: %92')).toHaveClass('info-text')
    expect(
      screen.getByText('Pencerenin ilk 1 ay için veri yok (metrik 10.2025 tarihinde başlıyor; eksik veri olmayabilir)'),
    ).toBeInTheDocument()
  })

  it('marks a real gap inside the own range and mentions excluded negative values', async () => {
    api.fetchSummary.mockResolvedValue([
      { ...followers, metric: 'ENGAGEMENT', coverage: 0.9973, internalMissing: 1, negativeCount: 1, leadingMissing: 0, windowCoverage: 0.9973 },
    ])
    renderPage()

    expect(await screen.findByText('Aralıkta: %99,7')).toHaveClass('warn')
    expect(screen.getByText('Kendi aralığında 1 gün eksik')).toBeInTheDocument()
    expect(screen.getByText('1 negatif değer hariç tutuldu')).toBeInTheDocument()
  })

  it('reloads the summary when a platform filter is chosen', async () => {
    const user = userEvent.setup()
    api.fetchSummary.mockResolvedValue([])
    renderPage()
    await screen.findByText(/Henüz veri yok/)

    await user.selectOptions(screen.getByLabelText('Platform'), 'X')

    await waitFor(() => expect(api.fetchSummary).toHaveBeenLastCalledWith('X'))
    expect(api.fetchSummary).toHaveBeenCalledWith(undefined)
  })

  it('shows an error when the summary cannot be loaded', async () => {
    api.fetchSummary.mockRejectedValue(new Error('boom'))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu')
  })
})

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

const row: MetricSummary = {
  platform: 'INSTAGRAM',
  metric: 'ENGAGEMENT',
  granularity: 'DAY',
  periods: 321,
  total: 100000,
  average: 311.53,
  min: -7,
  max: 25121,
  firstPeriod: '2025-10-15',
  lastPeriod: '2026-08-31',
  coverage: 0.8794,
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

  it('renders the summary rows with Turkish labels and formatted numbers', async () => {
    api.fetchSummary.mockResolvedValue([row])
    renderPage()

    expect(await screen.findByText('Instagram', { selector: 'td' })).toBeInTheDocument()
    expect(screen.getByText('Etkileşim', { selector: 'td' })).toBeInTheDocument()
    expect(screen.getByText('Günlük')).toBeInTheDocument()
    expect(screen.getByText('100.000')).toBeInTheDocument()
    expect(screen.getByText('311,53')).toBeInTheDocument()
    expect(screen.getByText('15.10.2025 – 31.08.2026')).toBeInTheDocument()
    expect(screen.getByText('%88')).toHaveClass('warn')
  })

  it('does not mark complete coverage as a warning', async () => {
    api.fetchSummary.mockResolvedValue([{ ...row, coverage: 1 }])
    renderPage()

    expect(await screen.findByText('%100')).not.toHaveClass('warn')
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

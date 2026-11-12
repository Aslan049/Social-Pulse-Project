import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { MetricSummary, MonthlyChange, PlatformComparison } from '../api/types'
import ComparePage from './ComparePage'

vi.mock('../api/analytics', () => ({
  fetchPlatformComparison: vi.fn(),
  fetchMonthlyChange: vi.fn(),
  fetchSummary: vi.fn(),
}))

import * as analytics from '../api/analytics'

const api = vi.mocked(analytics)

const comparison: PlatformComparison = {
  windowStart: '2025-09-01',
  windowEnd: '2025-11-30',
  months: ['2025-09-01', '2025-10-01', '2025-11-01'],
  metrics: [
    {
      metric: 'VIEWS',
      platforms: [
        {
          platform: 'INSTAGRAM',
          sourceGranularity: 'DAY',
          cells: [
            { month: '2025-09-01', value: 3504127, coverage: 1, status: 'OK' },
            { month: '2025-10-01', value: 1637892, coverage: 1, status: 'OK' },
            { month: '2025-11-01', value: 1500000, coverage: 1, status: 'OK' },
          ],
          monthsWithData: 3,
          missingMonths: [],
          partialMonths: [],
        },
        {
          platform: 'X',
          sourceGranularity: 'MONTH',
          cells: [
            { month: '2025-09-01', value: null, coverage: null, status: 'MISSING' },
            { month: '2025-10-01', value: 3255, coverage: null, status: 'OK' },
            { month: '2025-11-01', value: 7415, coverage: null, status: 'OK' },
          ],
          monthsWithData: 2,
          missingMonths: ['2025-09-01'],
          partialMonths: [],
        },
      ],
    },
    {
      metric: 'NEW_FOLLOWERS',
      platforms: [
        {
          platform: 'INSTAGRAM',
          sourceGranularity: 'DAY',
          cells: [
            { month: '2025-09-01', value: null, coverage: null, status: 'MISSING' },
            { month: '2025-10-01', value: 902, coverage: 0.5484, status: 'PARTIAL' },
            { month: '2025-11-01', value: 1200, coverage: 1, status: 'OK' },
          ],
          monthsWithData: 2,
          missingMonths: ['2025-09-01'],
          partialMonths: ['2025-10-01'],
        },
        {
          platform: 'X',
          sourceGranularity: 'MONTH',
          cells: [
            { month: '2025-09-01', value: null, coverage: null, status: 'MISSING' },
            { month: '2025-10-01', value: 10, coverage: null, status: 'OK' },
            { month: '2025-11-01', value: 12, coverage: null, status: 'OK' },
          ],
          monthsWithData: 2,
          missingMonths: ['2025-09-01'],
          partialMonths: [],
        },
      ],
    },
  ],
}

function summaryRow(platform: 'INSTAGRAM' | 'X', metric: MetricSummary['metric']): MetricSummary {
  return {
    platform,
    metric,
    granularity: platform === 'X' ? 'MONTH' : 'DAY',
    periods: 3,
    total: 10,
    average: 3.33,
    min: 1,
    max: 5,
    firstPeriod: '2025-10-01',
    lastPeriod: '2025-11-01',
    coverage: 1,
    internalMissing: 0,
    windowStart: '2025-09-01',
    windowEnd: '2025-11-30',
    windowCoverage: 1,
    leadingMissing: 0,
    trailingMissing: 0,
    negativeCount: 0,
  }
}

const monthlyChange: MonthlyChange = {
  platform: 'X',
  metric: 'VIEWS',
  sourceGranularity: 'MONTH',
  windowStart: '2025-10-01',
  windowEnd: '2025-12-31',
  rows: [
    { month: '2025-10-01', value: 3255, coverage: null, status: 'OK', previousValue: null, changePercent: null, comparable: false, note: null },
    { month: '2025-11-01', value: 7415, coverage: null, status: 'OK', previousValue: 3255, changePercent: 127.8, comparable: true, note: null },
    { month: '2025-12-01', value: null, coverage: null, status: 'MISSING', previousValue: 7415, changePercent: null, comparable: false, note: 'Bu ay için veri yok' },
  ],
}

function mockAll(overrides: Partial<{ comparison: PlatformComparison; summary: MetricSummary[] }> = {}) {
  api.fetchPlatformComparison.mockResolvedValue(overrides.comparison ?? comparison)
  api.fetchSummary.mockResolvedValue(overrides.summary ?? [summaryRow('X', 'VIEWS'), summaryRow('INSTAGRAM', 'VIEWS')])
  api.fetchMonthlyChange.mockResolvedValue(monthlyChange)
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('ComparePage', () => {
  it('shows the window and the month one platform lacks as "Veri yok", never as zero', async () => {
    mockAll()
    render(<ComparePage />)

    const section = await screen.findByRole('region', { name: 'Platform karşılaştırması' })
    expect(within(section).getByText(/Analiz penceresi/)).toHaveTextContent('01.09.2025 – 30.11.2025')
    expect(within(section).getByText('Eylül 2025')).toBeInTheDocument()
    expect(within(section).getByText('Veri yok')).toHaveClass('missing-cell')
    expect(within(section).getByText('X: Eylül 2025 için veri yok')).toBeInTheDocument()
    expect(within(section).getByText('3.504.127')).toBeInTheDocument()
    expect(within(section).getByText('3.255')).toBeInTheDocument()
  })

  it('tells where each platform value comes from', async () => {
    mockAll()
    render(<ComparePage />)

    const section = await screen.findByRole('region', { name: 'Platform karşılaştırması' })
    expect(within(section).getByText('Instagram: günlük veriden aya toplandı')).toBeInTheDocument()
    expect(within(section).getByText('X: platformun aylık değeri')).toBeInTheDocument()
  })

  it('switches metric and shows partial months with their coverage', async () => {
    const user = userEvent.setup()
    mockAll()
    render(<ComparePage />)
    const section = await screen.findByRole('region', { name: 'Platform karşılaştırması' })

    await user.selectOptions(within(section).getByLabelText('Metrik'), 'NEW_FOLLOWERS')

    expect(within(section).getByText(/kısmi, %55/)).toBeInTheDocument()
    expect(within(section).getByText('Instagram: Ekim 2025 için kısmi veri')).toBeInTheDocument()
    expect(within(section).getAllByText('Veri yok')).toHaveLength(2)
  })

  it('explains when there is nothing to compare', async () => {
    mockAll({ comparison: { windowStart: null, windowEnd: null, months: [], metrics: [] }, summary: [] })
    render(<ComparePage />)

    expect(await screen.findByText(/en az iki platformdan/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Aylık değişim' })).not.toBeInTheDocument()
  })

  it('loads the monthly change of the first series and formats percentages', async () => {
    mockAll()
    render(<ComparePage />)

    const section = await screen.findByRole('region', { name: 'Aylık değişim' })
    await waitFor(() => expect(api.fetchMonthlyChange).toHaveBeenCalledWith('X', 'VIEWS'))
    expect(await within(section).findByText('+127,8%')).toBeInTheDocument()
    expect(within(section).getByText('Bu ay için veri yok', { selector: 'td:last-child' })).toBeInTheDocument()
    expect(within(section).getByText('Kaynak veri: Aylık')).toBeInTheDocument()
  })

  it('reloads the monthly change when another series is chosen', async () => {
    const user = userEvent.setup()
    mockAll()
    render(<ComparePage />)
    const section = await screen.findByRole('region', { name: 'Aylık değişim' })
    await waitFor(() => expect(api.fetchMonthlyChange).toHaveBeenCalledTimes(1))

    await user.selectOptions(within(section).getByLabelText('Seri'), 'INSTAGRAM|VIEWS')

    await waitFor(() => expect(api.fetchMonthlyChange).toHaveBeenLastCalledWith('INSTAGRAM', 'VIEWS'))
  })

  it('shows an error when loading fails', async () => {
    api.fetchPlatformComparison.mockRejectedValue(new Error('boom'))
    api.fetchSummary.mockResolvedValue([])
    render(<ComparePage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu')
  })
})

import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { UploadIssue } from '../api/types'
import { IssuesTable } from './IssuesTable'

const issues: UploadIssue[] = [
  { severity: 'ERROR', sheet: 'Ziyaretler', row: 3, field: null, message: 'Geçersiz tarih: abc' },
  { severity: 'WARNING', sheet: null, row: 0, field: 'ENGAGEMENT', message: 'Negatif değer: 2025-09-19 = -7.' },
  { severity: 'INFO', sheet: null, row: 0, field: 'NEW_FOLLOWERS', message: 'Metrik 2025-10-15 tarihinde başlıyor.' },
]

describe('IssuesTable', () => {
  it('shows a separate badge for errors, warnings and info notes', () => {
    render(<IssuesTable issues={issues} />)

    expect(screen.getByText('Hata')).toHaveClass('badge-error')
    expect(screen.getByText('Uyarı')).toHaveClass('badge-warning')
    expect(screen.getByText('Bilgi')).toHaveClass('badge-info')
  })

  it('shows Turkish metric names and dashes for missing sheet or row', () => {
    render(<IssuesTable issues={issues} />)

    expect(screen.getByText('Etkileşim')).toBeInTheDocument()
    expect(screen.getByText('Yeni takip')).toBeInTheDocument()
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(4)
  })

  it('has an empty state', () => {
    render(<IssuesTable issues={[]} />)

    expect(screen.getByText('Hata veya uyarı yok.')).toBeInTheDocument()
  })
})

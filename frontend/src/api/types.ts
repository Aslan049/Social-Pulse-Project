export const PLATFORMS = ['INSTAGRAM', 'X'] as const
export type Platform = (typeof PLATFORMS)[number]

export const METRICS = [
  'VIEWS',
  'ENGAGEMENT',
  'NEW_FOLLOWERS',
  'PROFILE_VISITS',
  'REACH',
  'LINK_CLICKS',
  'UNFOLLOWS',
  'LIKES',
  'REPLIES',
  'REPOSTS',
  'POSTS_CREATED',
] as const
export type Metric = (typeof METRICS)[number]

export type Granularity = 'DAY' | 'MONTH'
export type UploadStatus = 'RECEIVED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type IssueSeverity = 'ERROR' | 'WARNING' | 'INFO'

export interface SheetSummary {
  name: string
  rowCount: number
  columnCount: number
  header: string[]
}

export interface UploadIssue {
  severity: IssueSeverity
  sheet: string | null
  row: number
  field: string | null
  message: string
}

export interface UploadResult {
  id: number
  platform: Platform
  filename: string
  status: UploadStatus
  totalRows: number
  importedRows: number
  rejectedRows: number
  warningCount: number
  uploadedAt: string
  sheets: SheetSummary[]
  ignoredSheets: string[]
  ignoredColumns: string[]
  issues: UploadIssue[]
}

export interface MetricSummary {
  platform: Platform
  metric: Metric
  granularity: Granularity
  /** Geçerli (negatif olmayan) değeri olan dönem sayısı */
  periods: number
  total: number
  average: number
  min: number
  max: number
  firstPeriod: string
  lastPeriod: string
  /** Kendi aralığında (ilk-son dönem) gözlenen oran: gerçek boşluk var mı? */
  coverage: number
  internalMissing: number
  /** Analiz penceresi: tüm platformların aralığının birleşimi */
  windowStart: string
  windowEnd: string
  windowCoverage: number
  /** Pencerenin başındaki, metrik başlamadan önceki dönemler (eksik veri olmayabilir) */
  leadingMissing: number
  trailingMissing: number
  negativeCount: number
}

export type CellStatus = 'OK' | 'PARTIAL' | 'MISSING'

export interface MonthCell {
  month: string
  value: number | null
  coverage: number | null
  status: CellStatus
}

export interface PlatformSeries {
  platform: Platform
  sourceGranularity: Granularity
  cells: MonthCell[]
  monthsWithData: number
  missingMonths: string[]
  partialMonths: string[]
}

export interface MetricComparison {
  metric: Metric
  platforms: PlatformSeries[]
}

export interface PlatformComparison {
  windowStart: string | null
  windowEnd: string | null
  months: string[]
  metrics: MetricComparison[]
}

export interface MonthlyChangeRow {
  month: string
  value: number | null
  coverage: number | null
  status: CellStatus
  previousValue: number | null
  changePercent: number | null
  comparable: boolean
  note: string | null
}

export interface MonthlyChange {
  platform: Platform
  metric: Metric
  sourceGranularity: Granularity | null
  windowStart: string | null
  windowEnd: string | null
  rows: MonthlyChangeRow[]
}

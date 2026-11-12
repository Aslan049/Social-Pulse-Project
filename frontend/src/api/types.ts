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
export type IssueSeverity = 'ERROR' | 'WARNING'

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
  periods: number
  total: number
  average: number
  min: number
  max: number
  firstPeriod: string
  lastPeriod: string
  coverage: number
}

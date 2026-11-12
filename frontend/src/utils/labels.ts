import type { Granularity, Metric, Platform, UploadStatus } from '../api/types'

export const PLATFORM_LABELS: Record<Platform, string> = {
  INSTAGRAM: 'Instagram',
  X: 'X',
}

export const METRIC_LABELS: Record<Metric, string> = {
  VIEWS: 'Görüntüleme',
  ENGAGEMENT: 'Etkileşim',
  NEW_FOLLOWERS: 'Yeni takip',
  PROFILE_VISITS: 'Profil ziyareti',
  REACH: 'Erişim',
  LINK_CLICKS: 'Bağlantı tıklaması',
  UNFOLLOWS: 'Takibi bırakma',
  LIKES: 'Beğeni',
  REPLIES: 'Yanıt',
  REPOSTS: 'Yeniden gönderi',
  POSTS_CREATED: 'Gönderi oluşturma',
}

export const GRANULARITY_LABELS: Record<Granularity, string> = {
  DAY: 'Günlük',
  MONTH: 'Aylık',
}

export const STATUS_LABELS: Record<UploadStatus, string> = {
  RECEIVED: 'Alındı',
  PROCESSING: 'İşleniyor',
  COMPLETED: 'Tamamlandı',
  FAILED: 'Başarısız',
}

/** Bilinmeyen bir anahtar gelirse (yeni metrik) anahtarın kendisini gösterir. */
export function metricLabel(key: string | null | undefined): string {
  if (!key) {
    return '—'
  }
  return (METRIC_LABELS as Record<string, string>)[key] ?? key
}

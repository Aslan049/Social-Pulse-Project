export const MAX_FILE_BYTES = 10 * 1024 * 1024
const ALLOWED_EXTENSIONS = ['csv', 'xlsx']

/** Geçerliyse null, değilse Türkçe hata mesajı döner. Sunucu aynı denetimleri yeniden yapar. */
export function validateFile(file: File): string | null {
  if (file.size === 0) {
    return 'Dosya boş'
  }
  if (file.size > MAX_FILE_BYTES) {
    return "Dosya 10 MB'tan büyük olamaz"
  }
  const dot = file.name.lastIndexOf('.')
  const extension = dot < 0 ? '' : file.name.slice(dot + 1).toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(extension)) {
    return 'Yalnızca .csv ve .xlsx dosyaları desteklenir'
  }
  return null
}

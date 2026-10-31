const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isValidEmail(value: string): boolean {
  return value.length <= 255 && EMAIL_PATTERN.test(value.trim())
}

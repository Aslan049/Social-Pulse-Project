import { describe, expect, it } from 'vitest'
import { MAX_FILE_BYTES, validateFile } from './files'

function fileOf(name: string, size: number) {
  return new File([new Uint8Array(size)], name)
}

describe('validateFile', () => {
  it('accepts csv and xlsx files regardless of letter case', () => {
    expect(validateFile(fileOf('rapor.csv', 10))).toBeNull()
    expect(validateFile(fileOf('RAPOR.XLSX', 10))).toBeNull()
  })

  it('rejects other extensions and names without extension', () => {
    expect(validateFile(fileOf('notlar.txt', 10))).toBe('Yalnızca .csv ve .xlsx dosyaları desteklenir')
    expect(validateFile(fileOf('rapor', 10))).toBe('Yalnızca .csv ve .xlsx dosyaları desteklenir')
    expect(validateFile(fileOf('rapor.xls', 10))).toBe('Yalnızca .csv ve .xlsx dosyaları desteklenir')
  })

  it('rejects empty files', () => {
    expect(validateFile(fileOf('bos.csv', 0))).toBe('Dosya boş')
  })

  it('rejects files above the size limit but accepts the limit itself', () => {
    expect(validateFile(fileOf('buyuk.csv', MAX_FILE_BYTES + 1))).toBe("Dosya 10 MB'tan büyük olamaz")
    expect(validateFile(fileOf('sinir.csv', MAX_FILE_BYTES))).toBeNull()
  })
})

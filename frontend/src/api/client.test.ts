import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { api } from './client'

/** Ağa çıkmadan, axios'un gövdeyi nasıl hazırladığını yakalar. */
function capture() {
  const seen: { data?: unknown; contentType?: unknown } = {}
  const adapter = async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    seen.data = config.data
    seen.contentType = config.headers.get('Content-Type')
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
  }
  return { seen, adapter }
}

describe('api client', () => {
  it('keeps FormData bodies as FormData so file uploads stay multipart', async () => {
    const { seen, adapter } = capture()
    const form = new FormData()
    form.append('platform', 'INSTAGRAM')

    await api.post('/api/uploads', form, { adapter })

    expect(seen.data).toBeInstanceOf(FormData)
  })

  it('still sends plain objects as JSON', async () => {
    const { seen, adapter } = capture()

    await api.post('/api/auth/login', { email: 'a@b.com' }, { adapter })

    expect(seen.data).toBe('{"email":"a@b.com"}')
    expect(String(seen.contentType)).toContain('application/json')
  })
})

import axios from 'axios'

export const TOKEN_KEY = 'socialpulse.token'
export const UNAUTHORIZED_EVENT = 'socialpulse:unauthorized'

// Content-Type bilerek ayarlanmaz: axios JSON gövdeler için application/json'u, FormData için sınır (boundary)
// bilgisiyle multipart/form-data'yı kendisi belirler. Sabit bir application/json başlığı, FormData gövdesini
// JSON'a çevirip dosya yüklemeyi bozar.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Her isteğe, varsa, saklanan JWT'yi ekler
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Korumalı bir uç 401 dönerse oturum süresi dolmuş demektir: token silinir, uygulamaya haber verilir
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = String(error?.config?.url ?? '').startsWith('/api/auth/')
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem(TOKEN_KEY)
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

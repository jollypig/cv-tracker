import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1',
  timeout: 10_000,
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const responseData = error.response?.data
      if (typeof responseData === 'object' && responseData !== null) {
        const detail = (responseData as { detail?: unknown }).detail
        if (typeof detail === 'string' && detail.length > 0) {
          error.message = detail
        }
      }

      if (error.response?.status === 403) {
        window.dispatchEvent(new Event('cv:forbidden'))
      } else if (error.response?.status === 401 && !error.config?.url?.endsWith('/auth/me')) {
        window.dispatchEvent(new Event('cv:unauthorized'))
      }
    }

    return Promise.reject(error)
  },
)

export default apiClient
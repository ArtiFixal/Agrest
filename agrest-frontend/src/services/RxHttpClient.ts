import { useAuthStore } from '@/stores/AuthStore'
import { catchError, map, retry, throwError, timeout, type Observable } from 'rxjs'
import { ajax } from 'rxjs/ajax'

interface RequestConfig {
  url?: string
  method?: string
  body?: unknown
  headers?: Record<string, string>
  requiresAuth?: boolean
  timeout?: number
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://localhost:8443'

class RxHttpClient {
  private defaultHeaders = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  }

  private request<T>(config: RequestConfig): Observable<T> {
    const authStore = useAuthStore()

    if (config.requiresAuth !== false && !authStore.isAuth) {
      return throwError(() => new Error('Unauthorized'))
    }

    const ajaxConfig = {
      url: `${API_BASE_URL}${config.url}`,
      method: config.method || 'GET',
      headers: { ...this.defaultHeaders, ...config.headers },
      body: config.body,
      withCredentials: true,
      timeout: config.timeout || 5000,
      crossDomain: true,
    }

    return ajax<T>(ajaxConfig).pipe(
      map((response) => response.response),
      timeout(5000),
      retry({ count: 2, delay: 1000 }),
      catchError(this.handleError),
    )
  }

  private handleError(error: any): Observable<never> {
    console.error('HTTP Error:', error)

    return throwError(() => ({
      message: error.response?.message || 'Network error',
      status: error.status,
      code: error.response?.code,
    }))
  }

  get<T>(url: string, requiresAuth = true): Observable<T> {
    return this.request<T>({ url, method: 'GET', requiresAuth })
  }

  post<T>(url: string, body: unknown, requiresAuth = true): Observable<T> {
    return this.request<T>({ url, method: 'POST', body, requiresAuth })
  }

  put<T>(url: string, body: unknown, requiresAuth = true): Observable<T> {
    return this.request<T>({ url, method: 'PUT', body, requiresAuth })
  }

  delete<T>(url: string, requiresAuth = true): Observable<T> {
    return this.request<T>({ url, method: 'DELETE', requiresAuth })
  }

  patch<T>(url: string, body: unknown, requiresAuth = true): Observable<T> {
    return this.request<T>({ url, method: 'PATCH', body, requiresAuth })
  }
}

export const httpClient = new RxHttpClient()

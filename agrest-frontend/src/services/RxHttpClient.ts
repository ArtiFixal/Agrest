import { useAuthStore } from '@/stores/AuthStore'
import { catchError, map, mergeMap, retry, take, throwError, timeout, type Observable } from 'rxjs'
import { ajax } from 'rxjs/ajax'

interface RequestConfig {
  url?: string
  method?: string
  body?: unknown
  headers?: Record<string, string>
  requiresAuth?: boolean
  timeout?: number
}

const CSRF_COOKIE = 'csrf'
const CSRF_HEADER = 'X-XSRF-TOKEN'
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
      headers: config.headers ?? { ...this.defaultHeaders },
      body: config.body,
      withCredentials: true,
      timeout: config.timeout || 5000,
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

  post<T>(
    url: string,
    body: unknown,
    requiresAuth = true,
    headers?: Record<string, string>,
  ): Observable<T> {
    if (requiresAuth)
      return this.getCsrfToken().pipe(
        take(1),
        mergeMap((token) => {
          return this.request<T>({
            url,
            method: 'POST',
            body,
            requiresAuth,
            headers: { ...headers, [CSRF_HEADER]: token },
          })
        }),
      )
    else
      return this.request<T>({
        url,
        method: 'POST',
        body,
        requiresAuth,
        headers,
      })
  }

  put<T>(
    url: string,
    body: unknown,
    requiresAuth = true,
    headers?: Record<string, string>,
  ): Observable<T> {
    return this.getCsrfToken().pipe(
      mergeMap((token) => {
        return this.request<T>({
          url,
          method: 'PUT',
          body,
          requiresAuth,
          headers: { ...headers, [CSRF_HEADER]: token },
        })
      }),
    )
  }

  delete<T>(url: string, requiresAuth = true): Observable<T> {
    return this.getCsrfToken().pipe(
      mergeMap((token) => {
        return this.request<T>({
          url,
          method: 'DELETE',
          requiresAuth,
          headers: { [CSRF_HEADER]: token },
        })
      }),
    )
  }

  patch<T>(
    url: string,
    body: unknown,
    requiresAuth = true,
    headers?: Record<string, string>,
  ): Observable<T> {
    return this.getCsrfToken().pipe(
      mergeMap((token) => {
        return this.request<T>({
          url,
          method: 'PATCH',
          body,
          requiresAuth,
          headers: { ...headers, [CSRF_HEADER]: token },
        })
      }),
    )
  }

  getCsrfToken(): Observable<string> {
    return ajax({ url: API_BASE_URL + '/v1/csrf', method: 'GET', withCredentials: true }).pipe(
      take(1),
      map((response) => {
        if (response.status === 200) {
          const match = document.cookie.match(new RegExp(`(^| )${CSRF_COOKIE}=([^;]+)`))
          if (match) return decodeURIComponent(match[2]!)
        }
        throw Error('No CSRF cookie')
      }),
    )
  }
}

export const httpClient = new RxHttpClient()

import {
  CURRENT_PAGE_HEADER,
  PAGE_SIZE_HEADER,
  TOTAL_COUNT_HEADER,
  TOTAL_PAGES_HEADER,
  type PaginationInfo,
} from '@/models/PaginationInfo'
import { Observable, Subscription } from 'rxjs'
import { onScopeDispose, ref } from 'vue'
import { fetchEventSource, type FetchEventSourceInit } from '@microsoft/fetch-event-source'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://localhost:8443'

interface SSEOptions {
  url: string
  onItem?: (item: unknown, index: number) => void
}

class FatalError extends Error {}

export function useSSEStream<T>(options: SSEOptions) {
  const items = ref<(T | null)[]>([])
  const isLoading = ref(false)
  const error = ref<Error | null>(null)
  const paginationHeaders = ref<PaginationInfo | null>(null)

  let currentIndex = 0
  let abortController: AbortController | null = null
  let subscription: Subscription | null = null

  const createSSEObservable = (url: string): Observable<T> => {
    return new Observable((subscriber) => {
      abortController = new AbortController()

      const fetchOptions: FetchEventSourceInit = {
        credentials: 'include',
        signal: abortController.signal,
        openWhenHidden: true,
        headers: {
          Accept: 'text/event-stream',
        },

        async onopen(response) {
          if (response.ok) {
            const totalCount = parseInt(response.headers.get(TOTAL_COUNT_HEADER) || '0')
            const pageSize = parseInt(response.headers.get(PAGE_SIZE_HEADER) || '0')
            const currentPage = parseInt(response.headers.get(CURRENT_PAGE_HEADER) || '0')
            const totalPages = parseInt(response.headers.get(TOTAL_PAGES_HEADER) || '0')

            paginationHeaders.value = {
              totalCount,
              totalPages,
              pageSize,
              currentPage,
            }
            let fetchedItems =
              currentPage >= totalPages - 1
                ? totalCount - currentPage * pageSize
                : Math.min(pageSize, totalCount)
            if (fetchedItems < 0) fetchedItems = 0
            items.value = Array(fetchedItems).fill(null)
          } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
            throw new FatalError(`Non-retryable error HTTP ${response.status}`)
          } else {
            throw new Error(`Unexpected error: ${response.status}`)
          }
        },

        onmessage(event) {
          try {
            const data: T = JSON.parse(event.data)
            subscriber.next(data)
          } catch (e) {
            console.error('Parse error:', e)
            subscriber.error(e)
          }
        },

        onclose() {
          subscriber.complete()
        },

        onerror(err) {
          if (err instanceof FatalError) {
            subscriber.error(err)
            throw err
          }
        },
      }

      fetchEventSource(`${API_BASE_URL}${url}`, fetchOptions).catch((err) => {
        if (err.name !== 'AbortError') {
          subscriber.error(err)
        }
      })

      return () => {
        if (abortController) {
          abortController.abort()
        }
      }
    })
  }

  const startStream = () => {
    if (isLoading.value) {
      console.warn('Stream already open')
      return
    }

    isLoading.value = true
    error.value = null
    items.value = []
    currentIndex = 0

    subscription = createSSEObservable(options.url).subscribe({
      next: (data) => {
        if (currentIndex < items.value.length) {
          ;(items.value as T[])[currentIndex] = data
          if (options.onItem) options.onItem(data, currentIndex)
          currentIndex++
          items.value = [...items.value]
        }
      },
      error: (err) => {
        console.error('Stream error:', err.message)
        error.value = err
        isLoading.value = false
      },
      complete: () => {
        isLoading.value = false
      },
    })
  }

  const stopStream = () => {
    if (abortController) {
      abortController.abort()
      abortController = null
    }

    if (subscription) {
      subscription.unsubscribe()
      subscription = null
    }

    isLoading.value = false
  }

  onScopeDispose(() => {
    stopStream()
  })

  return {
    items,
    isLoading,
    error,
    paginationHeaders,
    startStream,
    stopStream,
  }
}

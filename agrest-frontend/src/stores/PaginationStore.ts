import { httpClient } from '@/services/RxHttpClient'
import { defineStore } from 'pinia'
import { take } from 'rxjs'
import { ref } from 'vue'

export const usePaginationStore = defineStore(
  'pagination',
  () => {
    const pageSize = ref<string>('0')
    const availablePageSizes = ref<string[]>([])

    const init = async () => {
      if (!availablePageSizes.value.length)
        httpClient
          .get<string[]>('/v1/pagination', true)
          .pipe(take(1))
          .subscribe({
            next: (result) => {
              availablePageSizes.value = result
              if (pageSize.value == '0' && result[0]) pageSize.value = result[0]
            },
          })
    }

    return {
      init,
      pageSize,
      availablePageSizes,
    }
  },
  {
    persist: {
      storage: localStorage,
    },
  },
)

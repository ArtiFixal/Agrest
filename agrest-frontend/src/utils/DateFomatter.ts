import { useTimeAgo } from '@vueuse/core'
import type { ComputedRef } from 'vue'

export function formatLastScan(lastScanDate: Date): ComputedRef<string> {
  return useTimeAgo(lastScanDate, {
    max: 'year',
    fullDateFormatter: (date) => {
      return `${date.getDay()}.${date.getMonth()}.${date.getFullYear()}`
    },
  })
}

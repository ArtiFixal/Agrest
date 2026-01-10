export const TOTAL_PAGES_HEADER = 'X-Total-Pages'
export const TOTAL_COUNT_HEADER = 'X-Total-Count'
export const CURRENT_PAGE_HEADER = 'X-Current-Page'
export const PAGE_SIZE_HEADER = 'X-Page-Size'

export interface PaginationInfo {
  totalCount: number
  pageSize: number
  currentPage: number
  totalPages: number
}

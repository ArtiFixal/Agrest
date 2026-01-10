<script setup lang="ts">
import type { PaginationInfo } from '@/models/PaginationInfo'
import { usePaginationStore } from '@/stores/PaginationStore'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from './ui/select'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from './ui/pagination'

const props = defineProps<{
  paginationHeaders: PaginationInfo | null
  currentPage: number
}>()
const emit = defineEmits<{
  'update:page': [page: number]
}>()

const paginationStore = usePaginationStore()
</script>

<template>
  <footer class="flex mt-6">
    <Select v-model:model-value="paginationStore.pageSize">
      <SelectTrigger class="w-[180px]">
        <SelectValue placeholder="Select elements per page" />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>Page sizes</SelectLabel>
          <SelectItem
            v-for="pageSize in paginationStore.availablePageSizes"
            :key="pageSize"
            :value="pageSize"
            >{{ pageSize }}</SelectItem
          >
        </SelectGroup>
      </SelectContent>
    </Select>

    <Pagination
      v-slot="{ page }"
      :items-per-page="parseInt(paginationStore.pageSize)"
      :total="paginationHeaders?.totalCount"
      :default-page="currentPage"
      @update:page="emit('update:page', $event)"
    >
      <PaginationContent v-slot="{ items }">
        <PaginationPrevious />

        <template v-for="(item, index) in items" :key="index">
          <PaginationItem
            v-if="item.type === 'page'"
            :value="item.value"
            :is-active="item.value === page"
          >
            {{ item.value }}
          </PaginationItem>
        </template>

        <PaginationEllipsis :index="4" />

        <PaginationNext />
      </PaginationContent>
    </Pagination>
  </footer>
</template>

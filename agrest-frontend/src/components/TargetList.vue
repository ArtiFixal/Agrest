<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Circle, Edit, Eye, MoreVertical, Trash2 } from 'lucide-vue-next'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import type { PaginationInfo } from '@/models/PaginationInfo'
import { TargetService } from '@/services/TargetService'
import { formatLastScan } from '@/utils/DateFomatter'
import { getStatusClass } from '@/utils/StatusUtils'
import { onMounted, watch } from 'vue'

const props = defineProps<{ page: number; pageSize: number }>()
const emit = defineEmits<{
  headersLoaded: [headers: PaginationInfo]
  editTarget: [targetID: number]
}>()

const targetService = new TargetService()

const { items, startStream, isLoading, paginationHeaders } = targetService.getTargetPageStream(
  props.page,
  props.pageSize,
)

watch(
  paginationHeaders,
  (newHeaders) => {
    if (newHeaders) emit('headersLoaded', newHeaders)
  },
  { immediate: true },
)

onMounted(() => {
  startStream()
})
</script>

<template>
  <section class="grid gap-4">
    <article v-for="(target, index) in items" :key="index">
      <Card class="p-6 hover:bg-secondary/50 transition-colors">
        <div v-if="target" class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-2">
              <Circle class="h-3 w-3 fill-current" :class="getStatusClass(target.status)" />
              <h3 class="text-lg font-semibold text-foreground">{{ target.name }}</h3>
            </div>
            <p class="font-mono text-sm text-muted-foreground mb-3">{{ target.url }}</p>
            <div class="flex flex-wrap gap-2 mb-3">
              <Badge
                v-for="tag in target.tags"
                :key="tag.id"
                :id="`tag-${tag.id}`"
                variant="secondary"
                >{{ tag.name }}</Badge
              >
            </div>
            <div class="flex gap-6 text-sm">
              <span class="text-muted-foreground">
                Last Scan:
                <span class="text-foreground capitalize">{{
                  target.lastScan ? formatLastScan(target.lastScan) : 'never'
                }}</span>
              </span>
              <span> </span>
            </div>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                <MoreVertical class="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem as-child>
                <RouterLink :to="`/target/${target.id}`">
                  <Eye class="mr-2 w-4 h-4" />
                  View Details
                </RouterLink>
              </DropdownMenuItem>
              <DropdownMenuItem @click="emit('editTarget', target.id)">
                <Edit class="mr-2 w-4 h-4" />
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem class="text-destructive focus:text-destructive">
                <Trash2 class="mr-2 w-4 h-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div v-else class="flex flex-col">
          <div class="flex mb-3">
            <Skeleton class="h-4 w-4 mr-2 rounded-full" />
            <Skeleton class="h-4 w-[240px]" />
          </div>
          <Skeleton class="h-4 mb-3 w-[140px]" />
          <Skeleton class="h-4 mb-3 w-[180px]" />
          <Skeleton class="h-4 mb-3 w-[120px]" />
        </div>
      </Card>
    </article>
    <article v-if="!items.length && !isLoading">
      <div class="flex items-start justify-between">No targets</div>
    </article>
  </section>
</template>

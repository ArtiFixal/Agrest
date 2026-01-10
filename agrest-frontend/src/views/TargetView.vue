<script setup lang="ts">
import { OpenMode } from '@/components/models/OpenMode'
import TargetDialog from '@/components/TargetDialog.vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import Input from '@/components/ui/input/Input.vue'
import type { TargetCreationDTO } from '@/models/target/TargetCreationDTO'
import { Circle, Edit, Eye, MoreVertical, Plus, Search, Trash2 } from 'lucide-vue-next'
import { computed, onUnmounted, ref, shallowRef, watch } from 'vue'
import { formatLastScan } from '@/utils/DateFomatter'
import { Skeleton } from '@/components/ui/skeleton'
import { TargetService } from '@/services/TargetService'
import { usePaginationStore } from '@/stores/PaginationStore'
import { take } from 'rxjs'
import { useRouter } from 'vue-router'
import PaginationFooter from '@/components/PaginationFooter.vue'
import type { Target } from '@/models/target/Target'
import { getStatusClass } from '@/utils/StatusUtils'

const props = defineProps<{ page?: number }>()
const targetService = new TargetService()

const isOpen = ref(false)

const targetData = ref<TargetCreationDTO>()
const dialogMode = ref<OpenMode>(OpenMode.ADD)

function openAdd() {
  targetData.value = { name: '', url: '' }
  dialogMode.value = OpenMode.ADD
  isOpen.value = true
}

function openEdit(targetID: number) {
  targetService
    .getTargetForUpdate(targetID)
    .pipe(take(1))
    .subscribe({
      next: (result) => {
        result.id = targetID
        targetData.value = result
        dialogMode.value = OpenMode.EDIT
        isOpen.value = true
      },
    })
}

function changePage(newPage: number) {
  router.push(`/targets/${newPage}`)
}

const router = useRouter()
const currentPage = computed(() => props.page ?? 1)
const paginationStore = usePaginationStore()

const currentStream = shallowRef<ReturnType<typeof targetService.getTargetPageStream> | null>(null)
// const { items, startStream, stopStream, isLoading, paginationHeaders } =
//   targetService.getTargetPageStream(currentPage.value - 1, parseInt(paginationStore.pageSize))

const itemsSorted = ref<(Target | null)[]>([])

watch(
  () => currentStream.value?.items.value ?? [],
  (newItems) => {
    const actualItems = newItems.filter((item) => item !== null)
    const nullCount = newItems.filter((item) => item === null).length
    actualItems.sort((a, b) => a.id - b.id)
    itemsSorted.value = [...actualItems, ...Array(nullCount).fill(null)]
  },
  { deep: true },
)

watch(
  [() => props.page, () => paginationStore.pageSize],
  () => {
    currentStream.value = targetService.getTargetPageStream(
      currentPage.value - 1,
      parseInt(paginationStore.pageSize),
    )
    currentStream.value.startStream()
  },
  { immediate: true },
)

onUnmounted(() => {
  currentStream.value?.stopStream()
})
</script>

<template>
  <section class="flex-1 overflow-y-auto">
    <div class="mb-8 flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-foreground">Targets</h1>
        <p class="mt-2 text-muted-foreground">Manage your security testing targets</p>
      </div>
      <Button class="gap-2" @click="openAdd">
        <Plus class="h-4 w-4" />
        Add Target
      </Button>
    </div>

    <Card class="mb-6 p-4">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[300px]">
          <div class="relative">
            <Search
              class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
            />
            <Input placeholder="Search by name or URL..." class="pl-10" />
          </div>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="outline" class="min-w-[140px] bg-transparent">Status: All</Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem>All</DropdownMenuItem>
            <DropdownMenuItem>Online</DropdownMenuItem>
            <DropdownMenuItem>Offline</DropdownMenuItem>
            <DropdownMenuItem>Unknown</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </Card>

    <section class="grid gap-4">
      <article v-for="(target, index) in itemsSorted" :key="index">
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
                <DropdownMenuItem @click="openEdit(target.id)">
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
      <article v-if="!itemsSorted.length && !currentStream?.isLoading.value">
        <div class="flex items-start justify-between">No targets</div>
      </article>
    </section>
    <PaginationFooter
      :pagination-headers="currentStream?.paginationHeaders.value ?? null"
      :current-page="currentPage"
      @update:page="changePage"
    ></PaginationFooter>
  </section>
  <TargetDialog v-model:open="isOpen" :mode="dialogMode" :data="targetData"></TargetDialog>
</template>

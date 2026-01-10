<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import type { TargetDetails } from '@/models/target/TargetDetails'
import { TargetService } from '@/services/TargetService'
import { getStatusName } from '@/utils/StatusUtils'
import { useTimeAgo } from '@vueuse/core'
import { ArrowLeft, Circle, Play } from 'lucide-vue-next'
import { take } from 'rxjs'
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps<{ id?: number }>()
const targetService = new TargetService()
const target = ref<TargetDetails>()
const lastScanTime = useTimeAgo(target.value?.lastScan ?? '', {})

onMounted(() => {
  if (props.id)
    targetService
      .getTargetDetails(props.id)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          target.value = response
        },
      })
})
</script>

<template>
  <div class="mb-6">
    <RouterLink to="/targets">
      <Button variant="ghost" class="gap-2 mb-4">
        <ArrowLeft class="w-4 h-4" />
        Back to Targets
      </Button>
    </RouterLink>
    <div class="flex items-start justify-between">
      <div>
        <div class="flex items-center gap-3 mb-2">
          <Circle class="w-3 h-3 fill-current" />
          <h1 class="text-3xl font-bold text-foreground">{{ target?.name }}</h1>
        </div>
        <p class="font-mono text-muted-foreground">{{ target?.url }}</p>
      </div>
      <Button class="gap-2">
        <Play class="w-4 h-4" />
        Start Scan
      </Button>
    </div>
  </div>

  <div class="grid gap-6 md:grid-cols-3 mb-8">
    <template v-if="target?.scans?.length">
      <Card class="p-6">
        <p class="text-sm font-medium text-muted-foreground mb-2">Total Vulnerabilities</p>
        <p class="text-3xl font-semibold text-foreground">12</p>
        <p class="text-sm text-destructive mt-2">3 Critical, 5 High</p>
      </Card>
      <Card class="p-6">
        <p class="text-sm font-medium text-muted-foreground mb-2">Last Scan</p>
        <p class="text-3xl font-semibold text-foreground">{{ lastScanTime }}</p>
        <p class="text-sm text-muted-foreground mt-2">Feb 12, 2024 14:30</p>
      </Card>
    </template>
    <template v-else-if="!target?.scans?.length">
      <Card class="p-6">
        <p class="text-sm font-medium text-muted-foreground mb-2">Target never scanned</p>
        <p class="text-3xl font-semibold text-foreground">No scans</p>
      </Card>
    </template>
  </div>

  <Tabs defaultValue="overview" class="w-full">
    <TabsList>
      <TabsTrigger value="overview">Overview</TabsTrigger>
      <TabsTrigger value="history">Scan History</TabsTrigger>
      <TabsTrigger value="config">Configuration</TabsTrigger>
    </TabsList>

    <TabsContent value="overview" class="mt-6">
      <Card class="p-6 gap-4">
        <h3 class="text-lg font-semibold text-foreground mb-2">Target Information</h3>
        <div>
          <p class="text-sm font-medium text-muted-foreground mb-1">Description</p>
          <span class="text-foreground">{{ target?.description }}</span>
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">Status</p>
            <div class="flex items-center gap-2">
              <Circle class="h-2 w-2 fill-current text-success" />
              <span class="text-foreground capitalize">{{ getStatusName(target?.status) }}</span>
            </div>
          </div>
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">Tags</p>
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="tag in target?.tags"
                :key="tag.id"
                :id="`tag${tag.id}`"
                variant="secondary"
                >{{ tag.name }}</Badge
              >
            </div>
            <div v-if="!target?.tags?.length" class="font-mono text-foreground">No tags</div>
          </div>
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">Created</p>
            <span class="font-mono text-foreground">{{ target?.created }}</span>
          </div>
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">Last updated</p>
            <span class="font-mono text-foreground">{{ target?.edited }}</span>
          </div>
        </div>
      </Card>
    </TabsContent>

    <TabsContent value="history" class="mt-6">
      <Card class="p-6 gap-4">
        <h3 class="text-lg font-semibold text-foreground mb-2">Scan History</h3>
        <div class="space-y-3">
          <div v-if="!target?.scans?.length">No scans</div>
        </div>
      </Card>
    </TabsContent>

    <TabsContent value="config" class="mt-6">
      <Card class="p-6 gap-4">
        <h3 class="text-lg font-semibold text-foreground mb-2">Configuration</h3>
        <div class="space-y-4">
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-2">Custom Headers</p>
            <div
              v-if="Object.keys(target?.headers ?? {}).length > 0"
              class="rounded-lg border border-border p-3 font-mono text-sm"
            >
              <div
                v-for="(headerValue, headerName) in target?.headers"
                :key="headerName"
                class="text-foreground"
              >
                {{ headerName }}: {{ headerValue }}
              </div>
            </div>
            <div v-else class="text-foreground">No headers</div>
          </div>
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-2">Custom Cookies</p>
            <div
              v-if="Object.keys(target?.cookies ?? {}).length > 0"
              class="rounded-lg border border-border p-3 font-mono text-sm"
            >
              <div
                v-for="(cookieValue, cookieName) in target?.cookies"
                :key="cookieName"
                class="text-foreground"
              >
                {{ cookieName }}: {{ cookieValue }}
              </div>
            </div>
            <div v-else class="text-foreground">No cookies</div>
          </div>
        </div>
      </Card>
    </TabsContent>
  </Tabs>
</template>

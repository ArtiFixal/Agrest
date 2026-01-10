<script setup lang="ts">
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import { Plus, X } from 'lucide-vue-next'
import {
  TagsInput,
  TagsInputInput,
  TagsInputItem,
  TagsInputItemDelete,
  TagsInputItemText,
} from '@/components/ui/tags-input'
import type { TargetCreationDTO } from '@/models/target/TargetCreationDTO'
import { reactive, ref, toRaw, watch } from 'vue'
import { toast } from 'vue-sonner'
import { OpenMode } from './models/OpenMode'
import { TargetService } from '@/services/TargetService'
import { take } from 'rxjs'

const prop = defineProps<{
  open: boolean
  mode: OpenMode
  data?: TargetCreationDTO
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

function handleOpenChange(value: boolean) {
  emit('update:open', value)
}

function addTag() {
  const input = tagInput.value?.$el as HTMLInputElement
  const value = input.value.trim()
  if (!value) {
    toast.error('Error', {
      description: 'Tag already exists',
    })
    return
  }
  if (target.tags && target.tags.includes(value)) target.tags = [...target.tags, value]
  input.value = ''
}

watch(
  () => prop.open,
  (isOpen) => {
    if (isOpen) {
      if (prop.data && prop.mode === OpenMode.EDIT) {
        target = structuredClone(toRaw(prop.data)) ?? {}
        cookies.value = Object.entries(target.cookies || {}).map(([key, value]) => ({
          id: crypto.randomUUID(),
          key,
          value,
        }))
        headers.value = Object.entries(target.headers || {}).map(([key, value]) => ({
          id: crypto.randomUUID(),
          key,
          value,
        }))
      } else if (prop.mode === OpenMode.ADD) {
        addCookie()
        addHeader()
      }
    } else if (!isOpen) {
      cookies.value = []
      headers.value = []
    }
  },
)

function addCookie() {
  cookies.value.push({ id: crypto.randomUUID(), key: '', value: '' })
}

function addHeader() {
  headers.value.push({ id: crypto.randomUUID(), key: '', value: '' })
}

function deleteCookie(index: number) {
  cookies.value.splice(index, 1)
}

function deleteHeaders(index: number) {
  headers.value.splice(index, 1)
}

function save() {
  if (target) {
    target.headers = new Map<string, string>(headers.value.map((entry) => [entry.key, entry.value]))
    target.cookies = new Map<string, string>(cookies.value.map((entry) => [entry.key, entry.value]))

    if (prop.mode == OpenMode.ADD) targetService.addTarget(target).pipe(take(1)).subscribe()
    else if (target.id)
      targetService
        .editTarget(target.id, prop.data!, target)
        .pipe(take(1))
        .subscribe({
          next(value) {
            console.log('update ' + value)
          },
          error(err) {
            console.error('update error', err)
          },
        })
    handleOpenChange(false)
  }
}

interface Entry {
  id: string
  key: string
  value: string
}

const targetService = new TargetService()
const tagInput = ref()
const cookies = ref<Array<Entry>>([])
const headers = ref<Array<Entry>>([])
let target = reactive<TargetCreationDTO>({
  name: '',
  url: '',
})
</script>

<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="max-w-3xl max-h-[90vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>{{ mode === OpenMode.ADD ? 'Add New Target' : 'Edit Target' }}</DialogTitle>
      </DialogHeader>

      <Tabs defaultValue="basic" class="w-full">
        <TabsList class="grid w-full grid-cols-4">
          <TabsTrigger value="basic">Basic</TabsTrigger>
          <TabsTrigger value="api">API Mapping</TabsTrigger>
          <TabsTrigger value="advanced">Advanced</TabsTrigger>
        </TabsList>

        <TabsContent value="basic" class="space-y-4 mt-4">
          <div v-if="target.id" class="space-y-2">
            <Label for="name">Target ID</Label>
            <Input id="name" v-model="target.id" disabled />
          </div>
          <div class="space-y-2">
            <Label for="name">Target Name</Label>
            <Input id="name" v-model="target.name" placeholder="Production API" />
          </div>
          <div class="space-y-2">
            <Label for="host">Host URL</Label>
            <Input id="host" v-model="target.url" placeholder="https://api.example.com" />
          </div>
          <div class="space-y-2">
            <Label for="description">Description</Label>
            <Textarea
              id="description"
              placeholder="Brief description of the target..."
              v-model="target.description"
              rows="{3}"
            ></Textarea>
          </div>
          <div class="space-y-2">
            <Label>Tags</Label>
            <div class="flex gap-2">
              <TagsInput v-model="target.tags" class="w-full">
                <TagsInputItem v-for="tag in target.tags" :key="tag" :value="tag">
                  <TagsInputItemText />
                  <TagsInputItemDelete />
                </TagsInputItem>

                <TagsInputInput ref="tagInput" placeholder="Add tag..." />
              </TagsInput>
              <Button type="button" size="icon" @click="addTag">
                <Plus class="h-4 w-4" />
              </Button>
            </div>
            <div class="flex flex-wrap gap-2 mt-2"></div>
          </div>
        </TabsContent>

        <TabsContent value="api" class="space-y-4 mt-4">
          <div class="space-y-2">
            <Label for="base-path">OpenAPI Swagger</Label>
            <Input id="base-path" type="file" />
          </div>
        </TabsContent>

        <TabsContent value="advanced" class="space-y-4 mt-4">
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <Label>Custom Headers</Label>
              <Button
                type="button"
                @click="addHeader"
                size="sm"
                variant="outline"
                class="gap-2 bg-transparent"
              >
                <Plus class="h-4 w-4" />
                Add Header
              </Button>
            </div>
            <div v-for="(header, index) in headers" :key="header.id" class="space-y-2">
              <div class="flex gap-2">
                <Input v-model="header.key" placeholder="Header name" />
                <Input v-model="header.value" placeholder="Header value" />
                <Button
                  v-if="headers.length > 1"
                  @click="deleteHeaders(index)"
                  variant="ghost"
                  size="icon"
                >
                  <X class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <Label>Custom Cookies</Label>
              <Button
                type="button"
                @click="addCookie"
                size="sm"
                variant="outline"
                class="gap-2 bg-transparent"
              >
                <Plus class="h-4 w-4" />
                Add Cookie
              </Button>
            </div>
            <div v-for="(cookie, index) in cookies" :key="cookie.id" class="space-y-2">
              <div class="flex gap-2">
                <Input v-model="cookie.key" placeholder="Cookie name" />
                <Input v-model="cookie.value" placeholder="Cookie value" />
                <Button
                  v-if="cookies.length > 1"
                  @click="deleteCookie(index)"
                  variant="ghost"
                  size="icon"
                >
                  <X class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>

      <div class="flex justify-end gap-3 mt-6">
        <Button variant="outline" @click="handleOpenChange(false)">Cancel</Button>
        <Button @click="save">{{
          mode === OpenMode.ADD ? 'Create Target' : 'Update Target'
        }}</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

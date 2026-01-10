<template>
  <div class="flex h-full justify-center items-center">
    <Card class="w-full max-w-sm">
      <CardHeader class="text-center">
        <CardTitle class="text-xl">Login</CardTitle>
      </CardHeader>
      <CardContent>
        <form ref="formRef" @submit.prevent="login">
          <div class="grid w-full items-center gap-4">
            <div class="flex flex-col space-y-1.5">
              <Label for="email">Email</Label>
              <Input
                v-model="credentials.email"
                id="email"
                type="text"
                placeholder="user@example.com"
                required
              />
            </div>
            <div class="flex flex-col space-y-1.5">
              <Label for="password">Password</Label>
              <Input
                v-model="credentials.password"
                id="password"
                type="password"
                required
                placeholder="Password"
              />
            </div>
          </div>
        </form>
      </CardContent>
      <CardFooter class="flex flex-col gap-2">
        <Button :disabled="authStore.isLoading" @click="login" class="w-full">Login</Button>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/AuthStore'
import { reactive } from 'vue'
import type { LoginCredentials } from '@/models/user/LoginCredentials'

const authStore = useAuthStore()
const credentials: LoginCredentials = reactive({
  email: '',
  password: '',
})

function login() {
  authStore.login(credentials)
}
</script>

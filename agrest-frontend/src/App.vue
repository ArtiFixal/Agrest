<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useAuthStore } from './stores/AuthStore'
import AppSidebar from './components/AppSidebar.vue'
import { Toaster } from './components/ui/sonner'
const authStore = useAuthStore()
</script>

<template>
  <aside v-if="authStore.isAuth">
    <AppSidebar />
  </aside>
  <div class="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
    <div class="w-full max-w-sm">
      <Toaster />
      <RouterView />
    </div>
  </div>
</template>

<style scoped></style>

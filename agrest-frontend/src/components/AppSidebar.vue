<script setup lang="ts">
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from '@/components/ui/sidebar'
import { useAuthStore } from '@/stores/AuthStore'
import { Blocks, FileText, LayoutDashboard, Scan, Target } from 'lucide-vue-next'
import type { FunctionalComponent } from 'vue'
import { RouterLink } from 'vue-router'

const authStore = useAuthStore()

interface NavEntry {
  name: string
  href: string
  icon: FunctionalComponent
}

const mainNav: Array<NavEntry> = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard },
  { name: 'Targets', href: '/targets', icon: Target },
  { name: 'Scans', href: '/scans', icon: Scan },
  { name: 'Reports', href: '/reports', icon: FileText },
  { name: 'Integrations', href: '/integrations', icon: Blocks },
]
</script>

<template>
  <SidebarProvider>
    <Sidebar class="border-r border-sidebar-border h-full">
      <SidebarHeader class="h-16 flex-row items-center gap-2 border-b border-border px-6">
        <h2 class="text-lg font-semibold text-foreground">Agrest</h2>
      </SidebarHeader>
      <SidebarContent class="h-full">
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem v-for="entry in mainNav" :key="entry.href">
                <SidebarMenuButton asChild>
                  <RouterLink
                    :to="entry.href"
                    exactActiveClass="bg-sidebar-accent text-sidebar-accent-foreground font-medium"
                    class="hover:bg-sidebar-accent/50"
                  >
                    <component :is="entry.icon" class="w-4 h-4" />
                    {{ entry.name }}
                  </RouterLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="border-t border-border p-4">
        <div className="flex items-center gap-3 rounded-lg px-3 py-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/20">
            <span className="text-sm font-medium text-primary">{{
              authStore.user?.email.charAt(0).toUpperCase() ?? 'U'
            }}</span>
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-foreground">
              {{ authStore.user?.email }}
            </p>
          </div>
        </div>
      </SidebarFooter>
    </Sidebar>
  </SidebarProvider>
</template>

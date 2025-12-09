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
import { RouterLink } from 'vue-router'

const authStore = useAuthStore()
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
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <RouterLink to="/" class="w-full">Dashboard</RouterLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <RouterLink to="/Targets">Targets</RouterLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <RouterLink to="/Scans">Scans</RouterLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <RouterLink to="/Reports">Reports</RouterLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton>
                  <RouterLink to="/Integrations">Integrations</RouterLink>
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

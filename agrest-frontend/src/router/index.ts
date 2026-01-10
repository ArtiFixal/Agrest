import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore'
import LoginView from '@/views/LoginView.vue'
import HomeView from '@/views/HomeView.vue'
import TargetView from '@/views/TargetView.vue'
import TargetDetailsView from '@/views/TargetDetailsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/targets/:page?',
      name: 'targets',
      component: TargetView,
      props: (route) => ({
        page: parseInt(route.params.page as string) || 1,
      }),
    },
    {
      path: '/target/:id?',
      name: 'target',
      component: TargetDetailsView,
      props: (route) => ({
        id: parseInt(route.params.id as string),
      }),
    },
  ],
})

router.beforeEach(async (route) => {
  const publicPages = ['/login']
  const authRequired = !publicPages.includes(route.path)
  const authStore = useAuthStore()
  if (authRequired && !authStore.isAuth) return '/login'
})

export default router

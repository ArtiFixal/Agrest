import type { LoginCredentials } from '@/models/user/LoginCredentials'
import type { User } from '@/models/user/User'
import router from '@/router'
import { AuthService } from '@/services/AuthService'
import { defineStore } from 'pinia'
import { take } from 'rxjs'
import { computed, ref } from 'vue'
import { toast } from 'vue-sonner'

export const useAuthStore = defineStore(
  'auth',
  () => {
    const authService = new AuthService()
    const user = ref<User | null>(null)
    const isLoading = ref(false)
    const isAuth = computed(() => user.value != null)

    function login(credentials: LoginCredentials) {
      isLoading.value = true
      authService
        .login(credentials)
        .pipe(take(1))
        .subscribe({
          next: (response: User) => {
            user.value = response
            router.push('/')
          },
          error: (err: Error) => {
            toast.error('Login failed', {
              description: err.message,
            })
          },
        })
      isLoading.value = false
    }

    return {
      user,
      isAuth,
      isLoading,
      login,
    }
  },
  {
    persist: {
      pick: ['user'],
    },
  },
)

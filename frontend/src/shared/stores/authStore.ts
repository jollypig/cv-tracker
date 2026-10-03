import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import authApi, { type AuthenticatedUser } from '../api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthenticatedUser | null>(null)
  const checked = ref(false)
  let loadRequest: Promise<boolean> | null = null

  async function load(): Promise<boolean> {
    if (checked.value) return user.value !== null
    if (loadRequest) return loadRequest

    loadRequest = authApi.currentUser()
      .then((authenticatedUser) => {
        user.value = authenticatedUser
        checked.value = true
        return true
      })
      .catch((error: unknown) => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          user.value = null
          checked.value = true
          return false
        }
        throw error
      })
      .finally(() => {
        loadRequest = null
      })

    return loadRequest
  }

  async function logout(): Promise<void> {
    await authApi.logout()
    user.value = null
    checked.value = false
  }

  function reset(): void {
    user.value = null
    checked.value = false
  }

  return { user, checked, load, logout, reset }
})
<template>
  <main class="auth-page">
    <section v-if="isForbidden" class="auth-panel" aria-labelledby="auth-title">
      <div class="auth-mark"><v-icon icon="mdi-shield-lock-outline" size="26" /></div>
      <div class="eyebrow">ACCESS CONTROL</div>
      <h1 id="auth-title">This workspace is not yours to open.</h1>
      <p>Your account does not have permission to view that record.</p>
      <v-btn color="primary" to="/" prepend-icon="mdi-arrow-left">Back to workspace</v-btn>
    </section>

    <section v-else class="auth-panel" aria-labelledby="auth-title">
      <div class="auth-brand"><span class="auth-brand-mark"><v-icon icon="mdi-file-account-outline" /></span>folio</div>
      <div class="eyebrow">PRIVATE CV WORKSPACE</div>
      <h1 id="auth-title">Sign in to your workspace</h1>
      <p v-if="loading">Checking sign-in options…</p>
      <p v-else-if="!oidcEnabled" class="auth-error">
        Sign-in is not configured. Set the OIDC issuer and client ID on the API before continuing.
      </p>
      <p v-else>Continue with your organization’s identity provider.</p>
      <v-btn
        v-if="oidcEnabled && loginHref"
        class="auth-submit"
        color="primary"
        size="large"
        block
        append-icon="mdi-arrow-right"
        :href="loginHref"
      >
        Continue with OIDC
      </v-btn>
      <v-alert v-if="error" class="auth-alert" type="error" variant="tonal">{{ error }}</v-alert>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import authApi from '../shared/api/authApi'
import { useAuthStore } from '../shared/stores/authStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const oidcEnabled = ref(false)
const loginHref = ref('')
const error = ref('')
const isForbidden = computed(() => route.meta.view === 'forbidden')

onMounted(async () => {
  if (isForbidden.value) return
  try {
    if (await auth.load()) {
      await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
      return
    }
    const config = await authApi.configuration()
    oidcEnabled.value = config.oidcEnabled
    const apiBase = new URL(import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1')
    apiBase.pathname = apiBase.pathname.replace(/\/api\/v1\/?$/, '/')
    loginHref.value = new URL(config.loginPath, apiBase).toString()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Unable to load sign-in options.'
  } finally {
    loading.value = false
  }
})
</script>
<template>
  <main class="auth-page">
    <section v-if="isForbidden" class="auth-panel" aria-labelledby="auth-title">
      <v-select :model-value="locale" :items="languageOptions" :label="translate('language.label')" :aria-label="translate('language.label')" density="compact" hide-details variant="outlined" class="auth-language" @update:model-value="changeLocale" />
      <div class="auth-mark"><v-icon icon="mdi-shield-lock-outline" size="26" /></div>
      <div class="eyebrow">{{ translate('auth.accessEyebrow') }}</div>
      <h1 id="auth-title">{{ translate('auth.forbiddenTitle') }}</h1>
      <p>{{ translate('auth.forbiddenMessage') }}</p>
      <v-btn color="primary" to="/" prepend-icon="mdi-arrow-left">{{ translate('auth.back') }}</v-btn>
    </section>

    <section v-else class="auth-panel" aria-labelledby="auth-title">
      <v-select :model-value="locale" :items="languageOptions" :label="translate('language.label')" :aria-label="translate('language.label')" density="compact" hide-details variant="outlined" class="auth-language" @update:model-value="changeLocale" />
      <div class="auth-brand"><span class="auth-brand-mark"><v-icon icon="mdi-file-account-outline" /></span>folio</div>
      <div class="eyebrow">{{ translate('auth.privateEyebrow') }}</div>
      <h1 id="auth-title">{{ translate('auth.signInTitle') }}</h1>
      <p v-if="loading">{{ translate('auth.checking') }}</p>
      <p v-else-if="!oidcEnabled" class="auth-error">
        {{ translate('auth.notConfigured') }}
      </p>
      <p v-else>{{ translate('auth.continueMessage') }}</p>
      <v-btn
        v-if="oidcEnabled && loginHref"
        class="auth-submit"
        color="primary"
        size="large"
        block
        append-icon="mdi-arrow-right"
        :href="loginHref"
      >
        {{ translate('auth.continueOidc') }}
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
import { locale, setLocale, translate, type Locale } from '../shared/i18n'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const oidcEnabled = ref(false)
const loginHref = ref('')
const error = ref('')
const isForbidden = computed(() => route.meta.view === 'forbidden')
const languageOptions = computed(() => [
  { title: translate('language.english'), value: 'en' },
  { title: translate('language.latvian'), value: 'lv' },
])

function changeLocale(value: string | null) {
  if (value === 'en' || value === 'lv') setLocale(value as Locale)
}

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
    error.value = cause instanceof Error ? cause.message : translate('auth.loadError')
  } finally {
    loading.value = false
  }
})
</script>
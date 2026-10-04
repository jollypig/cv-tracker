<template>
  <v-app>
    <RouterView v-if="route.meta.public" />
    <template v-else>
    <v-navigation-drawer permanent :width="drawerWidth" class="app-drawer">
      <div class="brand-lockup">
        <div class="brand-mark"><v-icon icon="mdi-file-account-outline" /></div>
        <div>
          <div class="brand-name">folio</div>
          <div class="brand-caption">{{ translate('app.brandCaption') }}</div>
        </div>
      </div>

      <div class="nav-label">{{ translate('navigation.workspace') }}</div>
      <v-list nav density="compact" class="nav-list">
        <v-list-item
          prepend-icon="mdi-account-multiple-outline"
          :title="translate('navigation.people')"
          to="/"
          exact
          rounded="lg"
        />
        <v-list-item
          prepend-icon="mdi-file-document-multiple-outline"
          :title="translate('navigation.cvLibrary')"
          to="/cvs"
          rounded="lg"
        />
      </v-list>

      <template #append>
        <div class="drawer-footer">
          <v-icon icon="mdi-shield-check-outline" size="18" />
          <span>{{ translate('navigation.privateWorkspace') }}</span>
        </div>
      </template>
    </v-navigation-drawer>

    <v-main>
      <header class="topbar">
        <div class="breadcrumb">{{ translate('navigation.home') }} <span>/</span> {{ pageTitle }}</div>
        <div class="topbar-account">
          <v-select
            :model-value="locale"
            :items="languageOptions"
            :label="translate('language.label')"
            :aria-label="translate('language.label')"
            density="compact"
            hide-details
            variant="outlined"
            class="language-select"
            @update:model-value="changeLocale"
          />
          <span class="account-name">{{ auth.user?.displayName || auth.user?.email }}</span>
          <v-tooltip :text="translate('navigation.signOut')">
            <template #activator="{ props }">
              <v-btn v-bind="props" :aria-label="translate('navigation.signOut')" icon="mdi-logout" size="small" variant="text" @click="signOut" />
            </template>
          </v-tooltip>
        </div>
      </header>
      <div class="page-frame">
        <RouterView />
      </div>
    </v-main>
    </template>
  </v-app>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useDisplay } from 'vuetify'
import { useRouter } from 'vue-router'
import { useAuthStore } from './shared/stores/authStore'
import { locale, setLocale, translate, type Locale, type TranslationKey } from './shared/i18n'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mobile } = useDisplay()
const drawerWidth = computed(() => mobile.value ? 76 : 248)
const pageTitle = computed(() => translate((route.meta.titleKey as TranslationKey | undefined) ?? 'navigation.peopleTitle'))
const languageOptions = computed(() => [
  { title: translate('language.english'), value: 'en' },
  { title: translate('language.latvian'), value: 'lv' },
])

function changeLocale(value: string | null) {
  if (value === 'en' || value === 'lv') setLocale(value as Locale)
}

async function signOut() {
  try {
    await auth.logout()
  } finally {
    await router.replace('/login')
  }
}
</script>
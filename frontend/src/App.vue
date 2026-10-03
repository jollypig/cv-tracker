<template>
  <v-app>
    <RouterView v-if="route.meta.public" />
    <template v-else>
    <v-navigation-drawer permanent :width="drawerWidth" class="app-drawer">
      <div class="brand-lockup">
        <div class="brand-mark"><v-icon icon="mdi-file-account-outline" /></div>
        <div>
          <div class="brand-name">folio</div>
          <div class="brand-caption">CV WORKSPACE</div>
        </div>
      </div>

      <div class="nav-label">WORKSPACE</div>
      <v-list nav density="compact" class="nav-list">
        <v-list-item
          prepend-icon="mdi-account-multiple-outline"
          title="People"
          to="/"
          exact
          rounded="lg"
        />
        <v-list-item
          prepend-icon="mdi-file-document-multiple-outline"
          title="CV library"
          to="/cvs"
          rounded="lg"
        />
      </v-list>

      <template #append>
        <div class="drawer-footer">
          <v-icon icon="mdi-shield-check-outline" size="18" />
          <span>Private workspace</span>
        </div>
      </template>
    </v-navigation-drawer>

    <v-main>
      <header class="topbar">
        <div class="breadcrumb">Workspace <span>/</span> {{ pageTitle }}</div>
        <div class="topbar-account">
          <span class="account-name">{{ auth.user?.displayName || auth.user?.email }}</span>
          <v-tooltip text="Sign out">
            <template #activator="{ props }">
              <v-btn v-bind="props" aria-label="Sign out" icon="mdi-logout" size="small" variant="text" @click="signOut" />
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

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mobile } = useDisplay()
const drawerWidth = computed(() => mobile.value ? 76 : 248)
const pageTitle = computed(() => route.meta.title ?? 'People')

async function signOut() {
  try {
    await auth.logout()
  } finally {
    await router.replace('/login')
  }
}
</script>
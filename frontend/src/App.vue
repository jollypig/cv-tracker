<template>
  <v-app>
    <v-navigation-drawer permanent width="248" class="app-drawer">
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
        <div class="local-status"><span class="status-dot" /> Local environment</div>
      </header>
      <div class="page-frame">
        <RouterView />
      </div>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => route.meta.title ?? 'People')
</script>
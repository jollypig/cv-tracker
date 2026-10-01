<template>
  <main class="workspace-view">
    <div class="page-heading">
      <div>
        <div class="eyebrow">YOUR CAREER DOCUMENTS</div>
        <h1>{{ isLibrary ? 'CV library' : 'People' }}</h1>
        <p>{{ isLibrary ? 'CVs across your workspace.' : 'Profiles and the CVs connected to them.' }}</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" rounded="lg" :disabled="true">
        {{ isLibrary ? 'Create CV' : 'Add person' }}
      </v-btn>
    </div>

    <section class="empty-state" aria-labelledby="empty-title">
      <div class="empty-illustration" aria-hidden="true">
        <div class="paper paper-back"><span /></div>
        <div class="paper paper-front">
          <div class="paper-avatar"><v-icon icon="mdi-account-outline" /></div>
          <span class="paper-line line-long" />
          <span class="paper-line line-medium" />
          <span class="paper-line line-short" />
        </div>
        <div class="illustration-spark"><v-icon icon="mdi-plus" size="16" /></div>
      </div>
      <div class="empty-copy">
        <div class="empty-kicker">A GOOD PLACE TO BEGIN</div>
        <h2 id="empty-title">Your workspace is ready.</h2>
        <p>People, CVs, versions, and exports will live here as you build them.</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isLibrary = computed(() => route.path === '/cvs')
</script>